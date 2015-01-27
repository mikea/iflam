#ifndef __RENDERER_H__
#define __RENDERER_H__

#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/scoped_array.hpp>
#include <boost/utility.hpp>
#include <boost/gil/gil_all.hpp>

#include "common.h"
#include "genome.h"

double _pow(double x, double y);

void CalcNewRgb(Float* rgb, Float* newRgb, Float ls, Float highpow);

Float AdjustPercentage(Float p);

const Float kPrefilterWhite = 255;

class RenderBuffer {
public:

    // Shouldn't depend on genome.
    RenderBuffer(size_t width, size_t height);

    ~RenderBuffer();

    size_t height() const {
        return height_;
    }

    size_t width() const {
        return width_;
    }

    void Update(size_t x, size_t y, const Color& c, Float opacity);

    template<typename Image>
    void Render(const Genome& genome, Image* image);

    const Float* accum() const {
        return accum_.get();
    }

    size_t samples() const {
        return samples_;
    }

    // d = [r, g, b, density]
    void at(size_t x, size_t y, double* d) {
        size_t offset = (x + width_ * y) * 4;
        d[0] = accum_[offset];
        d[1] = accum_[offset + 1];
        d[2] = accum_[offset + 2];
        d[3] = accum_[offset + 3];
    }

    Float k1(const Genome& genome) const {
        return (genome.contrast() * genome.brightness() * kPrefilterWhite * 268.0)
                / 256;
    }

    float k2(const Genome& genome) const {
        double s = scale(genome);
        Float samples_per_unit = Float(samples_)
                / (genome.pixels_per_unit() * genome.pixels_per_unit() * s * s);
        return 1.0 / (genome.contrast() * samples_per_unit);
    }

    static double scale(const Genome& genome) {
        return pow(2, genome.zoom());
    }

    Float max_density() const {
        return max_density_;
    }

    void Reset() {
        samples_ = 0;
        max_density_ = 0;
        memset(accum_.get(), 0, sizeof(Float) * width_ * height_ * 4);
    }

private:
    const size_t width_;
    const size_t height_;

    size_t samples_;
    Float max_density_;

    boost::scoped_array<Float> accum_;
};

class RenderState {
public:

    RenderState(const Genome& genome, RenderBuffer* buffer);

    ~RenderState();

    void Iterate(int iterations);

    Float view_height() const {
        return view_height_;
    }

    Float view_width() const {
        return view_width_;
    }
private:

    void Reseed();

    void IterateImpl(int iterations);

    int DoIterationRound(int i);

    void UpdateBuffer(Float x, Float y, Float a, Float opacity);

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


    Float rotate1_;
    Float rotate2_;
    int consequent_errors_;

    boost::scoped_array<int> xform_distrib_;  // xforms.size() * kChooseXformGrain
    bool chaos_enabled_;
    size_t last_xform_;

    Random rnd;
};

class PixelInterface {
public:

    virtual void SetPixel(int x, int y, float r, float g, float b) = 0;

};

template<typename Image>
void RenderBuffer::Render(const Genome& genome, Image* image) {
    Float vibrancy = genome.vibrancy();
    Float gamma = 1.0 / genome.gamma();
    Float highpow = genome.highlight_power();
    Float k1 = this->k1(genome);
    Float k2 = this->k2(genome);

    Float newrgb[4] = {0, 0, 0, 0};

    for (size_t y = 0; y < height_; ++y) {
        for (size_t x = 0; x < width_; ++x) {
            size_t offset = (x + width_ * y) * 4;
            Float cr = accum_[offset];
            Float cg = accum_[offset + 1];
            Float cb = accum_[offset + 2];
            Float freq = accum_[offset + 3];

            if (freq == 0) {
                image->Set(x, y,
                        genome.background()[0],
                        genome.background()[1],
                        genome.background()[2],
                        255);
                continue;
            }

            Float ls = (k1 * log(1.0 + freq * k2)) / freq;
            freq *= ls;
            cr *= ls;
            cg *= ls;
            cb *= ls;

            Float tmp = freq / kPrefilterWhite;
            Float alpha = _pow(tmp, gamma);
            ls = vibrancy * 256.0 * alpha / tmp;
            if (alpha < 0.0) alpha = 0.0;
            if (alpha > 1.0) alpha = 1.0;

            Float t[4] = {cr, cg, cb, freq};
            CalcNewRgb(t, newrgb, ls, highpow);

            for (int rgbi = 0; rgbi < 3; rgbi++) {
                Float a = newrgb[rgbi];
                a += (1.0 - vibrancy) * 256.0 * _pow(t[rgbi] / kPrefilterWhite, gamma);
                a += ((1.0 - alpha) * genome.background()[rgbi]);
                if (a > 255) a = 255;
                if (a < 0) a = 0;
                t[rgbi] = a;
            }

            t[3] = alpha;

            image->Set(x, y, t[0], t[1], t[2], t[3]);
        }
    }
}

template<typename T>
class RGBAImage {
public:
    RGBAImage(T* buffer, size_t bytes_per_row, size_t height, T alpha = (T) (0xff), T scale = (T) (1))
    : buffer_(buffer), bytes_per_row_(bytes_per_row), height_(height), alpha_(alpha), scale_(scale) {
    }

    void Set(int x, int y, Float r, Float g, Float b, Float /*a*/) {
        size_t offset = x * 4 + (height_ - y - 1) * bytes_per_row_;
        buffer_[offset + 0] = (T) (r) * scale_;
        buffer_[offset + 1] = (T) (g) * scale_;
        buffer_[offset + 2] = (T) (b) * scale_;
        buffer_[offset + 3] = alpha_;
    }

private:
    T* buffer_;
    size_t bytes_per_row_;
    size_t height_;
    T alpha_;
    T scale_;
};

typedef RGBAImage<uint8_t> RGBA8Image;

#endif
