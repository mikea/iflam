// vim: filetype=objc
#import "flam_view_controller.h"
#import "flam_view.h"
#include "genome.h"

@implementation FlamViewController

- (void)loadView {
  self.view = [[FlamView alloc]initWithFrame:[[UIScreen mainScreen] bounds]];
}

- (void)dealloc {
    [super dealloc];
}

- (void)didReceiveMemoryWarning {
    // Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];

    // Release any cached data, images, etc that aren't in use.
}

- (void)loadGenome:(NSString*)path {
  Genome* genome = new Genome();
  genome->Read([path UTF8String]);
  [((FlamView*)self.view) setGenome: genome];
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
    // Release any retained subviews of the main view.
    // e.g. self.myOutlet = nil;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    // Return YES for supported orientations
    return YES;
}

@end
