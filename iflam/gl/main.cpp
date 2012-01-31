#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif

#include "genome.h"
#include "renderer.h"

static GLuint texture_id;

void renderScene(void) {
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
  if(h == 0) { h = 1; }
  glViewport(0, 0, (GLsizei) w, (GLsizei) h);

  glMatrixMode(GL_PROJECTION);
  glLoadIdentity();
  gluOrtho2D(0, 1, 0, 1);
  glMatrixMode(GL_MODELVIEW);
  glLoadIdentity();
}

void init() {
  glClearColor (0.0, 0.0, 0.0, 0.0);
  glShadeModel(GL_FLAT);
  glEnable(GL_DEPTH_TEST);

  glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

  size_t width = 1024;
  size_t height = 768;
  uint8_t* data = new uint8_t[width * height * 4];

  {
    Genome genome;
    genome.Read("/usr/local/google/home/aizatsky/projects/iflam/sheeps/2692.flam3");
    RenderBuffer render_buffer(genome, width, height);
    RenderState state(genome, &render_buffer);
    state.Iterate(1000000);
    RGBA8Image image(data, width * 4, height);
    render_buffer.Render(&image);
  }

  // create texture
  glGenTextures(1, &texture_id);
  glBindTexture(GL_TEXTURE_2D, texture_id);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

  glTexImage2D(
      GL_TEXTURE_2D,  // target
      0,  // level
      GL_RGBA, // internal format
      width,  // width
      height,  // height
      0, // border
      GL_RGBA,  // data format
      GL_UNSIGNED_BYTE,
      data);
}

int main(int argc, char *argv[]) {
  // init GLUT and create Window
  glutInit(&argc, argv);
  glutInitDisplayMode(GLUT_DEPTH | GLUT_DOUBLE | GLUT_RGBA);
  glutInitWindowPosition(100,100);
  glutInitWindowSize(320,320);
  glutCreateWindow("FLAM");

  init();

  // register callbacks
  glutDisplayFunc(renderScene);
  glutReshapeFunc(changeSize);

  // enter GLUT event processing cycle
  glutMainLoop();

  return 1;
}

