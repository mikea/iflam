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

@synthesize viewState=_viewState;

- (id)initWithFrame:(NSRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
        bitmap_context_ = nil;

        lock = [[NSLock alloc] init];
        operation_queue_ = [NSOperationQueue new];
        [operation_queue_ setMaxConcurrentOperationCount: 1];

        genome = new Genome();
        genome->Read("/Users/aizatsky/Projects/iflam/flam-java/flams/e_6.flam3");
        _viewState = [[ViewState alloc] initWithGenome: genome
                                                 width: 0
                                                height: 0];
    }

    return self;
}

- (void)iterateDone:(ViewState*) aViewState {
    [lock lock];
    [aViewState lock];

    if (bitmap_context_ != NULL) {
      CGContextRelease(bitmap_context_);
      bitmap_context_ = NULL;
    }

    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    bitmap_context_ = CGBitmapContextCreate(
        aViewState.imageData,
        aViewState.width,
        aViewState.height,
        8,
        BytesPerRow(aViewState.width),
        colorSpace,
        kCGImageAlphaNoneSkipLast);

    CGColorSpaceRelease(colorSpace);

    [aViewState unlock];

    if (aViewState == self.viewState) {
      // enqueue another iterate operation.
      IterateOperation* operation = [[[IterateOperation alloc]
        initWithDelegate: self
               viewState: self.viewState] autorelease];
      [operation_queue_ addOperation:operation];
    }

    [lock unlock];
    [self setNeedsDisplay:YES];
    [aViewState release];
}


- (void) drawRect:(NSRect)rect {
    [lock lock];

    NSRect bounds = self.bounds;

    if (bitmap_context_ != nil) {
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

    ViewState* viewState = [[self.viewState retain] autorelease];

    [viewState lock];
    size_t width = (size_t) bounds.size.width;
    size_t height = (size_t) bounds.size.height;
    if (viewState.width != width ||
        viewState.height != height ||
        viewState.genome != genome) {
      [operation_queue_ cancelAllOperations];

      ViewState* newState = [[[ViewState alloc]
        initWithGenome: genome
                 width: width
                height: height] autorelease];

      IterateOperation* operation = [[[IterateOperation alloc]
        initWithDelegate: self
               viewState: newState] autorelease];
      [operation_queue_ addOperation:operation];

      [viewState unlock];
      [newState lock];
      self.viewState = newState;
      viewState = newState;
    }

    [viewState unlock];
    [lock unlock];
}


- (void)dealloc {
    if (bitmap_context_ != NULL) {
        CGContextRelease(bitmap_context_);
    }
    [super dealloc];
}

- (void)magnifyWithEvent:(NSEvent *)event {
  double magnification = [event magnification];
  [lock lock];
  Genome* old_genome = genome;
  genome = new Genome(*genome);
  genome->Magnify(magnification);
  // delete old_genome;
  [lock unlock];
  [self setNeedsDisplay:YES];
}

- (void)rotateWithEvent:(NSEvent *)event {
  double rotation = [event rotation];
  NSLog(@"rotate: %f", rotation);
  [lock lock];
  Genome* old_genome = genome;
  genome = new Genome(*genome);
  genome->Rotate(rotation);
  // delete old_genome;
  [lock unlock];
  [self setNeedsDisplay:YES];
}

-(void)scrollWheel:(NSEvent *)event {
  double deltaX = [event deltaY];
  double deltaY = [event deltaX];
  if (deltaX == 0 && deltaY == 0) {
    return;
  }

  [lock lock];

  NSRect bounds = self.bounds;
  deltaX = - 2 * deltaX * self.viewState.renderState->view_width() / bounds.size.width;
  deltaY = - 2 * deltaY * self.viewState.renderState->view_height() / bounds.size.height;
  Genome* old_genome = genome;
  genome = new Genome(*genome);
  genome->Move(deltaX, deltaY);
  // delete old_genome;
  [lock unlock];
  [self setNeedsDisplay:YES];
}

-(BOOL)isOpaque {
  return YES;
}

@end

@implementation IterateOperation

@synthesize viewState=_viewState;

- (id)initWithDelegate:(id) delegate
             viewState:(ViewState*) aViewState {
    self = [super init];
    if (self) {
        delegate_ = delegate;
        self.viewState = aViewState;
    }

    return self;
}

- (void)dealloc {
  [_viewState release];
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
    // We are already cancelled. Don't even bother to start.
    return;
  }

  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

  [self.viewState lock];
  size_t iterations = 100;
  size_t batch = 1000;
  //NSLog(@"IterateOperation: Starting %lu iterations", iterations);

  for (size_t i = 0; i < iterations; ++i) {
    self.viewState.renderState->Iterate(batch);
    if (self.isCancelled) {
      // We've already rendered something. Deliver results.
      break;
    }
  }

  BufferImage image(
      self.viewState.imageData,
      BytesPerRow(self.viewState.width),
      self.viewState.height);
  self.viewState.renderBuffer->Render(&image);

  [delegate_ performSelectorOnMainThread:@selector(iterateDone:)
                              withObject:[self.viewState retain]
                           waitUntilDone:NO];

  [self.viewState unlock];
  [pool release];
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
      lock = [[NSLock alloc] init];
      genome = aGenome;
      width = aWidth;
      height = aHeight;
      imageData = (uint8_t*) calloc(BytesPerRow(width) * height, 1);
      renderBuffer = new RenderBuffer(*genome, width, height);
      renderState = new RenderState(*genome, renderBuffer);
    }

    return self;
}

-(void)lock {
  [lock lock];
}

-(void)unlock {
  [lock unlock];
}

- (void)dealloc {
  delete renderBuffer;
  delete renderState;
  free(imageData);
  [lock release];
  [super dealloc];
}

@end
