#ifndef __RENDERER_H__
#define __RENDERER_H__

#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/scoped_array.hpp>
#include <boost/utility.hpp>

class Genome;

class RenderBuffer {
  public:
    RenderBuffer(size_t width, size_t height);
    ~RenderBuffer();

    size_t height() const { return height_; }
    size_t width() const { return width_; }

    void Update(int x, int y, const Color& c, double opacity);
  private:
    const size_t width_;
    const size_t height_;
    boost::scoped_array<double> accum_;
};

class RenderState {
  public:
    RenderState(const Genome& genome, RenderBuffer* buffer);
    ~RenderState();

    void Iterate();
  private:
    void Reseed();

    void CreateXformDist(int xi, int xf);
    const Xform& PickRandomXform();

    const Genome& genome_;
    RenderBuffer* buffer_;

    boost::scoped_array<double> xyc_;

    const double scale_;
    const double ppux_;
    const double ppuy_;
    const double view_left_;
    const double view_bottom_;
    const double view_height_;
    const double view_width_;

    boost::scoped_array<int> xform_distrib_;  // xforms.size() * kChooseXformGrain
    bool chaos_enabled_;
    size_t last_xform_;
};

class PixelInterface {
public:
    virtual void SetPixel(int x, int y, float r, float g, float b) = 0;
};

class FlamRender : boost::noncopyable {
public:

    FlamRender(size_t width, size_t height)
    :  width_(width), height_(height) {
        histogram_.resize(width * height);
        color_histogram_.resize(width * height * 3);
    }

    void Render(const Genome& defition);
    void Visualize(PixelInterface* pixel_interface);
private:
    class RenderState : boost::noncopyable {
        double x;
        double y;
        double r;
        double g;
        double b;
    };

    void UpdateHistogram(const RenderState& state);

    size_t width_;
    size_t height_;

    std::vector<int32_t> histogram_;
    std::vector<double> color_histogram_;

};



#endif
