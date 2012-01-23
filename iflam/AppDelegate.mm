
#import "AppDelegate.h"
#import <CoreAudio/CoreAudio.h>
#import <boost/assert.hpp>
#import "fft/FFTBufferManager.h"
#import "genome.h"



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
    UInt32  param;

    {
      // Open the AudioOutputUnit
      // There are several different types of Audio Units.
      // Some audio units serve as Outputs, Mixers, or DSP
      // units. See AUComponent.h for listing
      Component component;
      ComponentDescription description;
      description.componentType = kAudioUnitType_Output;
      description.componentSubType = kAudioUnitSubType_HALOutput;
      description.componentManufacturer = kAudioUnitManufacturer_Apple;
      description.componentFlags = 0;
      description.componentFlagsMask = 0;
      component = FindNextComponent(NULL, &description);
      BOOST_ASSERT(component);
      VERIFY_OSSTATUS(OpenAComponent(component, &fAudioUnit));
    }

    // Configure the AudioOutputUnit
    // You must enable the Audio Unit (AUHAL) for input and output for the same  device.
    // When using AudioUnitSetProperty the 4th parameter in the method
    // refer to an AudioUnitElement.  When using an AudioOutputUnit
    // for input the element will be '1' and the output element will be '0'.

    // Enable input on the AUHAL
    param = 1;
    VERIFY_OSSTATUS(AudioUnitSetProperty(fAudioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, 1, &param, sizeof(UInt32)));
    // Disable Output on the AUHAL
    param = 0;
    VERIFY_OSSTATUS(AudioUnitSetProperty(fAudioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, 0, &param, sizeof(UInt32)));

    // Select the default input device
    param = sizeof(AudioDeviceID);
    VERIFY_OSSTATUS(AudioHardwareGetProperty(kAudioHardwarePropertyDefaultInputDevice, &param, &fInputDeviceID));

    // Set the current device to the default input unit.
    VERIFY_OSSTATUS(AudioUnitSetProperty(fAudioUnit, kAudioOutputUnitProperty_CurrentDevice, kAudioUnitScope_Global, 0, &fInputDeviceID, sizeof(AudioDeviceID)));

    {
      AURenderCallbackStruct callback;
      // Setup render callback
      // This will be called when the AUHAL has input data
      callback.inputProc = FFTCollector::AudioInputProc; // defined as static in the header file
      callback.inputProcRefCon = this;
      VERIFY_OSSTATUS(AudioUnitSetProperty(fAudioUnit, kAudioOutputUnitProperty_SetInputCallback, kAudioUnitScope_Global, 0, &callback, sizeof(AURenderCallbackStruct)));
    }

    // get hardware device format
    param = sizeof(AudioStreamBasicDescription);
    VERIFY_OSSTATUS(AudioUnitGetProperty(fAudioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, 1, &fDeviceFormat, &param));

    // Twiddle the format to our liking
    fAudioChannels = MAX(fDeviceFormat.mChannelsPerFrame, 2);
    fOutputFormat.mChannelsPerFrame = fAudioChannels;
    fOutputFormat.mSampleRate = fDeviceFormat.mSampleRate;
    fOutputFormat.mFormatID = kAudioFormatLinearPCM;
    fOutputFormat.mFormatFlags = kAudioFormatFlagIsFloat | kAudioFormatFlagIsPacked | kAudioFormatFlagIsNonInterleaved;
    if (fOutputFormat.mFormatID == kAudioFormatLinearPCM && fAudioChannels == 1) {
      fOutputFormat.mFormatFlags &= ~kLinearPCMFormatFlagIsNonInterleaved;
    }
    #if __BIG_ENDIAN__
        fOutputFormat.mFormatFlags |= kAudioFormatFlagIsBigEndian;
    #endif
    fOutputFormat.mBitsPerChannel = sizeof(Float32) * 8;
    fOutputFormat.mBytesPerFrame = fOutputFormat.mBitsPerChannel / 8;
    fOutputFormat.mFramesPerPacket = 1;
    fOutputFormat.mBytesPerPacket = fOutputFormat.mBytesPerFrame;

    // Set the AudioOutputUnit output data format
    VERIFY_OSSTATUS(AudioUnitSetProperty(fAudioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, 1, &fOutputFormat, sizeof(  AudioStreamBasicDescription)));

    // Get the number of frames in the IO buffer(s)
    param = sizeof(UInt32);
    VERIFY_OSSTATUS(AudioUnitGetProperty(fAudioUnit, kAudioDevicePropertyBufferFrameSize, kAudioUnitScope_Global, 0, &fAudioSamples, &param));

    UInt32 maxFPS;
    param = sizeof(maxFPS);
    VERIFY_OSSTATUS(AudioUnitGetProperty(fAudioUnit, kAudioUnitProperty_MaximumFramesPerSlice, kAudioUnitScope_Global, 0, &maxFPS, &param));

    fftBufferManager = new FFTBufferManager(maxFPS);
    fft_data_ = new int32_t[maxFPS/2];
    fft_data_size_ = maxFPS / 2;

    // Initialize the AU
    VERIFY_OSSTATUS(AudioUnitInitialize(fAudioUnit));

    // Allocate our audio buffers
    audio_buffer_ = AllocateAudioBufferList(fOutputFormat.mChannelsPerFrame, fAudioSamples * fOutputFormat.mBytesPerFrame);
    BOOST_ASSERT(audio_buffer_);
  }

  void Start() {
    VERIFY_OSSTATUS(AudioOutputUnitStart(fAudioUnit));
  }

  void Stop() {
    VERIFY_OSSTATUS(AudioOutputUnitStop(fAudioUnit));
  }

  void ProcessAudioInput() {
    NSLog(@"Data: %d", audio_buffer_->mBuffers[0].mDataByteSize);
    if (fftBufferManager->NeedsNewAudioData()) {
      fftBufferManager->GrabAudioData(audio_buffer_);
    } else {
      BOOST_ASSERT(fftBufferManager->HasNewAudioData());
      // memset(fft_data_, 0, sizeof(int32_t)*fft_data_size_);
      fftBufferManager->ComputeFFT(fft_data_);
      // memset(fft_data_, 0, sizeof(int32_t)*fft_data_size_);
      [delegate_ newFFtDataAvailable:fft_data_ size:fft_data_size_];
    }
  }


  static OSStatus AudioInputProc(
      void* inRefCon,
      AudioUnitRenderActionFlags* ioActionFlags,
      const AudioTimeStamp* inTimeStamp,
      UInt32 inBusNumber,
      UInt32 inNumberFrames,
      AudioBufferList* ioData) {
    FFTCollector *THIS = (FFTCollector*)inRefCon;
    // Render into audio buffer
    VERIFY_OSSTATUS(AudioUnitRender(THIS->fAudioUnit, ioActionFlags, inTimeStamp, inBusNumber, inNumberFrames, THIS->audio_buffer_));
    // VERIFY_OSSTATUS(ExtAudioFileWriteAsync(afr->fOutputAudioFile, inNumberFrames, afr->fAudioBuffer));

    THIS->ProcessAudioInput();

    return noErr;
  }


  AppDelegate*                delegate_;
  AudioBufferList*            audio_buffer_;
  AudioUnit                   fAudioUnit;
  AudioDeviceID               fInputDeviceID;
  UInt32                      fAudioChannels;
  UInt32                      fAudioSamples;
  AudioStreamBasicDescription fOutputFormat;
  AudioStreamBasicDescription fDeviceFormat;
  FSRef                       fOutputDirectory;

  FFTBufferManager*           fftBufferManager;
  int32_t*                    fft_data_;
  size_t                      fft_data_size_;
};


@implementation AppDelegate

@synthesize window;
@synthesize flamView;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
  FFTCollector* collector = new FFTCollector(self);
  collector->ConfigureAU();
  collector->Start();

/*  double fps = 25;

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

- (void)newFFtDataAvailable:(int32_t*) fftData size:(size_t) size {
  SInt8 f = (fftData[20]& 0xFF000000) >> 24;
  double v = (f + 80) / 64.;
  v = clamp(0, v, 1.);
  NSLog(@"%d", f);
}

- (void)onTimer:(NSTimer*)theTimer {
/*  Genome* genome = new Genome(*_genome);
  NSTimeInterval time = [NSDate timeIntervalSinceReferenceDate];
  genome->Move(sin(time * .1), cos(time * .1));
  [flamView setGenome: genome]; */
}

- (void)dealloc {
    [super dealloc];
}

@end
