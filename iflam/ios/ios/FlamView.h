#import <UIKit/UIKit.h>

class FlamComponent;

@interface FlamView : UIView {
    FlamComponent* _component;
    uint8_t* _data;
    CGContextRef _bitmapContext;
}

- (id)initWithFrame:(CGRect)frame component:(FlamComponent*) component;

@end
