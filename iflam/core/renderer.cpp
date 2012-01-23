#include "genome.h"
#include "renderer.h"
#include "color.h"

namespace {

const size_t kVLen = 6;
const size_t kFLen = 6 + kVLen;
const size_t kChooseXformGrain = 16384;
const Float kGamma = 2.5;

}  // namespace

double _pow(double a, double b) {
    int tmp = (*(1 + (int *)&a));
    int tmp2 = (int)(b * (tmp - 1072632447) + 1072632447);
    double p = 0.0;
    *(1 + (int * )&p) = tmp2;
    return p;
}

Float AdjustPercentage(Float p) {
  if (p == 0) {
    return p;
  } else {
    return pow(10, -log(1.0/p)) / log(2);
  }
}


void CalcNewRgb(Float* rgb, Float* newRgb, Float ls, Float highpow) {
  if (ls == 0.0 || (rgb[0] == 0.0 && rgb[1] == 0.0 && rgb[2] == 0.0)) {
    newRgb[0] = 0.0;
    newRgb[1] = 0.0;
    newRgb[2] = 0.0;
    return;
  }

  /* Identify the most saturated channel */
  Float maxA = -1.0;
  Float maxC = 0;
  Float maxComponent = -1;
  for (int i = 0; i < 3; i++) {
    Float c = rgb[i];
    if (c > maxComponent) {
      maxA = ls * (c / kPrefilterWhite);
      maxC = c / kPrefilterWhite;
      maxComponent = c;
    }
  }

  // If a channel is saturated and we have a non-negative highlight power
  // modify the color to prevent hue shift
  if (maxA > 255 && highpow >= 0.0) {
    // Calculate the max-value color (ranged 0 - 1)
    for (int i = 0; i < 3; i++)
      newRgb[i] = rgb[i] / maxComponent;

    // Reduce saturation by the lsratio
    boost::scoped_array<Float> newHsv(new Float[3]);
    rgb2hsv(newRgb, newHsv.get());
    newHsv[1] *= pow(255.0 / (ls * maxC), highpow);
    hsv2rgb(newHsv.get(), newRgb);

    for (int i = 0; i < 3; i++) {
      newRgb[i] *= 255.0;
    }
  } else {
    Float newLs = 255.0 / maxC;
    Float adjHlp = -highpow;
    if (adjHlp > 1 || maxA <= 255) {
      adjHlp = 1;
    }

    double k = ((1.0 - adjHlp) * newLs + adjHlp * ls);

    /* Calculate the max-value color (ranged 0 - 1) interpolated with the old behaviour */
    for (int i = 0; i < 3; i++) {
      newRgb[i] =  k * (rgb[i] / kPrefilterWhite);
    }
  }
}

RenderBuffer::RenderBuffer(const Genome& genome, size_t width, size_t height)
  : genome_(genome),
    width_(width),
    height_(height),
    scale_(pow(2, genome_.zoom())),
    ppux_(genome_.pixels_per_unit() * scale_),
    ppuy_(genome_.pixels_per_unit() * scale_),
    samples_(0),
    accum_(new Float[width * height * 4]) {
  memset(accum_.get(), 0, width * height * 4 * sizeof(Float));
}

RenderBuffer::~RenderBuffer() { }


void RenderBuffer::Update(size_t x, size_t y,
    const Color& color, Float opacity) {
  if (x >= width_ || y >= height_) {
    return;
  }

  size_t offset = (x + width_ * y) * 4;
  accum_[offset] += color[0];
  accum_[offset + 1] += color[1];
  accum_[offset + 2] += color[2];
  accum_[offset + 3] += opacity;
  ++samples_;
}

RenderState::RenderState(const Genome& genome, RenderBuffer* buffer)
  : genome_(genome),
    buffer_(buffer),
    scale_(pow(2, genome_.zoom())),
    ppux_(genome_.pixels_per_unit() * scale_),
    ppuy_(genome_.pixels_per_unit() * scale_),

    // genome_height_(buffer->height())
    // genome_width_(buffer->width_())
    genome_height_(genome_.size()[1]),
    genome_width_(genome_height_ * (buffer->width() * 1.0 / buffer->height())),

    view_left_(genome_.center()[0] - genome_width_ / ppux_ / 2.0),
    view_bottom_(genome_.center()[1] - genome_height_ / ppuy_ / 2.0),
    view_height_(genome_height_ / ppuy_),
    view_width_(genome_width_ / ppux_),
    xform_distrib_(new int[genome_.xforms().size() * kChooseXformGrain]),
    last_xform_(0) {
  size_t xforms_size = genome_.xforms().size();
//  for (size_t i = 0; i < xforms_size * kChooseXformGrain; ++i) {
//    xform_distrib_[i] = 0;
//  }


  if (xforms_size > 0) {
    // setup xform_distrib_
    CreateXformDist(-1, 0);
    chaos_enabled_ = genome_.is_chaos_enabled();
    if (chaos_enabled_) {
      chaos_enabled_ = true;
      for (size_t i = 0; i < xforms_size; ++i) {
        CreateXformDist(i, i);
      }
    }
  }

  Reseed();
}

void RenderState::CreateXformDist(int xi, int k) {
  size_t xforms_size = genome_.xforms().size();

  Float weight_sum = 0;
  for (size_t i = 0; i < xforms_size; ++i) {
    Float d =  genome_.xforms()[i].weight();
    if (xi > 0) {
      // d *= genome_.chaos(xi, i);
    }
    if (d < 0) {
      BOOST_THROW_EXCEPTION(error());
    }
    weight_sum += d;
  }

  if (weight_sum == 0) {
    BOOST_THROW_EXCEPTION(error());
  }

  Float step = weight_sum / kChooseXformGrain;
  Float t = genome_.xforms()[0].weight();
  if (xi > 0) {
    // d *= genome_.chaos(xi, 0);
  }

  Float r = 0;
  size_t j = 0;
  for (size_t i = 0; i < kChooseXformGrain; ++i) {
    while (r >= t) {
      j++;

      if (xi >= 0) {
        t += genome_.xforms()[j].weight() /* * genome_.chaos(xi, j) */;
      } else {
        t += genome_.xforms()[j].weight();
      }
    }

    xform_distrib_[k * kChooseXformGrain + i] = j;
    r += step;
  }

}

RenderState::~RenderState() { }

void RenderState::Reseed() {
  xyc_[0] = rnd.crnd();
  xyc_[1] = rnd.crnd();
  xyc_[2] = rnd.crnd();
}

void RenderState::Iterate(int iterations) {
  if (genome_.xforms().empty()) {
    return;
  }

  int consequent_errors_ = 0;
  array<Float, 3> xyc2;

  Float rotate1 = 0;
  Float rotate2 = 0;

  if (genome_.rotate() != 0) {
    rotate1 = cos(genome_.rotate() * 2 * kPI / 360.0);
    rotate2 = sin(genome_.rotate() * 2 * kPI / 360.0);
  }

  for (int i = -20; i < iterations; ++i) {
    const Xform& xform = PickRandomXform();
    if (!xform.Apply(xyc_.c_array(), xyc_.c_array(), &rnd)) {
      std::cout << "Apply resulted in error\n";
      ++consequent_errors_;
      if (consequent_errors_ < 5) {
        i -= 4;
        continue;
      }
    }

    consequent_errors_ = 0;

    if (i <= 0) {
      continue;
    }

    Float opacity = xform.opacity();

    if (opacity != 1.0) {
      opacity = AdjustPercentage(opacity);
    }

    if (genome_.has_final_xform()) {
      genome_.final_xform().Apply(xyc_.c_array(), xyc2.c_array(), &rnd);
    } else {
      xyc2[0] = xyc_[0];
      xyc2[1] = xyc_[1];
      xyc2[2] = xyc_[2];
    }

    if (genome_.rotate() != 0) {
      //todo: optimize
      Float x1 = xyc2[0] - genome_.center()[0];
      Float y1 = xyc2[1] - genome_.center()[1];
      Float x = rotate1 * x1 - rotate2 * y1 + genome_.center()[0];
      Float y = rotate2 * x1 + rotate1 * y1 + genome_.center()[1];
      xyc2[0] = x;
      xyc2[1] = y;
    }
    {
      // todo: use round
      int x1 = (int) ((xyc2[0] - view_left_) *
          buffer_->width() / view_width_ + 0.5);
      int y1 = (int) ((xyc2[1] - view_bottom_) *
          buffer_->height() / view_height_ + 0.5);

      // todo: move check into Update.
      if (x1 >= 0 && y1 >= 0) {
        buffer_->Update(
            x1,
            y1,
            genome_.color((int) std::min(std::max(xyc2[2] * Float(255.0),
                  Float(0.0)), Float(255.0))),
            opacity);
      }
    }
  }
}

const Xform& RenderState::PickRandomXform() {
  size_t k;

  boost::random::uniform_int_distribution<> randomGrain(
      0, kChooseXformGrain - 1);

  size_t r = randomGrain(rnd.rng_);

  if (chaos_enabled_) {
    k = xform_distrib_[last_xform_ * kChooseXformGrain + r];
    last_xform_ = k + 1;
  } else {
    k = xform_distrib_[r];
  }

  return genome_.xforms()[k];
}
