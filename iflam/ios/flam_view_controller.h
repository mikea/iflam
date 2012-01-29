#import <UIKit/UIKit.h>
#import "flam_view_model.h"

@interface FlamViewController : UIViewController {
  FlamViewModel* _model;
}

@property (retain) FlamViewModel* model;

- (void)loadGenome:(NSString*)path;

@end
