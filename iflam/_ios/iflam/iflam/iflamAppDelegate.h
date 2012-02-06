//
//  iflamAppDelegate.h
//  iflam
//
//  Created by Mike Aizatskyi on 2/1/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>

@class iflamViewController;

@interface iflamAppDelegate : NSObject <UIApplicationDelegate> {

}

@property (nonatomic, retain) IBOutlet UIWindow *window;

@property (nonatomic, retain) IBOutlet iflamViewController *viewController;

@end
