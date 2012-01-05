#import "FlamView.h"


@implementation FlamView

class CGBitmapContextPixelInterface : public PixelInterface {
public:
    CGBitmapContextPixelInterface(CGContextRef bitmapContext) 
    : bitmapContext_(bitmapContext) { }
    
    virtual void SetPixel(int x, int y, float r, float g, float b) {
        CGContextSetRGBFillColor(bitmapContext_, r, g, b, 1.0f);
        CGContextFillRect (bitmapContext_, CGRectMake(x, y, 1.0f, 1.0f));
    }

private:
    CGContextRef bitmapContext_;
};

- (id)initWithFrame:(NSRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
        def = new FlamDefinition();
        def->Randomize();
    }
    
    return self;
}


- (void) drawRect:(NSRect)rect {
    NSRect bounds = self.bounds;
    int width = (int) bounds.size.width;
    int height = (int) bounds.size.width;
    
    if (bitmapContext != NULL) {
        if (width != CGBitmapContextGetWidth(bitmapContext) ||
            height != CGBitmapContextGetHeight(bitmapContext)) {
            CGContextRelease(bitmapContext);
            bitmapContext = NULL;
        }
    }
    
    if (bitmapContext == NULL) {
        int bitmapBytesPerRow = (width * 4);
        bitmapBytesPerRow += (16 - bitmapBytesPerRow%16)%16; // make it 16-aligned
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        
        bitmapContext = CGBitmapContextCreate(NULL, width, height, 8, bitmapBytesPerRow, colorSpace, kCGImageAlphaNoneSkipFirst);
        CGColorSpaceRelease(colorSpace);
        
        
        CGContextSetAllowsAntialiasing(bitmapContext, FALSE);
        
        CGBitmapContextPixelInterface pixelInterface(bitmapContext);
        
        FlamRender render(width, height);
        render.Render(*def);
        render.Visualize(&pixelInterface);
    }
    
    CGContextRef context = (CGContextRef) [[NSGraphicsContext currentContext] graphicsPort];
    CGImageRef im = CGBitmapContextCreateImage(self->bitmapContext);
    CGContextDrawImage(context, CGRectMake(bounds.origin.x, bounds.origin.y, bounds.size.width, bounds.size.height), im);
    CGImageRelease(im);
}


- (void)dealloc
{
    if (bitmapContext != NULL) {
        CGContextRelease(bitmapContext);
    }
    delete def;
    [super dealloc];
}

@end
