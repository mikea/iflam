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

#define CHECK_GL_ERROR() \
  do { \
    GLenum __err = glGetError(); \
    BOOST_VERIFY_MSG(__err == GL_NO_ERROR, (std::string("GL Error: ") + std::string((const char*)gluErrorString(__err))).c_str()); \
  } while (0)


static GLuint texture_id;
static GLuint fragment_shader_id;
static GLuint vertex_shader_id;
static GLuint program_id;

static GLuint var_scale;
static GLuint var_k1;
static GLuint var_k2;
static GLuint var_vibrancy;
static GLuint var_gamma;
static GLuint var_highpow;
static GLuint var_samples;


class State {
  public:
    State(boost::shared_ptr<FlamComponent> component, size_t width, size_t height)
      : view_(component) {
      SetSize(width, height);
    }

    void SetSize(size_t width, size_t height) {
      width_ = width;
      height_ = height;
      view_->SetSize(width, height);
      data_.reset(new float[width_ * height_ * 4]);
    }

    void Iter() {
      view_->Tick();
      glutSetWindowTitle(view_->controller()->GetWindowTitle().c_str());

      CopyBufferToTexture();
    }

    boost::shared_ptr<FlamComponent> component() const { return view_; }

  private:

    void CopyBufferToTexture() {
      boost::shared_ptr<RenderBuffer> render_buffer(
                view_->render_buffer());
      double scale = render_buffer->max_density() + 1;

      const Genome& genome = *view_->genome();
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

    boost::shared_ptr<FlamComponent> view_;

    size_t width_;
    size_t height_;
    boost::scoped_array<float> data_;
};

static State* state;

void renderScene(void) {
  state->Iter();

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

  glutSwapBuffers();
}

void changeSize(int w, int h) {
  if (h == 0) { h = 1; }
  glViewport(0, 0, (GLsizei) w, (GLsizei) h);

  glMatrixMode(GL_PROJECTION);
  glLoadIdentity();
  gluOrtho2D(0, 1, 1, 0);
  glMatrixMode(GL_MODELVIEW);
  glLoadIdentity();
  state->SetSize(w, h);
}

void printShaderInfoLog(GLint shader) {
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

extern const unsigned char render_main_fragment_i[];
extern const unsigned char render_vertex_i[];

void glCheckProgramLog() {
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

void init() {
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
      printShaderInfoLog(vertex_shader_id);
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
      printShaderInfoLog(fragment_shader_id);
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

void processNormalKeys(unsigned char key, int /*x*/, int /*y*/) {
  if (key == 27) {
    exit(0);
  }

  if (key == ' ') {
    state->component()->controller()->Next();
  }
}

int main(int argc, char *argv[]) {
  {
    boost::shared_ptr<Controller> slide_show(
        new SlideshowController("../sheeps/"));
    boost::shared_ptr<Controller> animation(
        new AnimatingController(slide_show));
    boost::shared_ptr<FlamComponent> c(new FlamComponent(slide_show));
    state = new State(c, 0, 0);
  }

  // init GLUT and create Window
  glutInit(&argc, argv);
  glutInitDisplayMode(GLUT_DEPTH | GLUT_DOUBLE | GLUT_RGBA);
  glutInitWindowPosition(100,100);
  glutInitWindowSize(1024,768);
  glutCreateWindow("FLAM");

  changeSize(1024, 768);

  init();

  // register callbacks
  glutDisplayFunc(renderScene);
  glutIdleFunc(renderScene);
  glutReshapeFunc(changeSize);
  glutKeyboardFunc(processNormalKeys);

  // enter GLUT event processing cycle
  glutMainLoop();

  return 1;
}

