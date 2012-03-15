//
//  iosAppDelegate.h
//  ios
//
//  Created by Mike Aizatskyi on 2/5/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>

@class FlamiosViewController;

@interface FlamiosAppDelegate : NSObject <UIApplicationDelegate> {

}

@property (nonatomic, strong) IBOutlet UIWindow *window;
@property (strong) IBOutlet FlamiosViewController *viewController;

@end
