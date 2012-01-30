#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif

void renderScene(void) {
  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

  glBegin(GL_TRIANGLES);
  {
    glVertex3f(-2,-2,-5.0);
    glVertex3f(2,0.0,-5.0);
    glVertex3f(0.0,2,-5.0);
  }
  glEnd();

  glutSwapBuffers();

}
void changeSize(int w, int h) {
  if(h == 0) {
    h = 1;
  }
  float ratio = 1.0* w / h;

  // Use the Projection Matrix
  glMatrixMode(GL_PROJECTION);

  // Reset Matrix
  glLoadIdentity();

  // Set the viewport to be the entire window
  glViewport(0, 0, w, h);

  // Set the correct perspective.
  gluPerspective(45,ratio,1,1000);

  // Get Back to the Modelview
  glMatrixMode(GL_MODELVIEW);
}

int main(int argc, char *argv[]) {
  // init GLUT and create Window
  glutInit(&argc, argv);
  glutInitDisplayMode(GLUT_DEPTH | GLUT_DOUBLE | GLUT_RGBA);
  glutInitWindowPosition(100,100);
  glutInitWindowSize(320,320);
  glutCreateWindow("Lighthouse3D- GLUT Tutorial");

  // register callbacks
  glutDisplayFunc(renderScene);
  glutReshapeFunc(changeSize);

  // enter GLUT event processing cycle
  glutMainLoop();

  return 1;
}

