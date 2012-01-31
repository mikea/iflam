#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif

#include "genome.h"
#include "renderer.h"

static GLuint texture_id;

class State {
  public:
    State(size_t width, size_t height)
     : width_(width),
       height_(height) {
     genome_ = new Genome();
     genome_->Read("../sheeps/154.flam3");

     render_buffer_ = new RenderBuffer(*genome_, width, height);
     state_ = new RenderState(*genome_, render_buffer_);
     data_ = new uint8_t[width_ * height_ * 4];
    }

    void Iter() {
      state_->Iterate(50000);
      RGBA8Image image(data_, width_ * 4, height_);
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
          GL_UNSIGNED_BYTE,
          data_);
    }

  private:

  size_t width_;
  size_t height_;
  Genome* genome_;
  RenderBuffer* render_buffer_;
  RenderState* state_;
  uint8_t* data_;
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

void init() {

  glClearColor (0.0, 0.0, 0.0, 0.0);
  glShadeModel(GL_FLAT);
  glEnable(GL_DEPTH_TEST);

  glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

  // create texture
  glGenTextures(1, &texture_id);
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

