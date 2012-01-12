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
#include <boost/gil/extension/io/png_io.hpp>


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

class BufferImage {
public:
    BufferImage(uint8_t* buffer, size_t bytes_per_row, size_t height) : buffer_(buffer), bytes_per_row_(bytes_per_row), height_(height) { }
    
    void Set(int x, int y, Float r, Float g, Float b, Float a) {
        size_t offset = x * 4 + (height_ - y - 1) * bytes_per_row_;
        buffer_[offset + 0] = uint8_t(r);
        buffer_[offset + 1] = uint8_t(g);
        buffer_[offset + 2] = uint8_t(b);
    }
    
private:
    uint8_t* buffer_;
    size_t bytes_per_row_;
    size_t height_;
};

class Rgb8Image {
public:
    Rgb8Image(boost::gil::rgb8_view_t* view) : view_(view) { }
    typedef boost::gil::rgb8_view_t::reference pixel_t_ref;
    
    void Set(int x, int y, Float r, Float g, Float b, Float a) {
        pixel_t_ref pixel = (*view_)(x, y);
        pixel[0] = r;
        pixel[1] = g;
        pixel[2] = b;
    }
    
private:
    boost::gil::rgb8_view_t* view_;
};


-(void)main {
    if (self.isCancelled) {
        return;
    }
    
    size_t iterations = 1000000;
    NSLog(@"IterateOperation: Starting %lu iterations", iterations);
    
    RenderBuffer render_buffer(*genome_, width_, height_);
    RenderState state(*genome_, &render_buffer);
    state.Iterate(iterations);
    
    if (self.isCancelled) {
        return;
    }

    NSLog(@"IterateOperation: Rendering %u x %u", width_, height_);
    
    size_t bytes_per_row = width_ * 4;
    bytes_per_row += (16 - bytes_per_row % 16) % 16;

    uint8_t* image_data = new uint8_t[bytes_per_row * height_];
    BufferImage image(image_data, bytes_per_row, height_);
    render_buffer.Render(&image);

    {   // Save result to image
        /*
        boost::gil::rgb8_image_t img(width_, height_);
        boost::gil::rgb8_view_t v(view(img));
        
        Rgb8Image rgb8_image(&v);
        render_buffer.Render(&rgb8_image);
        
        boost::gil::png_write_view("/Users/aizatsky/render.png", v);
        */
    };
    
    NSLog(@"IterateOperation: done");

    if (self.isCancelled) {
        delete[] image_data;
        return;
    }
    
    IterationResult* result = [[IterationResult alloc] initWithImage: image_data width: width_ height: height_];
    [delegate_ performSelectorOnMainThread:@selector(iterateDone:) withObject:result waitUntilDone:NO];
}

@end
