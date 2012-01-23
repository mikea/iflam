#import <Cocoa/Cocoa.h>
#import "FlamView.h"

@interface AppDelegate : NSObject <NSApplicationDelegate> {
@private
  NSWindow* window;
  FlamView* flamView;
  Genome* _genome;
}

@property (assign) IBOutlet NSWindow *window;
@property (assign) IBOutlet FlamView *flamView;


- (void)newFFtDataAvailable:(int32_t*) fftData size:(size_t) size;

@end
