#ifndef __RENDERER_H__
#define __RENDERER_H__

#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/utility.hpp>

class FlamDefinition : boost::noncopyable {
public:
    FlamDefinition();
    ~FlamDefinition();

    void Randomize();
private:
    friend class FlamRender;

    size_t number_of_functions_;
    std::vector<double> coeffs_;
    std::vector<double> colors_;
};

class PixelInterface {
public:
    virtual void SetPixel(int x, int y, float r, float g, float b) = 0;
};

template<typename Mutex>
class FlamRender : boost::noncopyable {
public:

    FlamRender(Mutex* mutex, size_t width, size_t height)
    :  mutex_(mutex), width_(width), height_(height) {
        histogram_.resize(width * height);
        color_histogram_.resize(width * height * 3);
    }

    void Render(const FlamDefinition& defition);
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

    void Lock() { mutex_->Lock(); }
    void Unlock() { mutex_->Unlock(); }

    boost::scoped_ptr<Mutex> mutex_;
    size_t width_;
    size_t height_;

    std::vector<int32_t> histogram_;
    std::vector<double> color_histogram_;

};



#endif
