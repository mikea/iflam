#import <UIKit/UIKit.h>

class Genome;

@interface FlamViewModel : NSObject {
  Genome* _genome;
}

@property (assign) Genome* genome;


@end

