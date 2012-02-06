//
//  flamAppDelegate.h
//  flam
//
//  Created by Mike Aizatskyi on 1/24/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>

@class flamViewController;

@interface flamAppDelegate : NSObject <UIApplicationDelegate> {

}

@property (nonatomic, retain) IBOutlet UIWindow *window;

@property (nonatomic, retain) IBOutlet flamViewController *viewController;

@end
