//
//  iosViewController.h
//  ios
//
//  Created by Mike Aizatskyi on 2/5/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>

#include <boost/smart_ptr.hpp>

class FlamComponent;
class SlideshowController;

@interface FlamiosViewController : UIViewController {
    FlamComponent* _component;
    boost::shared_ptr<SlideshowController> _controller;
}

@end
