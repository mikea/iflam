#import <Cocoa/Cocoa.h>
#import "AppDelegate.h"

int main(int /*argc*/, char* /*argv*/[]) {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  NSApp = [NSApplication sharedApplication];
  [NSApp activateIgnoringOtherApps: YES];


  [NSApp setDelegate: [[AppDelegate alloc] init]];
  [NSApp setPresentationOptions: NSFullScreenWindowMask];


  /*
  NSMenu* m = [[NSMenu alloc] initWithTitle:@"Apple"];
  // [NSApp performSelector:@selector(setAppleMenu:) withObject: m];

  NSMenu *mainMenu = [[NSMenu alloc] initWithTitle: @"MainMenu"];
  NSMenuItem* mi = [mainMenu addItemWithTitle:@"Apple" action:NULL keyEquivalent:@""];
  [mainMenu setSubmenu:m forItem:mi];
  mi = [m addItemWithTitle: @"Test Item"
    action: nil
    keyEquivalent: @""];
  [NSApp setMainMenu: mainMenu];
  */

  [NSApp run];
  [NSApp release];
  [pool release];
  return EXIT_SUCCESS;
}
