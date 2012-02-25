#ifndef __FLAM_GL_VIEW__
#define __FLAM_GL_VIEW__


// TODO: optimize includes
#ifdef __APPLE__
#include <GLUT/glut.h>
#include <GL/glext.h>
#define GL_RGBA32F  0x8814
#else
#include <GL/glut.h>
#endif
#include <string>
#include <fstream>
#include <streambuf>

#include "genome.h"
#include "renderer.h"
#include "controller.h"
#include "component.h"


class FlamGLView {
  public:
    FlamGLView(boost::shared_ptr<FlamComponent> component);
    boost::shared_ptr<FlamComponent> component() const { return component_; }
    void Init();
    void SetSize(int w, int h);
    void Render();

  private:
    void Iter();
    void CopyBufferToTexture();
    void PrintShaderInfoLog(GLint shader);
    void GLCheckProgramLog();


    boost::shared_ptr<FlamComponent> component_;

    size_t width_;
    size_t height_;
    boost::scoped_array<float> data_;

    GLuint texture_id;
    GLuint fragment_shader_id;
    GLuint vertex_shader_id;
    GLuint program_id;

    GLuint var_scale;
    GLuint var_k1;
    GLuint var_k2;
    GLuint var_vibrancy;
    GLuint var_gamma;
    GLuint var_highpow;
    GLuint var_samples;

};


#endif

