#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif
#include <string>
#include <fstream>
#include <streambuf>

#include "genome.h"
#include "renderer.h"

#define CHECK_GL_ERROR() \
  do { \
    GLenum __err = glGetError(); \
    BOOST_VERIFY_MSG(__err == GL_NO_ERROR, (std::string("GL Error: ") + std::string((const char*)gluErrorString(__err))).c_str()); \
  } while (0)


static GLuint texture_id;
static GLuint fragment_shader_id;
static GLuint vertex_shader_id;
static GLuint program_id;

typedef float TT;

class State {
  public:
    State(size_t width, size_t height)
     : width_(width),
       height_(height) {
     genome_ = new Genome();
     genome_->Read("../sheeps/154.flam3");

     render_buffer_ = new RenderBuffer(*genome_, width, height);
     state_ = new RenderState(*genome_, render_buffer_);
     data_ = new TT[width_ * height_ * 4];
    }

    void Iter() {
      state_->Iterate(50000);
      RGBAImage<TT> image(data_, width_ * 4, height_, 1.0, 1.0/255.0);
      render_buffer_->Render(&image);

      glBindTexture(GL_TEXTURE_2D, texture_id);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

      glTexImage2D(
          GL_TEXTURE_2D,  // target
          0,  // level
          GL_RGBA, // internal format
          width_,  // width
          height_,  // height
          0, // border
          GL_RGBA,  // data format
          GL_FLOAT,
          data_);
    }

  private:

  size_t width_;
  size_t height_;
  Genome* genome_;
  RenderBuffer* render_buffer_;
  RenderState* state_;
  TT* data_;
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
  gluOrtho2D(0, 1, 0, 1);
  glMatrixMode(GL_MODELVIEW);
  glLoadIdentity();

  state = new State(w, h);
}

std::string LoadFile(const std::string& file_name) {
  std::ifstream t(file_name.c_str());
  BOOST_VERIFY(t.is_open());
  std::string str(
      (std::istreambuf_iterator<char>(t)),
       std::istreambuf_iterator<char>());
  return str;
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
    std::string vertex = LoadFile("../iflam/gl/blur.vertex");
    std::cout << "Vertex Shader: " << vertex;

    vertex_shader_id = glCreateShader(GL_VERTEX_SHADER);
    const char* ptr = vertex.c_str();
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
    std::string fragment = LoadFile("../iflam/gl/blur.fragment");
    std::cout << "Fragment Shader: " << fragment;

    fragment_shader_id = glCreateShader(GL_FRAGMENT_SHADER);
    const char* ptr = fragment.c_str();
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

  // Check the status of the compile/link
  int logLen;
  glGetProgramiv(program_id, GL_INFO_LOG_LENGTH, &logLen);
  if(logLen > 0) {
    char* log = new char[logLen];
    // Show any errors as appropriate
    glGetProgramInfoLog(program_id, logLen, &logLen, log);
    fprintf(stderr, "Prog Info Log: %s\n", log);
    BOOST_ASSERT(0);
  }
  glUseProgram(program_id);
  CHECK_GL_ERROR();
}

int main(int argc, char *argv[]) {
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

  // enter GLUT event processing cycle
  glutMainLoop();

  return 1;
}

