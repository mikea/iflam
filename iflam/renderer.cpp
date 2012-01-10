#include "genome.h"
#include "renderer.h"
#include <boost/random/mersenne_twister.hpp>
#include <boost/random/uniform_real_distribution.hpp>
#include <boost/random/uniform_int_distribution.hpp>
#include <boost/math/special_functions/fpclassify.hpp>
#include <boost/math/constants/constants.hpp>

namespace {
boost::random::mt19937 rng(std::time(0));
boost::random::uniform_real_distribution<> rndDist(0, 1);
boost::random::uniform_real_distribution<> crndDist(-1, 1);

const size_t kVLen = 6;
const size_t kFLen = 6 + kVLen;
const size_t kChooseXformGrain = 16384;
const double kPI = boost::math::constants::pi<double>();
const double kGamma = 2.5;

void assertSane(double dx) {
    assert(!boost::math::isnan(dx));
    assert(dx < 1e20 && dx > -1e20);
    assert(dx > 1e-20 || dx < -1e-20);
}

double AdjustPercentage(double p) {
  if (p == 0) {
    return p;
  } else {
    return pow(10, -log(1.0/p)) / log(2);
  }
}

}

RenderBuffer::RenderBuffer(size_t width, size_t height)
  : width_(width),
    height_(height),
    accum_(new double[width * height * 4]) { }

RenderBuffer::~RenderBuffer() { }


void RenderBuffer::Update(int x, int y,
    const Color& color, double opacity) {
  if (x < 0 || x >= width_ || y < 0 || y >= height_) {
    return;
  }

  int offset = (x + width_ * y) * 4;
  accum_[offset] += color[0];
  accum_[offset + 1] += color[1];
  accum_[offset + 2] += color[2];
  accum_[offset + 3] += opacity;
}


RenderState::RenderState(const Genome& genome, RenderBuffer* buffer)
  : genome_(genome),
    buffer_(buffer),
    xyc_(new double[3]),
    scale_(pow(2, genome_.zoom())),
    ppux_(genome_.pixels_per_unit() * scale_),
    ppuy_(genome_.pixels_per_unit() * scale_),
    view_left_(genome_.center()[0] - buffer->width() / ppux_ / 2.0),
    view_bottom_(genome_.center()[1] - buffer->height() / ppuy_ / 2.0),
    view_height_(buffer->height() / ppuy_),
    view_width_(buffer->width() / ppux_),
    xform_distrib_(new int[genome_.xforms().size() * kChooseXformGrain]),
    last_xform_(0) {
  size_t xforms_size = genome_.xforms().size();
  // setup xform_distrib_
  for (size_t i = 0; i < xforms_size * kChooseXformGrain; ++i) {
    xform_distrib_[i] = 1;
  }

  CreateXformDist(-1, 0);
  chaos_enabled_ = genome_.is_chaos_enabled();
  if (chaos_enabled_) {
    chaos_enabled_ = true;
    for (size_t i = 0; i < xforms_size; ++i) {
      CreateXformDist(i, i);
    }
  }
}

void RenderState::CreateXformDist(int xi, int k) {
  size_t xforms_size = genome_.xforms().size();

  double weight_sum = 0;
  for (size_t i = 0; i < xforms_size; ++i) {
    double d =  genome_.xforms()[i].weight();
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

  double step = weight_sum / kChooseXformGrain;
  double t = genome_.xforms()[0].weight();
  if (xi > 0) {
    // d *= genome_.chaos(xi, 0);
  }

  double r = 0;
  size_t j = 0;
  for (size_t i = 0; i < kChooseXformGrain; ++i) {
    while (r >= t) {
      j++;

      if (xi >= 0) {
        t += genome_.xforms()[j].weight() /* * genome_.chaos(xi, j) */;
      } else {
        t += genome_.xforms()[j].weight();
      }

      xform_distrib_[k * kChooseXformGrain + i] = j;
      r += step;
    }
  }

}

RenderState::~RenderState() { }

void RenderState::Reseed() {
  xyc_[0] = crndDist(rng);
  xyc_[1] = crndDist(rng);
  xyc_[2] = crndDist(rng);
}

void RenderState::Iterate() {
  int consequent_errors_ = 0;
  boost::scoped_array<double> xyc2(new double[3]);

  for (int i = -20; i < 1000000; ++i) {
    const Xform& xform = PickRandomXform();
    if (!xform.Apply(xyc_.get(), xyc_.get())) {
      ++consequent_errors_;
    }

    if (consequent_errors_ < 5) {
      continue;
    }

    consequent_errors_ = 0;

    if (i <= 0) {
      continue;
    }

    double opacity = xform.opacity();

    if (opacity != 1.0) {
      opacity = AdjustPercentage(opacity);
    }

    if (genome_.has_final_xform()) {
      genome_.final_xform().Apply(xyc_.get(), xyc2.get());
    } else {
      xyc2[0] = xyc_[0];
      xyc2[1] = xyc_[1];
      xyc2[2] = xyc_[2];
    }

    //TODO: rotate
    {
      // todo: use round
      int x1 = (int) ((xyc2[0] - view_left_) *
          buffer_->width() / view_width_ + 0.5);
      int y1 = (int) ((xyc2[1] - view_bottom_) *
          buffer_->height() / view_height_ + 0.5);

      buffer_->Update(
          x1,
          y1,
          genome_.color((int) std::min(std::max(xyc2[2] * 255.0, 0.0), 255.0)),
          opacity);
    }
  }
}

const Xform& RenderState::PickRandomXform() {
  size_t k;

  boost::random::uniform_int_distribution<> randomGrain(
      0, kChooseXformGrain - 1);

  if (chaos_enabled_) {
    k = xform_distrib_[last_xform_ * kChooseXformGrain +
      randomGrain(rng)];
    last_xform_ = k + 1;
  } else {
    k = xform_distrib_[randomGrain(rng)];
  }

  return genome_.xforms()[k];
}

void FlamRender::UpdateHistogram(const RenderState& s) {
/*    if (s.x < 0 || s.x > 1 || s.y < 0 || s.y > 1) {
        return;
    }
    double x1 = s.x * width_;
    double y1 = s.y * height_;

    int offset = (int) x1 + height_ * (int) y1;

    if (offset > width_ * height_ || offset < 0) {
        return;
    }

    ++histogram_[offset];
    color_histogram_[offset * 3] = (color_histogram_[offset * 3] + s.r) / 2;
    color_histogram_[offset * 3 + 1] = (color_histogram_[offset * 3 + 1] + s.g) / 2;
    color_histogram_[offset * 3 + 2] = (color_histogram_[offset * 3 + 2] + s.b) / 2;*/
}

void FlamRender::Render(const Genome& definition) {
 /*   Lock();
    assert(definition.number_of_functions_ * kFLen == definition.coeffs_.size());
    boost::random::uniform_int_distribution<> fnDist(0, definition.number_of_functions_ - 1);
    Unlock();

    RenderState s;
    s.x = crndDist(rng);
    s.y = crndDist(rng);
    s.r = 0;
    s.g = 0;
    s.b = 0;

    for (int i = 0; i < 500000; ++i) {
        int k = fnDist(rng);
        assert(k < definition.number_of_functions_);

        int idx = k * kFLen;
        double a = definition.coeffs_[idx];
        double b = definition.coeffs_[idx + 1];
        double c = definition.coeffs_[idx + 2];
        double d = definition.coeffs_[idx + 3];
        double e = definition.coeffs_[idx + 4];
        double f = definition.coeffs_[idx + 5];

        assertSane(a);
        assertSane(b);
        assertSane(c);
        assertSane(d);
        assertSane(e);
        assertSane(f);

        {   // Affine transform
            double x1, y1;
            x1 = s.x * a + s.y * b + c;
            y1 = s.x * d + s.y * e + f;

            assertSane(x1);
            assertSane(y1);
            s.x = x1;
            s.y = y1;
        }

        {   // Nonlinear transform
            double x2 = 0, y2 = 0;

            double r2 = s.x * s.x + s.y * s.y;
            double r = sqrt(r2);
            double theta = atan(s.x / s.y);

            for (int j = 0; j < kVLen; ++j) {
                double dx;
                double dy;

                switch (j) {
                    default:
                        assert(0);
                    case 0:
                        dx = s.x;
                        dy = s.y;
                        break;
                    case 1:
                        dx = sin(s.x);
                        dy = sin(s.y);
                        break;
                    case 2:
                        dx = s.x / r2;
                        dy = s.y / r2;
                        break;
                    case 3:
                        dx = s.x * sin(r2) - s.y * cos(r2);
                        dy = s.x * cos(r2) + s.y * sin(r2);
                        break;
                    case 4:
                        dx = (s.x - s.y) * (s.x + s.y) / r;
                        dy = 2 * s.x * s.y / r;
                        break;
                    case 5:
                        dx = theta / kPI;
                        dy = r - 1;
                        break;
                    case 6:
                        dx = r * sin(theta + r);
                        dy = r * cos(theta - r);
                        break;
                }

                assertSane(dx);
                assertSane(dy);

                x2 += definition.coeffs_[idx + 6 + j] * dx;
                y2 += definition.coeffs_[idx + 6 + j] * dy;
            }

            assertSane(x2);
            assertSane(y2);

            s.x = x2;
            s.y = y2;
        }

        s.r = (s.r + definition.colors_[k * 3]) / 2;
        s.g = (s.g + definition.colors_[k * 3 + 1]) / 2;
        s.b = (s.b + definition.colors_[k * 3 + 2]) / 2;

        if (i > 20) {
            UpdateHistogram(s);
        }
    }*/
}

void FlamRender::Visualize(PixelInterface *pixelInterface) {
 /*   int maxFreq = 0;
    for (int i = 0; i < width_ * height_; ++i) {
        if (maxFreq < histogram_[i]) {
            maxFreq = histogram_[i];
        }
    }

    double maxFreqLog = log(maxFreq);
    for (int x = 0; x < width_; ++x) {
        for (int y = 0; y < height_; ++y) {
            int offset = x + height_ * y;
            double alpha = log(histogram_[offset]) / maxFreqLog;
            alpha = pow(alpha, 1 / kGamma);
            double r = color_histogram_[offset * 3];
            double g = color_histogram_[offset * 3 + 1];
            double b = color_histogram_[offset * 3 + 2];

            pixelInterface->SetPixel(x, y, r * alpha, g * alpha, b * alpha);
        }
    }*/
}
