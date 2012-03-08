#import <UIKit/UIKit.h>

#include "component.h"

class FlamGLView;

@interface FlamView : UIView {
    CAEAGLLayer* _eaglLayer;
    EAGLContext* _context;
    
    boost::shared_ptr<FlamComponent> _component;
    FlamGLView* _gl_view;
    uint8_t* _data;
    CGContextRef _bitmapContext;
}

- (id)initWithFrame:(CGRect)frame component:(boost::shared_ptr<FlamComponent>*) component;

@end
