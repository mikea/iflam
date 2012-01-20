//
//  iflamAppDelegate.m
//  iflam
//
//  Created by Mike Aizatskyi on 1/3/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import "iflamAppDelegate.h"
#import <CoreAudio/CoreAudio.h>
#import <boost/assert.hpp>
#import "fft/FFTBufferManager.h"
#import "genome.h"

struct IOProcData {
  UInt32 frames;
  FFTBufferManager* fftManager;
  SInt32* buf;
  double* accum;
  UInt32 samples;
  NSLock* accumLock;
};

NSString* GetDeviceName(AudioDeviceID device) {
  OSStatus theStatus;
  CFStringRef theCFString;
  NSString * rv;
  UInt32 theSize;

  theSize = sizeof ( CFStringRef );
  theStatus = AudioDeviceGetProperty(device, 0, false, kAudioDevicePropertyDeviceNameCFString, &theSize, &theCFString );
  if ( theStatus != 0 || theCFString == NULL )
    return nil;
  rv = [NSString stringWithString:(NSString *)theCFString];
  CFRelease ( theCFString );
  return rv;
}

double clamp(double min,double x,double max) { return (x < min ? min : (x > max ? max : x)); }

AudioDeviceID GetDefaultInputDevice() {
    AudioDeviceID theAnswer = 0;
    UInt32 theSize = sizeof(AudioDeviceID);
    AudioObjectPropertyAddress theAddress = {
      kAudioHardwarePropertyDefaultInputDevice,
      kAudioObjectPropertyScopeGlobal,
      kAudioObjectPropertyElementMaster};

    BOOST_VERIFY(!AudioObjectGetPropertyData(
          kAudioObjectSystemObject,
          &theAddress,
          0,
          NULL,
          &theSize,
          &theAnswer));
    return theAnswer;
}

OSStatus MyIOProc(AudioDeviceID           inDevice,
                  const AudioTimeStamp*   inNow,
                  const AudioBufferList*  inInputData,
                  const AudioTimeStamp*   inInputTime,
                  AudioBufferList*        outOutputData,
                  const AudioTimeStamp*   inOutputTime,
                  void*                   inClientData) {
  IOProcData* procData = (IOProcData*)inClientData;

  if (procData->fftManager->NeedsNewAudioData()) {
    procData->fftManager->GrabAudioData(inInputData);
  } else {
    BOOST_ASSERT(procData->fftManager->HasNewAudioData());
    procData->fftManager->ComputeFFT(procData->buf);

    NSLock* lock = procData->accumLock;

    [lock lock];
    for (size_t i = 0; i < procData->frames / 2; ++i) {
      SInt8 f = (procData->buf[i]& 0xFF000000) >> 24;
      double v = (f + 80) / 64.;
      v = clamp(0, v, 1.);
      //v += 80;
      //v /= 64;
//      if (v > 1) { v = 1; }
//      if (v < 0) { v = 0; }
//      v = sqrt(v);
      if (i == 500) {
        NSLog(@"fft: %f", v);
      }
      procData->accum[i] = v;
    }
    procData->samples = 1;
    [lock unlock];
  }

  return 0;
}

void DestroyAudioBufferList(AudioBufferList* list) {
  UInt32 i;

  if(list) {
    for(i = 0; i < list->mNumberBuffers; i++) {
      if(list->mBuffers[i].mData)
        free(list->mBuffers[i].mData);
    }
    free(list);
  }
}

AudioBufferList* AllocateAudioBufferList(UInt32 numChannels, UInt32 size) {
  AudioBufferList* list;
  UInt32 i;

  list = (AudioBufferList*)calloc(1,
  sizeof(AudioBufferList) + numChannels * sizeof(AudioBuffer));
  if(list == NULL) {
    return NULL;
  }

  list->mNumberBuffers = numChannels;
  for(i = 0; i < numChannels; ++i) {
    list->mBuffers[i].mNumberChannels = 1;
    list->mBuffers[i].mDataByteSize = size;
    list->mBuffers[i].mData = malloc(size);
    if(list->mBuffers[i].mData == NULL) {
      DestroyAudioBufferList(list);
      return NULL;
    }
  }
  return list;
}

void CheckStatus(OSStatus status) {
  if (status) {
    NSLog(@"@@@@@@@@@@ Error: %d", status);
    abort();
  }
}

class FFTCollector {
public:

  OSStatus ConfigureAU() {
    Component                   component;
    ComponentDescription        description;
    OSStatus    err = noErr;
    UInt32  param;
    AURenderCallbackStruct  callback;

    // Open the AudioOutputUnit
    // There are several different types of Audio Units.
    // Some audio units serve as Outputs, Mixers, or DSP
    // units. See AUComponent.h for listing
    description.componentType = kAudioUnitType_Output;
    description.componentSubType = kAudioUnitSubType_HALOutput;
    description.componentManufacturer = kAudioUnitManufacturer_Apple;
    description.componentFlags = 0;
    description.componentFlagsMask = 0;
    if(component = FindNextComponent(NULL, &description))
    {
      CheckStatus(OpenAComponent(component, &fAudioUnit));
    }

    // Configure the AudioOutputUnit
    // You must enable the Audio Unit (AUHAL) for input and output for the same  device.
    // When using AudioUnitSetProperty the 4th parameter in the method
    // refer to an AudioUnitElement.  When using an AudioOutputUnit
    // for input the element will be '1' and the output element will be '0'.

    // Enable input on the AUHAL
    param = 1;
    err = AudioUnitSetProperty(fAudioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, 1, &param, sizeof(UInt32));
    if(err == noErr)
    {
      // Disable Output on the AUHAL
      param = 0;
      CheckStatus(AudioUnitSetProperty(fAudioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, 0, &param, sizeof(UInt32)));
    }

    // Select the default input device
    param = sizeof(AudioDeviceID);
    CheckStatus(AudioHardwareGetProperty(kAudioHardwarePropertyDefaultInputDevice, &param, &fInputDeviceID));

    // Set the current device to the default input unit.
    CheckStatus(
        AudioUnitSetProperty(fAudioUnit, kAudioOutputUnitProperty_CurrentDevice, kAudioUnitScope_Global, 0, &fInputDeviceID, sizeof(AudioDeviceID)));

    // Setup render callback
    // This will be called when the AUHAL has input data
    callback.inputProc = FFTCollector::AudioInputProc; // defined as static in the header file
    callback.inputProcRefCon = this;
    err = AudioUnitSetProperty(fAudioUnit, kAudioOutputUnitProperty_SetInputCallback, kAudioUnitScope_Global, 0, &callback, sizeof(AURenderCallbackStruct));

    // get hardware device format
    param = sizeof(AudioStreamBasicDescription);
    CheckStatus(AudioUnitGetProperty(fAudioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, 1, &fDeviceFormat, &param));

    // Twiddle the format to our liking
    fAudioChannels = MAX(fDeviceFormat.mChannelsPerFrame, 2);
    fOutputFormat.mChannelsPerFrame = fAudioChannels;
    fOutputFormat.mSampleRate = fDeviceFormat.mSampleRate;
    fOutputFormat.mFormatID = kAudioFormatLinearPCM;
    fOutputFormat.mFormatFlags = kAudioFormatFlagIsFloat | kAudioFormatFlagIsPacked | kAudioFormatFlagIsNonInterleaved;
    if (fOutputFormat.mFormatID == kAudioFormatLinearPCM && fAudioChannels == 1)
      fOutputFormat.mFormatFlags &= ~kLinearPCMFormatFlagIsNonInterleaved;
#if __BIG_ENDIAN__
    fOutputFormat.mFormatFlags |= kAudioFormatFlagIsBigEndian;
#endif
    fOutputFormat.mBitsPerChannel = sizeof(Float32) * 8;
    fOutputFormat.mBytesPerFrame = fOutputFormat.mBitsPerChannel / 8;
    fOutputFormat.mFramesPerPacket = 1;
    fOutputFormat.mBytesPerPacket = fOutputFormat.mBytesPerFrame;

    // Set the AudioOutputUnit output data format
    CheckStatus(AudioUnitSetProperty(fAudioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, 1, &fOutputFormat, sizeof(AudioStreamBasicDescription)));

    // Get the number of frames in the IO buffer(s)
    param = sizeof(UInt32);
    CheckStatus(AudioUnitGetProperty(fAudioUnit, kAudioDevicePropertyBufferFrameSize, kAudioUnitScope_Global, 0, &fAudioSamples, &param));

    // Initialize the AU
    CheckStatus(AudioUnitInitialize(fAudioUnit));

    // Allocate our audio buffers
    fAudioBuffer = AllocateAudioBufferList(fOutputFormat.mChannelsPerFrame, fAudioSamples * fOutputFormat.mBytesPerFrame);
    if(fAudioBuffer == NULL)
    {
      fprintf(stderr, "failed to allocate buffers\n");
      return err;
    }

    return noErr;
  }

  void Start() {
    CheckStatus(AudioOutputUnitStart(fAudioUnit));
  }

  void Stop() {
    CheckStatus(AudioOutputUnitStop(fAudioUnit));
  }

  static OSStatus AudioInputProc(
      void* inRefCon,
      AudioUnitRenderActionFlags* ioActionFlags,
      const AudioTimeStamp* inTimeStamp,
      UInt32 inBusNumber,
      UInt32 inNumberFrames,
      AudioBufferList* ioData) {
    FFTCollector *afr = (FFTCollector*)inRefCon;

    // Render into audio buffer
    CheckStatus(
        AudioUnitRender(
          afr->fAudioUnit,
          ioActionFlags,
          inTimeStamp,
          inBusNumber,
          inNumberFrames,
          afr->fAudioBuffer));

    // Write to file, ExtAudioFile auto-magicly handles conversion/encoding
    // NOTE: Async writes may not be flushed to disk until a the file
    // reference is disposed using ExtAudioFileDispose
    // err = ExtAudioFileWriteAsync(afr->fOutputAudioFile, inNumberFrames, afr->fAudioBuffer);

    return noErr;
  }


  AudioBufferList *fAudioBuffer;
  AudioUnit   fAudioUnit;
  AudioDeviceID   fInputDeviceID;
  UInt32  fAudioChannels, fAudioSamples;
  AudioStreamBasicDescription fOutputFormat, fDeviceFormat;
  FSRef fOutputDirectory;
};


@implementation iflamAppDelegate

@synthesize window;
@synthesize flamView;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {

  procData = new IOProcData();
  int frames = 4096;
  procData->frames = frames;
  procData->fftManager = new FFTBufferManager(frames);
  procData->buf = new int32_t[frames / 2];
  procData->accum = new double[frames / 2];
  procData->samples = 0;
  procData->accumLock = [[NSLock alloc] init];
  memset(procData->accum, 0, sizeof(double)* frames / 2);

  FFTCollector* collector = new FFTCollector();
  collector->ConfigureAU();
  collector->Start();

/*  AudioDeviceID inDevice = GetDefaultInputDevice();
  NSLog(@"Default input device: %d, %@", inDevice, GetDeviceName(inDevice));
  AudioDeviceIOProcID theIOProcID = NULL;

  BOOST_VERIFY(!AudioDeviceCreateIOProcID(
      inDevice,
      MyIOProc,
      procData,
      &theIOProcID));
  BOOST_VERIFY(!AudioDeviceStart(inDevice, theIOProcID));
  // theError = AudioDeviceStop(inDevice, theIOProcID);
  // theError = AudioDeviceDestroyIOProcID(inDevice, theIOProcID);


  double fps = 25;

  NSTimer *timer = [NSTimer
    scheduledTimerWithTimeInterval:1/fps
                            target:self
                          selector:@selector(onTimer:)
                          userInfo:nil
                           repeats:YES];*/
  _genome = new Genome();
  _genome->Read("/Users/aizatsky/Projects/iflam/flam-java/flams/e_6.flam3");
  [flamView setGenome: new Genome(*_genome)];
}

- (void)onTimer:(NSTimer*)theTimer {
  NSLock* lock = procData->accumLock;

  [lock lock];
    if (procData->samples > 0) {
//      size_t bucket_length = 1;
//      for (int i = 0; i < bucket_length; ++i) {
//        d += procData->accum[i];
//      }
      double d = 0;
      size_t accum_length = procData->frames / 2;

      d = 0.5 * procData->accum[50] + 0.5 * procData->accum[51];
      d /=  procData->samples;

      // NSLog(@"%d, %f", procData->samples, d);

      memset(procData->accum, 0, sizeof(double)* accum_length);
      procData->samples = 0;

      d /= 128 * 100;
      // NSLog(@"%f", d);
      Genome* genome = new Genome(*_genome);
      genome->mutable_xforms()->at(0).mutable_coefs()->at(2) += d;
      [flamView setGenome: genome];
    }
  [lock unlock];


/*  Genome* genome = new Genome(*_genome);
  NSTimeInterval time = [NSDate timeIntervalSinceReferenceDate];
  genome->Move(sin(time * .1), cos(time * .1));
  [flamView setGenome: genome]; */
}

- (void)dealloc {
    [super dealloc];
}

@end
