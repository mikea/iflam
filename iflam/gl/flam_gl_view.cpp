#include "flam_gl_view.h"
#include <iostream>

#define CHECK_GL_ERROR() \
  do { \
    GLenum __err = glGetError(); \
    BOOST_VERIFY_MSG(__err == GL_NO_ERROR, (std::string("GL Error: ") + \
          boost::lexical_cast<std::string>(__err)).c_str()); \
  } while (0)


/////////////////

extern const unsigned char render_vertex_i[];

#ifdef FLAM_ES
  extern const unsigned char render_main_fragment_es_i[];
#else
  extern const unsigned char render_main_fragment_i[];
#endif

const GLuint kAttrPosition = 0;
const GLuint kAttrTexCoord = 1;

FlamGLView::FlamGLView(boost::shared_ptr<FlamComponent> component)
  : component_(component) {
  }

void FlamGLView::Iter() {
  component_->Tick();

  CopyBufferToTexture();
}


void FlamGLView::Init() {
  glClearColor (0.0, 0.0, 0.0, 0.0);

  glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

  // create texture
  glGenTextures(1, &texture_id);

  // setup shaders
  {
    GLint compiled;
    vertex_shader_id = glCreateShader(GL_VERTEX_SHADER);
    const GLchar* ptr = (const GLchar*) render_vertex_i;
    glShaderSource(vertex_shader_id, 1, &ptr, NULL);
    glCompileShader(vertex_shader_id);
    glGetShaderiv(vertex_shader_id, GL_COMPILE_STATUS, &compiled);
    if(!compiled) {
      PrintShaderInfoLog(vertex_shader_id);
      BOOST_ASSERT(0);
    }
    CHECK_GL_ERROR();
  }
  {
    GLint compiled;
    fragment_shader_id = glCreateShader(GL_FRAGMENT_SHADER);

    #ifdef FLAM_ES
      const GLchar* ptr = (const GLchar*) render_main_fragment_es_i;
    #else
      const GLchar* ptr = (const GLchar*) render_main_fragment_i;
    #endif

    glShaderSource(fragment_shader_id, 1, &ptr, NULL);
    glCompileShader(fragment_shader_id);

    glGetShaderiv(fragment_shader_id, GL_COMPILE_STATUS, &compiled);
    if(!compiled) {
      PrintShaderInfoLog(fragment_shader_id);
      BOOST_ASSERT(0);
    }
    CHECK_GL_ERROR();
  }

  program_id = glCreateProgram();
  CHECK_GL_ERROR();
  glAttachShader(program_id, vertex_shader_id);
  CHECK_GL_ERROR();
  glAttachShader(program_id, fragment_shader_id);
  CHECK_GL_ERROR();
  glLinkProgram(program_id);
  CHECK_GL_ERROR();

  // Validate program
  glValidateProgram(program_id);
  CHECK_GL_ERROR();

  glUseProgram(program_id);
  CHECK_GL_ERROR();

  var_scale = glGetUniformLocation(program_id, "scale");
  CHECK_GL_ERROR();

  var_k1 = glGetUniformLocation(program_id, "k1");
  CHECK_GL_ERROR();

  var_k2 = glGetUniformLocation(program_id, "k2");
  CHECK_GL_ERROR();

  var_vibrancy = glGetUniformLocation(program_id, "vibrancy");
  CHECK_GL_ERROR();

  var_gamma = glGetUniformLocation(program_id, "gamma");
  CHECK_GL_ERROR();

  var_highpow = glGetUniformLocation(program_id, "highpow");
  CHECK_GL_ERROR();

  var_samples = glGetUniformLocation(program_id, "samples");
  CHECK_GL_ERROR();

  var_tex = glGetUniformLocation(program_id, "tex");
  CHECK_GL_ERROR();
  glUniform1i(var_tex, 0);
  CHECK_GL_ERROR();

  glBindAttribLocation(program_id, kAttrPosition, "a_position");
  CHECK_GL_ERROR();

  glBindAttribLocation(program_id, kAttrTexCoord, "a_texCoord");
  CHECK_GL_ERROR();
}

void FlamGLView::SetSize(int w, int h) {
  if (width_ == w || height_ == h) {
    return;
  }

  width_ = w;
  height_ = h;

  glViewport(0, 0, (GLsizei) w, (GLsizei) h);

  data_.reset(new GLfloat[width_ * height_ * 4]);
  component_->SetSize(width_, height_);
  CHECK_GL_ERROR();
}

void FlamGLView::Render() {
  CHECK_GL_ERROR();
  Iter();

  CHECK_GL_ERROR();
  glClear(GL_COLOR_BUFFER_BIT);
  CHECK_GL_ERROR();

  // glEnable(GL_TEXTURE_2D);
  // CHECK_GL_ERROR();
  //glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_DECAL);
  glBindTexture(GL_TEXTURE_2D, texture_id);
  CHECK_GL_ERROR();

  {
    GLfloat vCoords[] = {
      -1.0, -1.0, 0.0,
      -1.0,  1.0, 0.0,
      1.0,  1.0, 0.0,
      1.0, -1.0, 0.0
    };
    glVertexAttribPointer(kAttrPosition,
        3 /* number of components */,
        GL_FLOAT,
        GL_FALSE /* normalize */,
        0,
        vCoords);
    glEnableVertexAttribArray(kAttrPosition);
  }

  {
    GLfloat texCoords[] = {
      0.0, 0.0,
      0.0, 1.0,
      1.0, 1.0,
      1.0, 0.0
    };

    glVertexAttribPointer(kAttrTexCoord,
        2 /* number of components */,
        GL_FLOAT,
        GL_FALSE /* normalize */,
        0,
        texCoords);
    glEnableVertexAttribArray(kAttrTexCoord);
  }

  glDrawArrays(
      GL_TRIANGLE_FAN,
      0 /* first */,
      4 /* count */);

  glFlush();
  //glDisable(GL_TEXTURE_2D);
  CHECK_GL_ERROR();
}

void FlamGLView::CopyBufferToTexture() {
  boost::shared_ptr<RenderBuffer> render_buffer(
      component_->render_buffer());
  double scale = render_buffer->max_density() + 1;

  const Genome& genome = *component_->genome();
  {
    /*
    const Float* accum = render_buffer->accum();
    for (size_t i = 0; i < height_ * width_ * 4 ; ++i) {
      data_[i] = accum[i] / scale;
    }*/
    scale = 1;
    for (size_t x = 0; x < width_; ++x) {
      for (size_t y = 0; y < height_; ++y) {
        size_t idx = (x + y * width_) * 4;
        data_[idx] = 0;
        data_[idx + 1] = 0;
        data_[idx + 2] = 0;
        data_[idx + 3] = 1.0 * x / width_;
      }
    }
  }

  glUniform1f(var_scale, scale);
  glUniform1f(var_k1, render_buffer->k1(genome));
  glUniform1f(var_k2, render_buffer->k2(genome));
  glUniform1f(var_vibrancy, genome.vibrancy());
  glUniform1f(var_gamma, 1.0 / genome.gamma());
  glUniform1f(var_highpow, genome.highlight_power());
  glUniform1f(var_samples, render_buffer->samples());
  CHECK_GL_ERROR();

  glActiveTexture(GL_TEXTURE0);
  glBindTexture(GL_TEXTURE_2D, texture_id);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
  CHECK_GL_ERROR();

  glTexImage2D(
      GL_TEXTURE_2D,  // target
      0,        // level
      GL_RGBA,  // internal format
      width_,   // width
      height_,  // height
      0,        // border
      GL_RGBA,  // data format
      GL_FLOAT,
      data_.get());

  CHECK_GL_ERROR();
}

void FlamGLView::PrintShaderInfoLog(GLint shader) {
  int infoLogLen = 0;
  int charsWritten = 0;
  GLchar *infoLog;

  glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLogLen);

  if (infoLogLen > 0)
  {
    infoLog = new GLchar[infoLogLen];
    glGetShaderInfoLog(shader, infoLogLen, &charsWritten, infoLog);
    std::cout << infoLog << std::endl;
    delete [] infoLog;
  }
}

void FlamGLView::GLCheckProgramLog() {
  int logLen = 0;
  glGetProgramiv(program_id, GL_INFO_LOG_LENGTH, &logLen);
  if(logLen > 0) {
    std::cout << logLen;
    char* log = new char[logLen];
    // Show any errors as appropriate
    glGetProgramInfoLog(program_id, logLen, &logLen, log);
    BOOST_ASSERT_MSG(0, log);
  }
}
