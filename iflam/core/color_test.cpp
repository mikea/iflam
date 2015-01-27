#define BOOST_TEST_MODULE renderer test

#include <boost/test/unit_test.hpp>

#include "common.h"
#include "color.h"
#include "renderer.h"

const Float kPrecision = 1e-4;

void TestRgb2Hsv(double r, double g, double b,
        double h, double s, double v) {
    Float in[] = {r, g, b};
    Float out[] = {-1, -1, -1};
    rgb2hsv(in, out);

    BOOST_CHECK_MESSAGE(
    fabs(out[0] - h) < kPrecision &&
                    fabs(out[1] - s) < kPrecision &&
                    fabs(out[2] - v) < kPrecision,
    "Error transforming (" << r << ", " << g << ", " << b << ")."
            << " Expected: (" << h << ", " << s << ", " << v << ")"
            << " but got: (" << out[0] << ", " << out[1] << ", " << out[2] << ")"
    );
}

void TestHsv2Rgb(double h, double s, double v,
        double r, double g, double b) {
    Float in[] = {h, s, v};
    Float out[] = {-1, -1, -1};
    hsv2rgb(in, out);

    BOOST_CHECK_MESSAGE(
    fabs(out[0] - r) < kPrecision &&
                    fabs(out[1] - g) < kPrecision &&
                    fabs(out[2] - b) < kPrecision,
    "Error transforming (" << h << ", " << s << ", " << v << ")."
            << " Expected: (" << r << ", " << g << ", " << b << ")"
            << " but got: (" << out[0] << ", " << out[1] << ", " << out[2] << ")"
    );
}

BOOST_AUTO_TEST_CASE(rgb2hsv_test){
    TestRgb2Hsv(0, 0, 0, 0, 0, 0);
    TestRgb2Hsv(.495, .493, .721, 4.00877, .316227, .721);
    TestRgb2Hsv(0.211, 0.149, 0.597, 4.13839, 0.750419, 0.597);
    TestRgb2Hsv(0.931, 0.463, 0.316, .239024, 0.66058, 0.931);
}

BOOST_AUTO_TEST_CASE(hsv2rgb_test){
    TestHsv2Rgb(0, 0, 0, 0, 0, 0);
    TestHsv2Rgb(4.00877, .316227, .721, .495, .493, .721);
    TestHsv2Rgb(4.13839, 0.750419, 0.597, 0.211, 0.149, 0.597);
    TestHsv2Rgb(.239024, 0.66058, 0.931, 0.931, 0.463, 0.316);
}

BOOST_AUTO_TEST_CASE(rgb2hsv_random_test) {
    Random rnd;

    for (int i = 0; i < 10000; ++i) {
        double in[] = {rnd.rnd(), rnd.rnd(), rnd.rnd()};
        double out1[] = {0, 0, 0};
        double out2[] = {0, 0, 0};
        rgb2hsv(in, out1);
        hsv2rgb(out1, out2);

        BOOST_CHECK_MESSAGE(
        fabs(out2[0] - in[0]) < kPrecision &&
                        fabs(out2[1] - in[1]) < kPrecision &&
                        fabs(out2[2] - in[2]) < kPrecision,
        "Error transforming (" << in[0] << ", " << in[1] << ", " << in[2] << ")."
                << " got: (" << out2[0] << ", " << out2[1] << ", " << out2[2] << ")"
        );
    }
}

