#import "FlamiosViewController.h"
#import "FlamView.h"

#include "controller.h"
#include "component.h"

@implementation FlamiosViewController

- (id)init {
    self = [super init];
    if (self) {
        {
            NSString* path = [[NSBundle mainBundle] pathForResource: @"sheeps"
                                                             ofType: nil];
            boost::shared_ptr<Controller> slide_show(new SlideshowController([path UTF8String]));
            _component = new FlamComponent(slide_show);
        }
        
    }
    
    return self;
}

- (void) loadView {
    self.view = [[FlamView alloc]initWithFrame:[[UIScreen mainScreen] bounds] component:_component];
}

- (void)dealloc {
    delete _component;
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
