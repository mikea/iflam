//
//  iosViewController.m
//  ios
//
//  Created by Mike Aizatskyi on 2/5/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import "FlamiosViewController.h"

#include "controller.h"
#include "component.h"

@implementation FlamiosViewController

- (id)init {
    self = [super init];
    if (self) {
        {
            boost::shared_ptr<Controller> slide_show(new SlideshowController("../sheeps/"));
            _component = new FlamComponent(slide_show);
        }
        
    }
    
    return self;
}

- (void)dealloc {
    [super dealloc];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

#pragma mark - View lifecycle

/*
// Implement viewDidLoad to do additional setup after loading the view, typically from a nib.
- (void)viewDidLoad
{
    [super viewDidLoad];
}
*/

- (void)viewDidUnload {
    [super viewDidUnload];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    return YES;
}

@end
