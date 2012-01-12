//
//  IterateOperation.h
//  iflam
//
//  Created by Mike Aizatskyi on 1/11/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>

class Genome;

@interface IterationResult : NSObject {
@private
    size_t width;
    size_t height;
    uint8_t* image;
}
-(id)initWithImage:(uint8_t*) image width:(size_t)width height:(size_t)height;

@property (readonly) size_t width;
@property (readonly) size_t height;
@property (readonly) uint8_t* image;

@end

@interface IterateOperation : NSOperation {
@private
    id delegate_;
    Genome* genome_;
    size_t width_;
    size_t height_;
}

-(id)initWithDelegate:(id)delegate genome:(Genome*)genome width:(size_t)width height:(size_t)height;

@end
