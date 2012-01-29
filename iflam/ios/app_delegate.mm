//
//  flamAppDelegate.m
//  flam
// vim: filetype=objc
#import "app_delegate.h"
#import "flam_view.h"
#import "flamViewController.h"

#include "genome.h"

@implementation AppDelegate


@synthesize window=_window;

@synthesize viewController=_viewController;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
  NSLog(@"application didFinishLaunchingWithOptions");

  self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
  //    self.window.rootViewController = self.viewController;
  [self.window makeKeyAndVisible];

  FlamView* flamView = [[FlamView alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
  [self.window addSubview: flamView];

  NSString* sheepPath = [NSString pathWithComponents:
    [NSArray arrayWithObjects:
        [[NSBundle mainBundle] pathForResource: @"sheeps"
                                        ofType: nil],
        @"976.flam3",
        nil]];

  NSLog(@"path: %@", sheepPath);

  Genome* genome = new Genome();
  genome->Read([sheepPath UTF8String]);
  [flamView setGenome: genome];

  return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application {
  NSLog(@"applicationWillResignActive");
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
  NSLog(@"applicationDidEnterBackground");
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
  NSLog(@"applicationWillEnterForeground");
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
  NSLog(@"applicationDidBecomeActive");
}

- (void)applicationWillTerminate:(UIApplication *)application {
  NSLog(@"applicationWillTerminate");
}

- (void)dealloc {
  [_window release];
  [_viewController release];
  [super dealloc];
}

@end
