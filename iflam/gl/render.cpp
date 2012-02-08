#include <math.h>

#define uniform
#define sampler2D void*

class vec3 {
  public:
    vec3(float x1, float x2, float x3) { 
      value[0] = x1;
      value[1] = x2;
      value[2] = x3;
    }

    float& operator[](int idx) { return value[idx]; }
  private:
    float value[3];
};

class RenderShader {
#include "render.fragment"
};
