// vim: filetype=objc
#import "flam_view_model.h"
#include "genome.h"

@implementation FlamViewModel

@synthesize genome=_genome;

- (id)init {
    self = [super init];
    if (self) {
      _genome = new Genome();
    }

    return self;
}

@end

