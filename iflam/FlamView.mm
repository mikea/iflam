#import "FlamView.h"
#import "IterateOperation.h"

@implementation FlamView


- (id)initWithFrame:(NSRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
        genome_ = new Genome();
        genome_->Read("/Users/aizatsky/Projects/iflam/flam-java/flams/e_1.flam3");
        bitmap_context_ = nil;

        image_lock_ = [[NSLock alloc] init];
        operation_queue_ = [NSOperationQueue new];
        width_ = 0;
        height_ = 0;
    }

    return self;
}

- (void)iterateDone:(id) id_result {
    [image_lock_ lock];

    IterationResult* result = (IterationResult*) id_result;
    NSLog(@"iterateDone: %lu x %lu", result.width, result.height);

    if (bitmap_context_ != NULL) {
        CGContextRelease(bitmap_context_);
        bitmap_context_ = NULL;
        delete[] image_data_;
    }
    
    width_ = result.width;
    height_ = result.height;
    image_data_ = result.image;

    size_t bytes_per_row = width_ * 4;
    bytes_per_row += (16 - bytes_per_row % 16) % 16;
    
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    bitmap_context_ = CGBitmapContextCreate(image_data_, width_, height_, 8, bytes_per_row, colorSpace, kCGImageAlphaNoneSkipLast);
    
    CGColorSpaceRelease(colorSpace);

    [image_lock_ unlock];

    [self setNeedsDisplay:YES];
}


- (void) drawRect:(NSRect)rect {
    [image_lock_ lock];
    
    NSRect bounds = self.bounds;
    size_t width = (size_t) bounds.size.width;
    size_t height = (size_t) bounds.size.height;

    if (width_ != width || height_ != height) {
        IterateOperation* operation = [[IterateOperation alloc] initWithDelegate: self genome:genome_ width:width height:height];
        [operation_queue_ addOperation:operation];
    } 
    
    if (bitmap_context_ != nil) {
        [[NSColor redColor] set];
        [NSBezierPath fillRect:rect];

        NSLog(@"Drawing bitmap of %d x %d size onto %d x %d view",
              CGBitmapContextGetWidth(bitmap_context_),
              CGBitmapContextGetHeight(bitmap_context_),
              width,
              height);
        CGImageRef im = CGBitmapContextCreateImage(bitmap_context_);

        CGContextRef context = (CGContextRef) [[NSGraphicsContext currentContext] graphicsPort];
        CGContextDrawImage(context, CGRectMake(bounds.origin.x, bounds.origin.y, bounds.size.width, bounds.size.height), im);
        CGImageRelease(im); 
    } 

    [image_lock_ unlock];
}


- (void)dealloc
{
    if (bitmap_context_ != NULL) {
        CGContextRelease(bitmap_context_);
        delete[] image_data_;
    }
    delete genome_;
    [super dealloc];
}

@end
