#import "FlamView.h"
#include "component.h"


static size_t BytesPerRow(size_t width) {
    size_t bytes_per_row = width * 4;
    bytes_per_row += (16 - bytes_per_row % 16) % 16;
    return bytes_per_row;
}

// TODO: use RGBA8Image
class BufferImage {
public:
    BufferImage(uint8_t* buffer, size_t bytes_per_row, size_t height) : buffer_(buffer), bytes_per_row_(bytes_per_row), height_(height) { }
    
    void Set(int x, int y, Float r, Float g, Float b, Float /*a*/) {
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


@implementation FlamView

- (id)initWithFrame:(CGRect)frame component:(FlamComponent*) component {
    self = [super initWithFrame:frame];
    if (self) {
        _component = component;
        _data = nil;
        _bitmapContext = nil;
    }
    
    return self;
}

-(void)update {
    [self setNeedsDisplay];
}

- (void) drawRect:(CGRect)rect {
    CGRect bounds = self.bounds;
    size_t width = bounds.size.width;
    size_t height = bounds.size.height;
    
    if (_component->width() != width ||
        _component->height() != height) {
        if (_data != nil) {
            free(_data);
            CGContextRelease(_bitmapContext);
        }
        _data = (uint8_t*) calloc(BytesPerRow(width) * height, 1);
        
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        _bitmapContext = CGBitmapContextCreate(
                                               _data,
                                               width,
                                               height,
                                               8,
                                               BytesPerRow(width),
                                               colorSpace,
                                               kCGImageAlphaNoneSkipLast);
        
        CGColorSpaceRelease(colorSpace);
    }
    
    _component->SetSize(width, height);
    _component->Tick();
    
    RGBA8Image image(
                      _data,
                      BytesPerRow(width),
                      height);
    _component->Render(&image);
    
    CGImageRef im = CGBitmapContextCreateImage(_bitmapContext);
    
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGContextDrawImage(context,
                       CGRectMake(
                                  bounds.origin.x,
                                  bounds.origin.y,
                                  bounds.size.width,
                                  bounds.size.height),
                       im);
    CGImageRelease(im);
    
    [self performSelectorOnMainThread:@selector(update)
                           withObject:nil
                        waitUntilDone:false];
}




@end
