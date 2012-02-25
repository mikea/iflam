#include "flam_gl_view.h"

#define CHECK_GL_ERROR() \
  do { \
    GLenum __err = glGetError(); \
    BOOST_VERIFY_MSG(__err == GL_NO_ERROR, (std::string("GL Error: ") + std::string((const char*)gluErrorString(__err))).c_str()); \
  } while (0)


extern const unsigned char render_main_fragment_i[];
extern const unsigned char render_vertex_i[];

FlamGLView::FlamGLView(boost::shared_ptr<FlamComponent> component)
  : component_(component) {
  }

void FlamGLView::Iter() {
  component_->Tick();
  glutSetWindowTitle(component_->controller()->GetWindowTitle().c_str());

  CopyBufferToTexture();
}


void FlamGLView::Init() {
  glClearColor (0.0, 0.0, 0.0, 0.0);
  glShadeModel(GL_FLAT);
  glEnable(GL_DEPTH_TEST);

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
    const GLchar* ptr = (const GLchar*) render_main_fragment_i;
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
}

void FlamGLView::SetSize(int w, int h) {
  if (width_ == w || height_ == h) {
    return;
  }

  width_ = w;
  height_ = h;

  glViewport(0, 0, (GLsizei) w, (GLsizei) h);

  glMatrixMode(GL_PROJECTION);
  glLoadIdentity();
  gluOrtho2D(0, 1, 1, 0);
  glMatrixMode(GL_MODELVIEW);
  glLoadIdentity();

  data_.reset(new float[width_ * height_ * 4]);
  component_->SetSize(width_, height_);
}

void FlamGLView::Render() {
  Iter();

  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

  glEnable(GL_TEXTURE_2D);
  glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_DECAL);
  glBindTexture(GL_TEXTURE_2D, texture_id);
  glBegin(GL_QUADS);
  glTexCoord2f(0.0, 0.0); glVertex3f(0.0, 0.0, 0.0);
  glTexCoord2f(0.0, 1.0); glVertex3f(0.0, 1.0, 0.0);
  glTexCoord2f(1.0, 1.0); glVertex3f(1.0, 1.0, 0.0);
  glTexCoord2f(1.0, 0.0); glVertex3f(1.0, 0.0, 0.0);
  glEnd();
  glFlush();
  glDisable(GL_TEXTURE_2D);
}

void FlamGLView::CopyBufferToTexture() {
  boost::shared_ptr<RenderBuffer> render_buffer(
      component_->render_buffer());
  double scale = render_buffer->max_density() + 1;

  const Genome& genome = *component_->genome();
  {
    const Float* accum = render_buffer->accum();
    for (size_t i = 0; i < height_ * width_ * 4 ; ++i) {
      data_[i] = accum[i] / scale;
    }
  }

  glUniform1f(var_scale, scale);
  glUniform1f(var_k1, render_buffer->k1(genome));
  glUniform1f(var_k2, render_buffer->k2(genome));
  glUniform1f(var_vibrancy, genome.vibrancy());
  glUniform1f(var_gamma, 1.0 / genome.gamma());
  glUniform1f(var_highpow, genome.highlight_power());
  glUniform1f(var_samples, render_buffer->samples());
  //RGBAImage<TT> image(data_, width_ * 4, height_, 1.0, 1.0/255.0);
  //render_buffer->Render(&image);

  glBindTexture(GL_TEXTURE_2D, texture_id);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

  glTexImage2D(
      GL_TEXTURE_2D,  // target
      0,  // level
      GL_RGBA32F, // internal format
      width_,  // width
      height_,  // height
      0, // border
      GL_RGBA,  // data format
      GL_FLOAT,
      data_.get());
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


