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
const double kPI = boost::math::constants::pi<double>();
const double kGamma = 2.5;

void assertSane(double dx) {
    assert(!boost::math::isnan(dx));
    assert(dx < 1e20 && dx > -1e20);
    assert(dx > 1e-20 || dx < -1e-20);
}

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
