//
//  IterateOperation.m
//  iflam
//
//  Created by Mike Aizatskyi on 1/11/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import "IterateOperation.h"
#import "genome.h"
#import "renderer.h"


@implementation IterationResult 

@synthesize image;
@synthesize width;
@synthesize height;


-(id)initWithImage:(uint8_t*) anImage width:(size_t)aWidth height:(size_t)aHeight
{
    self = [super init];
    if (self) {
        self->image = anImage;
        self->width = aWidth;
        self->height = aHeight;
    }
    
    return self;
}

@end

@implementation IterateOperation

- (id)initWithDelegate:(id)delegate genome:(Genome*)genome width:(size_t)width height:(size_t)height
{
    self = [super init];
    if (self) {
        delegate_ = delegate;
        genome_ = genome;
        width_ = width;
        height_ = height;
    }
    
    return self;
}

- (void)dealloc
{
    [super dealloc];
}

-(void)main {
    if (self.isCancelled) {
        return;
    }
    
    size_t iterations = 10000;
    NSLog(@"IterateOperation: Starting %lu iterations", iterations);
    
    RenderBuffer render_buffer(*genome_, width_, height_);
    RenderState state(*genome_, &render_buffer);
    state.Iterate(iterations);
    
    NSLog(@"IterateOperation: Rendering %u x %u", width_, height_);
    uint8_t* image_data = new uint8_t[width_ * height_ * 4];
    render_buffer.Render(image_data);

    NSLog(@"IterateOperation: done");

    if (self.isCancelled) {
        delete[] image_data;
        return;
    }
    
    IterationResult* result = [[IterationResult alloc] initWithImage: image_data width: width_ height: height_];
    [delegate_ performSelectorOnMainThread:@selector(iterateDone:) withObject:result waitUntilDone:NO];
}

@end
