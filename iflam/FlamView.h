//
//  FlamView.h
//  iflam
//
//  Created by Mike Aizatskyi on 1/3/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "renderer.h"
#import "genome.h"


@interface FlamView : NSView {
@private
    CGContextRef bitmapContext;
    Genome * def;
}

@end
