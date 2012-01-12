//
//  FlamView.h
//  iflam
//
//  Created by Mike Aizatskyi on 1/3/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "genome.h"


@interface FlamView : NSView {
@private
    NSLock* image_lock_;
    
    NSOperationQueue* operation_queue_;
    Genome *genome_;

    
    size_t width_;
    size_t height_;
    
    uint8_t* image_data_;
    CGContextRef bitmap_context_;

    uint8_t* image_data2_;
    CGContextRef bitmap_context2_;
}

@end
