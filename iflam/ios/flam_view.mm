// vim: filetype=objc
#import "flam_view.h"
#import "genome.h"
#import "renderer.h"

static size_t BytesPerRow(size_t width) {
  size_t bytes_per_row = width * 4;
  bytes_per_row += (16 - bytes_per_row % 16) % 16;
  return bytes_per_row;
}


@implementation FlamView

@synthesize delegate;
@synthesize viewState=_viewState;
@synthesize model=_model;

- (id)initWithFrame:(CGRect)frame model:(FlamViewModel*) model {
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
        NSLog(@"initWithFrame");
        delegate = nil;
        self.model = model;
        [self resetDefaults];
    }

    return self;
}

-(void)awakeFromNib {
  NSLog(@"awakeFromNib");
}

-(void)resetDefaults {
  lock = [[NSLock alloc] init];
  _viewState = [[ViewState alloc] initWithGenome: self.model.genome
                                           width: self.bounds.size.width
                                          height: self.bounds.size.height];
}

-(void)modelChanged {
  // NSLog(@"setGenome");
  [lock lock];
  self.viewState = [[[ViewState alloc]
    initWithGenome: self.model.genome
             width: self.viewState.width
            height: self.viewState.height] autorelease];
  [lock unlock];
  [self setNeedsDisplay];
}

- (void) update {
  [self setNeedsDisplay];
}

- (void) drawRect:(CGRect)rect {
  [lock lock];
 // NSLog(@"drawRect");

  CGRect bounds = self.bounds;

  if (bounds.size.width != self.viewState.width ||
      bounds.size.height != self.viewState.height) {
    self.viewState = [[[ViewState alloc]
      initWithGenome: self.model.genome
               width: bounds.size.width
              height: bounds.size.height] autorelease];
  }

  self.viewState.renderState->Iterate(100000);
  BufferImage image(
      self.viewState.imageData,
      BytesPerRow(self.viewState.width),
      self.viewState.height);
  self.viewState.renderBuffer->Render(&image);

  CGImageRef im = CGBitmapContextCreateImage(self.viewState.bitmapContext);

  CGContextRef context = UIGraphicsGetCurrentContext();

  CGContextDrawImage(context,
      CGRectMake(
        bounds.origin.x,
        bounds.origin.y,
        bounds.size.width,
        bounds.size.height),
      im);
  CGImageRelease(im);

  [lock unlock];
  [self performSelectorOnMainThread:@selector(update)
                         withObject:nil
                      waitUntilDone:false];
}


- (void)dealloc {
  [super dealloc];
}

/*
- (void)magnifyWithEvent:(UIEvent *)event {
  double magnification = [event magnification];

  [lock lock];
  Genome* genome = new Genome(*_genome);
  genome->Magnify(magnification);
  // delete old_genome;
  [lock unlock];
  [self setGenome: genome];
}

- (void)rotateWithEvent:(NSEvent *)event {
  double rotation = [event rotation];
  NSLog(@"rotate: %f", rotation);
  [lock lock];
  Genome* genome = new Genome(*_genome);
  genome->Rotate(rotation);
  // delete old_genome;
  [lock unlock];
  [self setGenome: genome];
}

-(void)scrollWheel:(UIEvent *)event {
  double deltaX = [event deltaY];
  double deltaY = [event deltaX];
  if (deltaX == 0 && deltaY == 0) {
    return;
  }

  [lock lock];

  CGRect bounds = self.bounds;
  deltaX = - 2 * deltaX * self.viewState.renderState->view_width() / bounds.size.width;
  deltaY = - 2 * deltaY * self.viewState.renderState->view_height() / bounds.size.height;
  Genome* genome = new Genome(*_genome);
  genome->Move(deltaX, deltaY);
  // delete old_genome;
  [lock unlock];
  [self setGenome: genome];
}
*/
-(BOOL)isOpaque {
  return YES;
}

/*
- (void)mouseDown:(NSEvent*) anEvent {
  if (delegate && [delegate respondsToSelector:@selector(onMouseDown:)]) {
    [delegate onMouseDown: anEvent];
  }
}
*/
@end



@implementation ViewState

@synthesize genome=_genome;
@synthesize width=_width;
@synthesize height=_height;
@synthesize imageData=_imageData;
@synthesize renderBuffer=_renderBuffer;
@synthesize renderState=_renderState;
@synthesize bitmapContext=_bitmapContext;

- (id)initWithGenome:(Genome*) aGenome
               width: (size_t) aWidth
              height: (size_t) aHeight {
    self = [super init];
    if (self) {
      _lock = [[NSLock alloc] init];
      _genome = aGenome;
      _width = aWidth;
      _height = aHeight;
      _imageData = (uint8_t*) calloc(BytesPerRow(_width) * _height, 1);
      _renderBuffer = new RenderBuffer(*_genome, _width, _height);
      _renderState = new RenderState(*_genome, _renderBuffer);

      CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
      _bitmapContext = CGBitmapContextCreate(
          _imageData,
          _width,
          _height,
          8,
          BytesPerRow(_width),
          colorSpace,
          kCGImageAlphaNoneSkipLast);

      CGColorSpaceRelease(colorSpace);

    }

    return self;
}

-(void)lock {
  [_lock lock];
}

-(void)unlock {
  [_lock unlock];
}

- (void)dealloc {
  delete _renderBuffer;
  delete _renderState;
  free(_imageData);
  [_lock release];
  if (_bitmapContext != NULL) {
    CGContextRelease(_bitmapContext);
  }
  [super dealloc];
}

@end
