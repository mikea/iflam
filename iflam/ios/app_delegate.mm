// vim: filetype=objc
#import "app_delegate.h"
#import "flam_view_controller.h"


@implementation AppDelegate


@synthesize window=_window;

@synthesize viewController=_viewController;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
  NSLog(@"application didFinishLaunchingWithOptions");

  self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
  //    self.window.rootViewController = self.viewController;
  [self.window makeKeyAndVisible];


  FlamViewController* flamController = [[FlamViewController alloc] init];
  [self.window addSubview:flamController.view];

  NSString* sheepPath = [NSString pathWithComponents:
    [NSArray arrayWithObjects:
        [[NSBundle mainBundle] pathForResource: @"sheeps"
                                        ofType: nil],
        @"976.flam3",
        nil]];

  NSLog(@"path: %@", sheepPath);
  [flamController loadGenome: sheepPath];

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
