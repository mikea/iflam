//
//  iosViewController.h
//  ios
//
//  Created by Mike Aizatskyi on 2/5/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>

#include "component.h"

@interface FlamiosViewController : UIViewController {
    boost::shared_ptr<FlamComponent> _component;
}

@end
