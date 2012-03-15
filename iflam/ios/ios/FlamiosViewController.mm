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
            _controller.reset(new SlideshowController([path UTF8String]));
            _component = new FlamComponent(_controller);
        }
        
    }
    
    return self;
}

- (void) loadView {
    CGRect screenBounds = [[UIScreen mainScreen] bounds];
    self.view = [[FlamView alloc]initWithFrame:screenBounds component:_component];
    UISwipeGestureRecognizer* swipeRecognizer = [[UISwipeGestureRecognizer alloc]
                                                 initWithTarget: self
                                                         action: @selector(handleSwipe:)];
    swipeRecognizer.direction = UISwipeGestureRecognizerDirectionLeft;
    [self.view addGestureRecognizer: swipeRecognizer];
    
    UIToolbar* toolbar = [[UIToolbar alloc] initWithFrame: CGRectMake(0, 0, screenBounds.size.width, 24)];
    toolbar.translucent = YES;
    toolbar.barStyle = UIBarStyleBlack;
    //toolbar.autoresizingMask |= UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
    //toolbar.shouldAutorotateToInterfaceOrientation = YES;
    [self.view addSubview:toolbar];
}

- (void) handleSwipe: (UIGestureRecognizer *)sender {
    NSLog(@"handleSwipe");
    _controller->Next();
}

- (void)dealloc {
    _controller.reset();
    delete _component;
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
