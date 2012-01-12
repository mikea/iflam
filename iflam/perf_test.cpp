#include <boost/scoped_array.hpp>

#include "color.h"

int main(int argc, char *argv[]) {
  Random rnd;

  {  // rgb2hsv
    int iterations = 100000000;
    int samples = 100000;
    boost::scoped_array<double> in(new double[samples * 3]);
    for (int i = 0; i < samples * 3; ++i) {
      in[i] = rnd.rnd();
    }

    double out[] = {0, 0, 0};

    {
      Stopwatch sw("rgb2hsv:", iterations);
      for (int i = 0; i < iterations; ++i) {
        rgb2hsv(in.get() + (i % samples) * 3, out);
      }
    }
  }

  {  // hsv2rgb
    int iterations = 100000000;
    int samples = 100000;
    boost::scoped_array<double> in(new double[samples * 3]);
    for (int i = 0; i < samples * 3; ++i) {
      if (i % samples == 0) {
        in[i] = rnd.rnd() * 6;
      } else {
        in[i] = rnd.rnd();
      }
    }

    double out[] = {0, 0, 0};

    {
      Stopwatch sw("hsv2rgb:", iterations);
      for (int i = 0; i < iterations; ++i) {
        hsv2rgb(in.get() + (i % samples) * 3, out);
      }
    }
  }
  return 0;
}


