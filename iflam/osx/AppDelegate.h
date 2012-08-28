#import <Cocoa/Cocoa.h>
#import "FlamView.h"

class FFTCollector;
class Genome;
class Animator;


@interface AppDelegate : NSObject <NSApplicationDelegate, NSWindowDelegate> {
@private
  NSWindow* window;
  FlamView* flamView;
  Genome* _genome;
  FFTCollector* collector_;
  Animator* animator_;
  double last_change_;
}

- (void)newFFtDataAvailable:(Float32*) fftData size:(size_t) size min:(Float32)aMin max:(Float32)aMax;

- (void)setRandomAnimator;

@end
