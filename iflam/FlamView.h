//
//  FlamView.h
//  iflam
//
//  Created by Mike Aizatskyi on 1/3/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "renderer.h"


@interface FlamView : NSView {
@private
    CGContextRef bitmapContext;
    FlamDefinition* def;
}

@end
