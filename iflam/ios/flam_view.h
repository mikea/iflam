#import <UIKit/UIKit.h>

class Genome;
class RenderBuffer;
class RenderState;


@interface ViewState : NSObject {
  NSLock*   _lock;
  Genome*   _genome;
  size_t    _width;
  size_t    _height;
  uint8_t*  _imageData;

  RenderBuffer* _renderBuffer;
  RenderState* _renderState;
  CGContextRef _bitmapContext;
}

@property (readonly) size_t width;
@property (readonly) size_t height;
@property (readonly) Genome* genome;
@property (readonly) uint8_t* imageData;
@property (readonly) RenderBuffer* renderBuffer;
@property (readonly) RenderState* renderState;
@property (readonly) CGContextRef bitmapContext;

-(id)initWithGenome:(Genome*) genome
              width:(size_t) width
             height:(size_t) height;

-(void)lock;
-(void)unlock;

@end

@protocol FlamViewDelegate
@optional
- (void)onMouseDown:(UIEvent*) anEvent;
@end

@interface FlamView : UIView {
@private
    NSLock* lock;
    Genome* _genome;

    ViewState* _viewState;
    id delegate;
}

@property (retain) ViewState* viewState;
@property (assign) IBOutlet id delegate;

-(void)setGenome:(Genome*) genome;
-(void)resetDefaults;

@end
