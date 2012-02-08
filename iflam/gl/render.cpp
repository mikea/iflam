#include <math.h>

#define uniform
#define sampler2D void*

class vec3 {
  public:
    vec3(float x1, float x2, float x3) { }

    float& operator[](int idx) { return value[idx]; }
  private:
    float* value;
};

class Render {
#include "render.fragment"
};

