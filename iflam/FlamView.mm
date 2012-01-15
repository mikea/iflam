// vim: filetype=objc
#import "FlamView.h"
#import "genome.h"
#import "renderer.h"

static size_t BytesPerRow(size_t width) {
  size_t bytes_per_row = width * 4;
  bytes_per_row += (16 - bytes_per_row % 16) % 16;
  return bytes_per_row;
}

@implementation FlamView

- (id)initWithFrame:(NSRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
        bitmap_context_ = nil;

        image_lock_ = [[NSLock alloc] init];
        operation_queue_ = [NSOperationQueue new];

        Genome *genome = new Genome();
        genome->Read("/Users/aizatsky/Projects/iflam/flam-java/flams/e_2.flam3");
        viewState = [[ViewState alloc] initWithGenome: genome
                                                width: 0
                                               height: 0];
    }

    return self;
}

- (void)iterateDone:(ViewState*) aViewState {
    [image_lock_ lock];

    if (aViewState != viewState) {
      NSLog(@"Skipping iteration result for other genome.");
      return;
    }

    NSLog(@"iterateDone: %lu x %lu",
        viewState.width,
        viewState.height);

    if (bitmap_context_ != NULL) {
        CGContextRelease(bitmap_context_);
        bitmap_context_ = NULL;
    }

    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    bitmap_context_ = CGBitmapContextCreate(
        viewState.imageData,
        viewState.width,
        viewState.height,
        8,
        BytesPerRow(viewState.width),
        colorSpace,
        kCGImageAlphaNoneSkipLast);

    CGColorSpaceRelease(colorSpace);

    [image_lock_ unlock];

    IterateOperation* operation = [[IterateOperation alloc]
      initWithDelegate: self
             viewState: viewState];
    [operation_queue_ addOperation:operation];
    [self setNeedsDisplay:YES];
}


- (void) drawRect:(NSRect)rect {
    [image_lock_ lock];

    NSRect bounds = self.bounds;
    size_t width = (size_t) bounds.size.width;
    size_t height = (size_t) bounds.size.height;

    if (viewState.width != width || viewState.height != height) {
      ViewState* newState = [[ViewState alloc]
        initWithGenome: viewState.genome
                 width: width
                height: height];
      [operation_queue_ cancelAllOperations];
      IterateOperation* operation = [[IterateOperation alloc]
        initWithDelegate: self
               viewState: newState];
      [operation_queue_ addOperation:operation];
      viewState = newState;
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

        CGContextRef context = (CGContextRef)
          [[NSGraphicsContext currentContext] graphicsPort];

        CGContextDrawImage(context,
            CGRectMake(
              bounds.origin.x,
              bounds.origin.y,
              bounds.size.width,
              bounds.size.height),
            im);
        CGImageRelease(im);
    }

    [image_lock_ unlock];
}


- (void)dealloc
{
    if (bitmap_context_ != NULL) {
        CGContextRelease(bitmap_context_);
    }
    [super dealloc];
}

@end

@implementation IterateOperation

- (id)initWithDelegate:(id) delegate
             viewState:(ViewState*) aViewState {
    self = [super init];
    if (self) {
        delegate_ = delegate;
        viewState = aViewState;
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


-(void)main {
    if (self.isCancelled) {
        return;
    }

    size_t iterations = 100000;
    NSLog(@"IterateOperation: Starting %lu iterations", iterations);

    viewState.renderState->Iterate(iterations);

    if (self.isCancelled) {
        return;
    }

    NSLog(@"IterateOperation: Rendering %u x %u",
        viewState.width, viewState.height);

    BufferImage image(
        viewState.imageData,
        BytesPerRow(viewState.width),
        viewState.height);
    viewState.renderBuffer->Render(&image);

    NSLog(@"IterateOperation: done");

    if (self.isCancelled) {
        return;
    }

    [delegate_ performSelectorOnMainThread:@selector(iterateDone:)
                                withObject:viewState
                             waitUntilDone:NO];
}

@end


@implementation ViewState

@synthesize genome;
@synthesize width;
@synthesize height;
@synthesize imageData;
@synthesize renderBuffer;
@synthesize renderState;

- (id)initWithGenome:(Genome*) aGenome
               width: (size_t) aWidth
              height: (size_t) aHeight {
    self = [super init];
    if (self) {
      genome = aGenome;
      width = aWidth;
      height = aHeight;
      imageData = (uint8_t*) calloc(BytesPerRow(width) * height, 1);
      renderBuffer = new RenderBuffer(*genome, width, height);
      renderState = new RenderState(*genome, renderBuffer);
    }

    return self;
}

@end
