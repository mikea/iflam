
#import "AppDelegate.h"

#include "CAAudioUnit.h"
#include "CAAudioUnitOutputCapturer.h"
#include "CABufferList.h"
#include "CAComponent.h"
#include "CAHALAudioDevice.h"
#include "CAHALAudioSystemObject.h"
#include "CASpectralProcessor.h"
#include "CAStreamBasicDescription.h"
#include "animator.h"
#include "fft/FFTBufferManager.h"
#include "genome.h"
#include <CoreAudio/CoreAudio.h>
#include <CoreFoundation/CFURL.h>
#include <CoreFoundation/CoreFoundation.h>
#include <boost/assert.hpp>

double clamp(double min,double x,double max) { return (x < min ? min : (x > max ? max : x)); }


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


class FFTCollector {
public:
  FFTCollector(AppDelegate* delegate)
    : delegate_(delegate) {

  }

  void ConfigureAU() {
    NSLog(@"ConfigureAU");

    {
      // Open the output unit
      AudioComponentDescription desc;
      desc.componentType = kAudioUnitType_Output;
      desc.componentSubType = kAudioUnitSubType_HALOutput; // iphone: kAudioUnitSubType_RemoteIO;
      desc.componentManufacturer = kAudioUnitManufacturer_Apple;
      desc.componentFlags = 0;
      desc.componentFlagsMask = 0;

      CAComponent halOutput(desc);
      halOutput.Print();
      BOOST_ASSERT(halOutput.IsValid());

      VERIFY_OSSTATUS(CAAudioUnit::Open(halOutput, audio_unit_));
      BOOST_ASSERT(audio_unit_.IsValid());
    }

    {
      // Enable input.
      UInt32 enableInput = 1;
      AudioUnitElement inputBus = 1;
      VERIFY_OSSTATUS(audio_unit_.SetProperty(kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, inputBus, &enableInput, sizeof(enableInput)));
    }

    {
      // Disable output.
      UInt32 enableOutput = 0;
      AudioUnitElement outputBus = 0;
      VERIFY_OSSTATUS(audio_unit_.SetProperty(kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, outputBus, &enableOutput, sizeof (enableOutput)));
    }

    {
      // Set the current device to the input unit.
      AudioDeviceID defaultInput = 0;
      UInt32 size = sizeof(AudioDeviceID);
      AudioObjectPropertyAddress propertyAddress = {kAudioHardwarePropertyDefaultInputDevice, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMaster};
      VERIFY_OSSTATUS(AudioObjectGetPropertyData(kAudioObjectSystemObject, &propertyAddress, 0, NULL, &size, &defaultInput));
      VERIFY_OSSTATUS(audio_unit_.SetProperty(kAudioOutputUnitProperty_CurrentDevice, kAudioUnitScope_Global, 0, &defaultInput, sizeof(defaultInput)));
    }


    {
      UInt32 bufferSize = 256;
      UInt32 size = sizeof(UInt32);
      VERIFY_OSSTATUS(audio_unit_.SetProperty(kAudioDevicePropertyBufferFrameSize,
            kAudioUnitScope_Global, 0, &bufferSize, size));
    }

    UInt32 bufferFrameSize;
    {
      UInt32 size = sizeof(UInt32);
      VERIFY_OSSTATUS(audio_unit_.GetProperty(kAudioDevicePropertyBufferFrameSize, kAudioUnitScope_Global, 0, &bufferFrameSize, &size));
      NSLog(@"bufferFrameSize=%d", bufferFrameSize);
    }

    {
      // Set output format format.
      CAStreamBasicDescription outFormat;
      outFormat.SetAUCanonical(2, false);
      outFormat.mSampleRate = 44100;

      VERIFY_OSSTATUS(audio_unit_.SetProperty(kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, 1, &outFormat, sizeof(outFormat)));

      {
        // Create buffers

        audio_buffer_ = AllocateAudioBufferList(outFormat.NumberChannels(), outFormat.FramesToBytes(bufferFrameSize));
        BOOST_ASSERT(audio_buffer_);
      }
    }

    {
      // Set callback.
      AURenderCallbackStruct callback;
      callback.inputProc = FFTCollector::AudioInputProc;
      callback.inputProcRefCon = this;

      VERIFY_OSSTATUS(audio_unit_.SetProperty(kAudioOutputUnitProperty_SetInputCallback, kAudioUnitScope_Global, 0, &callback, sizeof(callback)));
    }

    VERIFY_OSSTATUS(audio_unit_.Initialize());

    UInt32 block_size = bufferFrameSize / 8;
    UInt32 num_bins = block_size >> 1;
    UInt32 num_channels = 2;
    UInt32 sample_rate = 44100;

    spectral_processor_ = new CASpectralProcessor(block_size, num_bins, 1 /* channels */, bufferFrameSize);

    CAStreamBasicDescription  bufClientDesc;
    bufClientDesc.SetCanonical(num_channels, false);
    bufClientDesc.mSampleRate = sample_rate;

    spectral_data_buffer_ = CABufferList::New("spectral_data_buffer_", bufClientDesc);
    spectral_data_buffer_->AllocateBuffers(block_size * num_bins * sizeof(Float32) / 2);
  }

  void Start() {
    NSLog(@"StartAU");

    if (0) {
      AudioStreamBasicDescription anASBD;
      anASBD.mFormatID         = kAudioFormatLinearPCM;
      anASBD.mFormatFlags      = kAudioFormatFlagIsSignedInteger | kAudioFormatFlagIsBigEndian | kAudioFormatFlagIsPacked;
      anASBD.mSampleRate       = 44100;
      anASBD.mChannelsPerFrame = 2;
      anASBD.mFramesPerPacket  = 1;
      anASBD.mBytesPerPacket   = anASBD.mChannelsPerFrame * sizeof (SInt16);
      anASBD.mBytesPerFrame    = anASBD.mChannelsPerFrame * sizeof (SInt16);
      anASBD.mBitsPerChannel   = 16;

      CFURLRef fileUrl = CFURLCreateWithFileSystemPath(NULL, CFSTR("/tmp/recording.aiff"), kCFURLPOSIXPathStyle, false);
      VERIFY_OSSTATUS(ExtAudioFileCreateWithURL(fileUrl, kAudioFileAIFFType, &anASBD, NULL, kAudioFileFlags_EraseFile, &audio_file_));

      CAStreamBasicDescription clientFormat;
      clientFormat.SetAUCanonical(2, false);
      clientFormat.mSampleRate = 44100;

      VERIFY_OSSTATUS(ExtAudioFileSetProperty(audio_file_, kExtAudioFileProperty_ClientDataFormat, sizeof(clientFormat), &clientFormat));
      VERIFY_OSSTATUS(ExtAudioFileWriteAsync(audio_file_, 0, NULL));
    }

    VERIFY_OSSTATUS(AudioOutputUnitStart(audio_unit_.AU()));
    audio_unit_.Print();
  }

  void Stop() {
    NSLog(@"StopAU");
    VERIFY_OSSTATUS(AudioOutputUnitStop(audio_unit_.AU()));

    /*
    if (audio_file_) {
      VERIFY_OSSTATUS(ExtAudioFileDispose(audio_file_));
    }
    */
  }

  void ProcessAudioInput() {
    Float32 minAmp = 0, maxAmp = 0;
    spectral_processor_->GetMagnitude(&spectral_data_buffer_->GetModifiableBufferList(), &minAmp, &maxAmp);
    [delegate_ newFFtDataAvailable: nil size: -1 min:minAmp max:maxAmp];
}


  static OSStatus AudioInputProc(
      void* inRefCon,
      AudioUnitRenderActionFlags* ioActionFlags,
      const AudioTimeStamp* inTimeStamp,
      UInt32 inBusNumber,
      UInt32 inNumberFrames,
      AudioBufferList* ioData) {
    FFTCollector *THIS = (FFTCollector*)inRefCon;
    VERIFY_OSSTATUS(THIS->audio_unit_.Render(ioActionFlags, inTimeStamp, inBusNumber, inNumberFrames, THIS->audio_buffer_));
    // VERIFY_OSSTATUS(ExtAudioFileWriteAsync(THIS->audio_file_, inNumberFrames, THIS->audio_buffer_));

    if (THIS->spectral_processor_->ProcessForwards(inNumberFrames, THIS->audio_buffer_)) {
      THIS->ProcessAudioInput();
    }
    return noErr;
  }


  AppDelegate*                delegate_;
  AudioBufferList*            audio_buffer_;
  CASpectralProcessor*        spectral_processor_;
  CAAudioUnit                 audio_unit_;
  CAAudioUnitOutputCapturer*  capturer_;
  ExtAudioFileRef             audio_file_;
  CABufferList* spectral_data_buffer_;


  FFTBufferManager*           fftBufferManager;
  int32_t*                    fft_data_;
  size_t                      fft_data_size_;
};


@implementation AppDelegate

@synthesize window;
@synthesize flamView;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
  NSLog(@"applicationDidFinishLaunching");

  animator_ = new Animator();

  collector_ = new FFTCollector(self);
  collector_->ConfigureAU();
  collector_->Start();

/*  double fps = 25;

  NSTimer *timer = [NSTimer
    scheduledTimerWithTimeInterval:1/fps
                            target:self
                          selector:@selector(onTimer:)
                          userInfo:nil
                           repeats:YES];*/
  _genome = new Genome();
  _genome->Read("/Users/aizatsky/Projects/iflam/sheeps/1250.flam3");
  animator_->Randomize(*_genome);
  [flamView setGenome: new Genome(*_genome)];
}

- (void)applicationWillTerminate:(NSNotification *)aNotification {
  NSLog(@"applicationWillTerminate");
  collector_->Stop();
  delete collector_;
}


- (void)newFFtDataAvailable:(Float32*) fftData size:(size_t) size min:(Float32)aMin max:(Float32)aMax {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

  Genome* genome = new Genome(*_genome);
  Signal signal(WallTime(), aMax / 32);
  animator_->Animate(signal, genome);

  [flamView setGenome: genome];
  [pool release];
}

- (void)onTimer:(NSTimer*)theTimer {
/*  Genome* genome = new Genome(*_genome);
  NSTimeInterval time = [NSDate timeIntervalSinceReferenceDate];
  genome->Move(sin(time * .1), cos(time * .1));
  [flamView setGenome: genome]; */
}

- (void)onMouseDown:(NSEvent*) anEvent {
  NSLog(@"onMouseDown");
  animator_->Randomize(*_genome);
}

- (void)dealloc {
    [super dealloc];
}

- (void)setRandomAnimator {
}

@end
