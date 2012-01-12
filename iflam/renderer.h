#ifndef __RENDERER_H__
#define __RENDERER_H__

#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/scoped_array.hpp>
#include <boost/utility.hpp>
#include <boost/gil/gil_all.hpp>

#include "genome.h"

class RenderBuffer {
  public:
    RenderBuffer(const Genome& genome, size_t width, size_t height);
    ~RenderBuffer();

    size_t height() const { return height_; }
    size_t width() const { return width_; }

    void Update(int x, int y, const Color& c, Float opacity);

    void Render(boost::gil::rgb8_view_t* image);
  private:
    const Genome& genome_;
    const size_t width_;
    const size_t height_;

    const Float scale_;  // duplicated with render state
    const Float ppux_;  // duplicated with render state
    const Float ppuy_;  // duplicated with render state

    size_t samples_;
    boost::scoped_array<Float> accum_;
};

class RenderState {
  public:
    RenderState(const Genome& genome, RenderBuffer* buffer);
    ~RenderState();

    void Iterate(int iterations);
  private:
    void Reseed();

    void CreateXformDist(int xi, int xf);
    const Xform& PickRandomXform();

    const Genome& genome_;
    RenderBuffer* buffer_;

    array<Float, 3> xyc_;

    const Float scale_;
    const Float ppux_;
    const Float ppuy_;
    const Float genome_height_;
    const Float genome_width_;
    const Float view_left_;
    const Float view_bottom_;
    const Float view_height_;
    const Float view_width_;

    boost::scoped_array<int> xform_distrib_;  // xforms.size() * kChooseXformGrain
    bool chaos_enabled_;
    size_t last_xform_;
};

class PixelInterface {
public:
    virtual void SetPixel(int x, int y, float r, float g, float b) = 0;
};

#endif
