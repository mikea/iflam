// vim: filetype=objc
#import "FlamView.h"

#import "genome.h"
#import "renderer.h"
#import "component.h"

static size_t BytesPerRow(size_t width) {
  size_t bytes_per_row = width * 4;
  bytes_per_row += (16 - bytes_per_row % 16) % 16;
  return bytes_per_row;
}


@implementation FlamView

@synthesize delegate;
@synthesize component;

- (id)initWithFrame:(NSRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
        NSLog(@"initWithFrame");
        delegate = nil;
        _data = nil;
        _bitmapContext = nil;
        _component = nil;
        [self resetDefaults];
    }

    return self;
}

-(void)awakeFromNib {
  NSLog(@"awakeFromNib");
}

-(void)resetDefaults {
  lock = [[NSLock alloc] init];
}

-(void)update {
  [self setNeedsDisplay: YES];
}

- (void) drawRect:(NSRect)rect {
  if (self.component == nil) {
    [self performSelectorOnMainThread:@selector(update)
                           withObject:nil
                        waitUntilDone:false];
    return;
  }

  [lock lock];
 // NSLog(@"drawRect");

  NSRect bounds = self.bounds;
  size_t width = bounds.size.width;
  size_t height = bounds.size.height;

  if (self.component->width() != width ||
      self.component->height() != height) {
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

  self.component->SetSize(width, height);
  self.component->Tick();

  RGBA8Image image(
      _data,
      BytesPerRow(width),
      height);
  self.component->Render(&image);

  CGImageRef im = CGBitmapContextCreateImage(_bitmapContext);

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

  [lock unlock];
  [self performSelectorOnMainThread:@selector(update)
                         withObject:nil
                      waitUntilDone:false];
}


- (void)magnifyWithEvent:(NSEvent *)event {
  /*
  double magnification = [event magnification];

  [lock lock];
  Genome* genome = new Genome(*_genome);
  genome->Magnify(magnification);
  // delete old_genome;
  [lock unlock];
  [self setGenome: genome];*/
}

- (void)rotateWithEvent:(NSEvent *)event {
 /* double rotation = [event rotation];
  NSLog(@"rotate: %f", rotation);
  [lock lock];
  Genome* genome = new Genome(*_genome);
  genome->Rotate(rotation);
  // delete old_genome;
  [lock unlock];
  [self setGenome: genome];*/
}

-(void)scrollWheel:(NSEvent *)event {
/*  double deltaX = [event deltaY];
  double deltaY = [event deltaX];
  if (deltaX == 0 && deltaY == 0) {
    return;
  }

  [lock lock];

  NSRect bounds = self.bounds;
  deltaX = - 2 * deltaX * self.viewState.renderState->view_width() / bounds.size.width;
  deltaY = - 2 * deltaY * self.viewState.renderState->view_height() / bounds.size.height;
  Genome* genome = new Genome(*_genome);
  genome->Move(deltaX, deltaY);
  // delete old_genome;
  [lock unlock];
  [self setGenome: genome];*/
}

-(BOOL)isOpaque {
  return YES;
}

- (void)mouseDown:(NSEvent*) anEvent {
  if (delegate && [delegate respondsToSelector:@selector(onMouseDown:)]) {
    [delegate onMouseDown: anEvent];
  }
}

@end
