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
    RenderBuffer(const Genome& genome, size_t width, size_t height);
    ~RenderBuffer();

    size_t height() const { return height_; }
    size_t width() const { return width_; }

    void Update(size_t x, size_t y, const Color& c, Float opacity);

    template<typename Image>
    void Render(Image* image);
    const Float* accum() const { return accum_.get(); }
    size_t samples() const { return samples_; }
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

    Float view_height() const { return view_height_; }
    Float view_width() const { return view_width_; }
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

    Random rnd;
};

class PixelInterface {
public:
    virtual void SetPixel(int x, int y, float r, float g, float b) = 0;
};

template<typename Image>
void RenderBuffer::Render(Image* image) {
  Float vibrancy = genome_.vibrancy();
  Float gamma = 1.0 / genome_.gamma();
  Float highpow = genome_.highlight_power();
  Float k1 = (genome_.contrast() * genome_.brightness() * kPrefilterWhite * 268.0) / 256;
  Float samples_per_unit = Float(samples_) / (ppux_ * ppuy_);
  Float k2 = 1.0 / (genome_.contrast() * samples_per_unit);

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
            genome_.background()[0],
            genome_.background()[1],
            genome_.background()[2],
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
        a += ((1.0 - alpha) * genome_.background()[rgbi]);
        if (a > 255) a = 255;
        if (a < 0) a = 0;
        t[rgbi] = a;
      }

      t[3] = alpha;

      image->Set(x, y, t[0], t[1], t[2], t[3]);
    }
  }
}


#endif