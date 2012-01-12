#include "color.h"

// http://en.wikipedia.org/wiki/HSL_and_HSV
void rgb2hsv(Float* rgb, Float* hsv) {
  Float r = rgb[0];
  Float g = rgb[1];
  Float b = rgb[2];

  Float max = std::max(r, std::max(g, b));

  if (max == 0.0) {
    hsv[0] = 0;
    hsv[1] = 0;
    hsv[2] = 0;
    return;
  }

  Float min = std::min(r, std::min(g, b));
  Float c = max - min;
  Float h = 0;

  if (c != 0.0) {
    if (r == max) {
      h = (g - b) / c;
    }
    else if (g == max) {
      h = 2 + (b - r) / c;
    }
    else /* if (b == max) */ {
      h = 4 + (r - g) / c;
    }

    if (h < 0) h += 6;
  }

  hsv[0] = h;
  hsv[1] = c / max;  // s
  hsv[2] = max;      // v
}

void hsv2rgb(Float* hsv, Float* rgb) {
  Float h = hsv[0];
  Float s = hsv[1];
  Float v = hsv[2];

  while (h >= 6.0) h = h - 6.0;
  while (h < 0.0) h = h + 6.0;
  int j = (int) floor(h);
  Float f = h - j;
  Float p = v * (1 - s);
  Float q = v * (1 - (s * f));
  Float t = v * (1 - (s * (1 - f)));

  Float rd, gd, bd;
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

