// vim: filetype=objc
#import "FlamView.h"
#import "genome.h"
#import "renderer.h"

static size_t BytesPerRow(size_t width) {
  size_t bytes_per_row = width * 4;
  bytes_per_row += (16 - bytes_per_row % 16) % 16;
  return bytes_per_row;
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


@implementation FlamView

@synthesize delegate;
@synthesize viewState=_viewState;

- (id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
        NSLog(@"initWithFrame");
        delegate = nil;
        [self resetDefaults];
    }

    return self;
}

-(void)awakeFromNib {
  NSLog(@"awakeFromNib");
}

-(void)resetDefaults {
  lock = [[NSLock alloc] init];
  _genome = new Genome();
  _viewState = [[ViewState alloc] initWithGenome: _genome
                                           width: self.bounds.size.width
                                          height: self.bounds.size.height];
}

-(void)setGenome:(Genome*) aGenome {
  // NSLog(@"setGenome");
  [lock lock];
  delete _genome;
  _genome = aGenome;
  ViewState* newState = [[[ViewState alloc]
    initWithGenome: _genome
             width: self.viewState.width
            height: self.viewState.height] autorelease];

  self.viewState = newState;
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
