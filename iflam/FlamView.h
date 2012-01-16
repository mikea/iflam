//
//  FlamView.h
//  iflam
//
//  Created by Mike Aizatskyi on 1/3/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>

class Genome;
class RenderBuffer;
class RenderState;


@interface ViewState : NSObject {
@private
  NSLock* lock;

  Genome*   genome;
  size_t    width;
  size_t    height;
  uint8_t*  imageData;

  RenderBuffer* renderBuffer;
  RenderState* renderState;
}

@property (readonly) size_t width;
@property (readonly) size_t height;
@property (readonly) Genome* genome;
@property (readonly) uint8_t* imageData;
@property (readonly) RenderBuffer* renderBuffer;
@property (readonly) RenderState* renderState;

-(id)initWithGenome:(Genome*) genome
              width:(size_t) width
             height:(size_t) height;

-(void)lock;
-(void)unlock;

@end



@interface FlamView : NSView {
@private
    NSLock* lock;
    Genome* genome;

    NSOperationQueue* operation_queue_;
    ViewState* _viewState;
    CGContextRef bitmap_context_;
}

@property (retain) ViewState* viewState;

@end

@interface IterateOperation : NSOperation {
@private
    id delegate_;
    ViewState* _viewState;
}

@property (retain) ViewState* viewState;

-(id)initWithDelegate:(id)delegate
            viewState:(ViewState*)viewState;

@end

