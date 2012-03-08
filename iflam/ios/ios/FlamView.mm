#import "FlamView.h"
#import <QuartzCore/QuartzCore.h>
#import <OpenGLES/EAGLDrawable.h>

#include "component.h"
#include "flam_gl_view.h"

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

- (id)initWithFrame:(CGRect)frame component:(boost::shared_ptr<FlamComponent>*) component {
    self = [super initWithFrame:frame];
    if (self) {
        _component = *component;
        _gl_view = new FlamGLView(_component);
        
        _data = nil;
        _bitmapContext = nil;

        _eaglLayer = (CAEAGLLayer*)self.layer;
        _eaglLayer.opaque = YES;

        _context = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
        [EAGLContext setCurrentContext: _context];

        // Create framebuffer
        GLuint framebuffer;
        glGenFramebuffers(1, &framebuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        // Create renderbuffer
        GLuint colorRenderbuffer;
        glGenRenderbuffers(1, &colorRenderbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
        [_context renderbufferStorage:GL_RENDERBUFFER fromDrawable:_eaglLayer];
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorRenderbuffer);
        
        GLint width;
        GLint height;
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &width);
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &height);
        
        // Create depth buffer
/*        GLuint depthRenderbuffer;
        glGenRenderbuffers(1, &depthRenderbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbuffer);
  */      
        // Check status
        GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if(status != GL_FRAMEBUFFER_COMPLETE) {
            NSLog(@"failed to make complete framebuffer object %x", status);
            return nil;
        }
        
        _gl_view->Init();
        _gl_view->SetSize(width, height);
        
        CADisplayLink* displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(drawFrame:)];
        [displayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    }
    
    return self;
}

+ (Class) layerClass {
    return [CAEAGLLayer class];
}

-(void)update {
    [self setNeedsDisplay];
}

- (void) drawFrame:(CADisplayLink *)link {
    NSLog(@"drawFrame");
    _gl_view->Render();
    [_context presentRenderbuffer:GL_RENDERBUFFER];
}

/*
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
                                               kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
        CGContextSetInterpolationQuality(_bitmapContext, kCGInterpolationNone);
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
*/



@end
