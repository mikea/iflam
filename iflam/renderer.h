#ifndef __RENDERER_H__
#define __RENDERER_H__

#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/utility.hpp>

class Genome;

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
