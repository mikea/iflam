//
//  FlamView.h
//  iflam
//
//  Created by Mike Aizatskyi on 1/3/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>

class FlamComponent;


@protocol FlamViewDelegate
@optional
- (void)onMouseDown:(NSEvent*) anEvent;
@end

@interface FlamView : NSView {
@private
    NSLock* lock;

    FlamComponent* _component;
    uint8_t* _data;
    CGContextRef _bitmapContext;
    id delegate;
}

@property (assign) FlamComponent* component;
@property (retain) IBOutlet id delegate;

-(void)resetDefaults;

@end
