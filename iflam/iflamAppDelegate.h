#import <Cocoa/Cocoa.h>
#import "FlamView.h"

class IOProcData;

@interface iflamAppDelegate : NSObject <NSApplicationDelegate> {
@private
  NSWindow* window;
  FlamView* flamView;
  Genome* _genome;
  IOProcData* procData;
}

@property (assign) IBOutlet NSWindow *window;
@property (assign) IBOutlet FlamView *flamView;


@end
