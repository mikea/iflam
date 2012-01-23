#import <Cocoa/Cocoa.h>
#import "FlamView.h"

class FFTCollector;

@interface AppDelegate : NSObject <NSApplicationDelegate> {
@private
  NSWindow* window;
  FlamView* flamView;
  Genome* _genome;
  FFTCollector* collector_;
}

@property (assign) IBOutlet NSWindow *window;
@property (assign) IBOutlet FlamView *flamView;


- (void)newFFtDataAvailable:(Float32*) fftData size:(size_t) size min:(Float32)aMin max:(Float32)aMax;

@end
