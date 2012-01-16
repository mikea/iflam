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
  int32_t* buf;
  double* accum;
  UInt32 samples;
};

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

    for (int i = 0; i < procData->frames / 2; ++i) {
      procData->accum[i] += procData->buf[i];
    }
    procData->samples++;
  }

  return 0;
}

@implementation iflamAppDelegate

@synthesize window;
@synthesize flamView;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
  AudioDeviceID inDevice = GetDefaultInputDevice();
  NSLog(@"Default input device: %d", inDevice);

  IOProcData* procData = new IOProcData();
  int frames = 4096;
  procData->frames = frames;
  procData->fftManager = new FFTBufferManager(frames);
  procData->buf = new int32_t[frames / 2];
  procData->accum = new double[frames / 2];
  procData->samples = 0;
  memset(procData->accum, 0, sizeof(double)* frames / 2);

  AudioDeviceIOProcID theIOProcID = NULL;

  BOOST_VERIFY(!AudioDeviceCreateIOProcID(inDevice, MyIOProc, procData, &theIOProcID));
  BOOST_VERIFY(!AudioDeviceStart(inDevice, theIOProcID));
  // theError = AudioDeviceStop(inDevice, theIOProcID);
  // theError = AudioDeviceDestroyIOProcID(inDevice, theIOProcID);

  Genome* genome = new Genome();
  genome->Read("/Users/aizatsky/Projects/iflam/flam-java/flams/e_6.flam3");
  [flamView setGenome: genome];
}

- (void)dealloc {
    [super dealloc];
}

@end
