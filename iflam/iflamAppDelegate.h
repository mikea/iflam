#import <Cocoa/Cocoa.h>
#import "FlamView.h"

@interface iflamAppDelegate : NSObject <NSApplicationDelegate> {
@private
  NSWindow* window;
  FlamView* flamView;
  Genome* _genome;
}

@property (assign) IBOutlet NSWindow *window;
@property (assign) IBOutlet FlamView *flamView;


@end
