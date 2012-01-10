#include "genome.h"
#include "renderer.h"

namespace {

const size_t kVLen = 6;
const size_t kFLen = 6 + kVLen;
const size_t kChooseXformGrain = 16384;
const double kGamma = 2.5;
const double kPrefilterWhite = 255;

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

void rgb2hsv(double* rgb, double* hsv) {
  double rd, gd, bd, h, s, v, max, min, del, rc, gc, bc;

  rd = rgb[0];
  gd = rgb[1];
  bd = rgb[2];

  /* compute maximum of rd,gd,bd */
  if (rd >= gd) {
    if (rd >= bd) max = rd;
    else max = bd;
  } else {
    if (gd >= bd) max = gd;
    else max = bd;
  }

  /* compute minimum of rd,gd,bd */
  if (rd <= gd) {
    if (rd <= bd) min = rd;
    else min = bd;
  } else {
    if (gd <= bd) min = gd;
    else min = bd;
  }

  del = max - min;
  v = max;
  if (max != 0.0) s = (del) / max;
  else s = 0.0;

  h = 0;
  if (s != 0.0) {
    rc = (max - rd) / del;
    gc = (max - gd) / del;
    bc = (max - bd) / del;

    if (rd == max) h = bc - gc;
    else if (gd == max) h = 2 + rc - bc;
    else if (bd == max) h = 4 + gc - rc;

    if (h < 0) h += 6;
  }

  hsv[0] = h;
  hsv[1] = s;
  hsv[2] = v;
}
void hsv2rgb(double* hsv, double* rgb) {
  double h = hsv[0], s = hsv[1], v = hsv[2];
  int j;
  double rd, gd, bd;
  double f, p, q, t;

  while (h >= 6.0) h = h - 6.0;
  while (h < 0.0) h = h + 6.0;
  j = (int) floor(h);
  f = h - j;
  p = v * (1 - s);
  q = v * (1 - (s * f));
  t = v * (1 - (s * (1 - f)));

  switch (j) {
    case 0:
      rd = v;
      gd = t;
      bd = p;
      break;
    case 1:
      rd = q;
      gd = v;
      bd = p;
      break;
    case 2:
      rd = p;
      gd = v;
      bd = t;
      break;
    case 3:
      rd = p;
      gd = q;
      bd = v;
      break;
    case 4:
      rd = t;
      gd = p;
      bd = v;
      break;
    case 5:
      rd = v;
      gd = p;
      bd = q;
      break;
    default:
      rd = v;
      gd = t;
      bd = p;
      break;
  }

  rgb[0] = rd;
  rgb[1] = gd;
  rgb[2] = bd;
}

double CalcAlpha(double density, double gamma, double linearRange, double linRangePowGamma) {
  if (density > 0) {
    if (density < linearRange) {
      double frac = density / linearRange;
      return (1.0 - frac) * density * linRangePowGamma + frac * pow(density, gamma);
    } else {
      return pow(density, gamma);
    }
  } else {
    return 0;
  }
}

void CalcNewRgb(double* rgb, double* newRgb, double ls, double highpow) {
  if (ls == 0.0 || (rgb[0] == 0.0 && rgb[1] == 0.0 && rgb[2] == 0.0)) {
    newRgb[0] = 0.0;
    newRgb[1] = 0.0;
    newRgb[2] = 0.0;
    return;
  }

  /* Identify the most saturated channel */
  double maxA = -1.0;
  double maxC = 0;
  for (int i = 0; i < 3; i++) {
    double a = ls * (rgb[i] / kPrefilterWhite);
    if (a > maxA) {
      maxA = a;
      maxC = rgb[i] / kPrefilterWhite;
    }
  }

  /* If a channel is saturated and we have a non-negative highlight power */
  /* modify the color to prevent hue shift                                */
  if (maxA > 255 && highpow >= 0.0) {
    double newls = 255.0 / maxC;
    /* Calculate the max-value color (ranged 0 - 1) */
    for (int i = 0; i < 3; i++)
      newRgb[i] = newls * (rgb[i] / kPrefilterWhite) / 255.0;

    /* Reduce saturation by the lsratio */
    boost::scoped_array<double> newHsv(new double[3]);
    rgb2hsv(newRgb, newHsv.get());
    newHsv[1] *= pow(newls / ls, highpow);
    hsv2rgb(newHsv.get(), newRgb);

    for (int i = 0; i < 3; i++) {
      newRgb[i] *= 255.0;
    }
  } else {
    double newLs = 255.0 / maxC;
    double adjHlp = -highpow;
    if (adjHlp > 1)
      adjHlp = 1;
    if (maxA <= 255)
      adjHlp = 1.0;

    /* Calculate the max-value color (ranged 0 - 1) interpolated with the old behaviour */
    for (int i = 0; i < 3; i++) {
      newRgb[i] = ((1.0 - adjHlp) * newLs + adjHlp * ls) * (rgb[i] / kPrefilterWhite);
    }
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
  ++samples_;
}

void RenderBuffer::Render(boost::gil::rgb8_view_t* image) {
  typedef boost::gil::rgb8_view_t view_t;
  typedef view_t::reference pixel_t_ref;

  if (image->width() != width_ || image->height() != height_) {
    BOOST_THROW_EXCEPTION(error());
  }

  int vib_gam_n = 1;
  double vibrancy = genome_.vibrancy();
  vibrancy /= vib_gam_n;
  double linrange = genome_.gamma_threshold();
  double gamma = 1.0 / (genome_.gamma() / vib_gam_n);
  double highpow = genome_.highlight_power();

  int nbatches = 1; // genome_.nbatches();
  double oversample = 1.0; // genome.oversample
  // double sample_density = genome.quality * scale * scale;
  // double nsamples = sample_density * width * height;

  double sample_density = ((double) (samples_)) / (width_ * height_);
  double batch_filter = 1 / nbatches;

  double k1 = (genome_.contrast() * genome_.brightness() * kPrefilterWhite *
      268.0 * batch_filter) / 256;
  double area = width_ * height_ / (ppux_ * ppuy_);
  double sumfilt = 1;
  double k2 = (oversample * oversample * nbatches) /
    (genome_.contrast() * area * /* WHITE_LEVEL * */ sample_density * sumfilt);
  double linRangePowGamma = pow(linrange, gamma) / linrange;

  double newrgb[4] = {0, 0, 0, 0};

  for (int y = 0; y < height_; ++y) {
    for (int x = 0; x < width_; ++x) {
      int offset = (x + width_ * y) * 4;
      double cr = accum_[offset];
      double cg = accum_[offset + 1];
      double cb = accum_[offset + 2];
      double freq = accum_[offset + 3];

      if (freq != 0) {
        double ls = (k1 * log(1.0 + freq * k2)) / freq;
        freq *= ls;
        cr *= ls;
        cg *= ls;
        cb *= ls;
      }

      double alpha, ls;

      if (freq <= 0) {
        alpha = 0.0;
        ls = 0.0;
      } else {
        double tmp = freq / kPrefilterWhite;
        alpha = CalcAlpha(tmp, gamma, linrange, linRangePowGamma);
        ls = vibrancy * 256.0 * alpha / tmp;
        if (alpha < 0.0) alpha = 0.0;
        if (alpha > 1.0) alpha = 1.0;
      }

      double t[4] = {cr, cg, cb, freq};
      CalcNewRgb(t, newrgb, ls, highpow);

      for (int rgbi = 0; rgbi < 3; rgbi++) {
        double a = newrgb[rgbi];
        a += (1.0 - vibrancy) * 256.0 * pow(t[rgbi] / kPrefilterWhite, gamma);

        a += ((1.0 - alpha) * genome_.background()[rgbi]);


        if (a > 255) a = 255;
        if (a < 0) a = 0;
        t[rgbi] = a;
      }


      t[3] = alpha;
      pixel_t_ref pixel = (*image)(x, y);
      pixel[0] = int(t[0]);
      pixel[1] = int(t[1]);
      pixel[2] = int(t[2]);
    }
  }
}


RenderState::RenderState(const Genome& genome, RenderBuffer* buffer)
  : genome_(genome),
    buffer_(buffer),
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
//  for (size_t i = 0; i < xforms_size * kChooseXformGrain; ++i) {
//    xform_distrib_[i] = 0;
//  }

  // setup xform_distrib_
  CreateXformDist(-1, 0);
  chaos_enabled_ = genome_.is_chaos_enabled();
  if (chaos_enabled_) {
    chaos_enabled_ = true;
    for (size_t i = 0; i < xforms_size; ++i) {
      CreateXformDist(i, i);
    }
  }

  Reseed();
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
    }

    xform_distrib_[k * kChooseXformGrain + i] = j;
    r += step;
  }

}

RenderState::~RenderState() { }

void RenderState::Reseed() {
  xyc_[0] = Random::crnd();
  xyc_[1] = Random::crnd();
  xyc_[2] = Random::crnd();
}

void RenderState::Iterate() {
  int consequent_errors_ = 0;
  array<double, 3> xyc2;

  for (int i = -20; i < 1000000; ++i) {
    const Xform& xform = PickRandomXform();
    if (!xform.Apply(xyc_.c_array(), xyc_.c_array())) {
      std::cout << "Apply resulted in error\n";
      ++consequent_errors_;
      if (consequent_errors_ < 5) {
        continue;
      }
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
      genome_.final_xform().Apply(xyc_.c_array(), xyc2.c_array());
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

  size_t r = randomGrain(Random::rng_);

  if (chaos_enabled_) {
    k = xform_distrib_[last_xform_ * kChooseXformGrain + r];
    last_xform_ = k + 1;
  } else {
    k = xform_distrib_[r];
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
