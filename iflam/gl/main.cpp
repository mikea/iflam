#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif
#include <string>
#include <fstream>
#include <streambuf>
#include <boost/program_options.hpp>

#include "genome.h"
#include "renderer.h"
#include "controller.h"
#include "component.h"
#include "flam_gl_view.h"

namespace po = boost::program_options;
using std::string;

static FlamGLView* view;

void renderScene(void) {
  glutSetWindowTitle(view->component()->controller()->GetWindowTitle().c_str());
  view->Render();
  glutSwapBuffers();
}

void changeSize(int w, int h) {
  view->SetSize(w, h);
}


void processNormalKeys(unsigned char key, int /*x*/, int /*y*/) {
  if (key == 27) {
    exit(0);
  }

  if (key == ' ') {
    view->component()->controller()->Next();
  }
}

int main(int argc, char *argv[]) {
  std::set_terminate(UnhandledExceptionHandler);

  string dir("../sheeps/");

  po::options_description desc("Allowed options");
  desc.add_options()
    ("help", "produce help message")
    ("dir", po::value<string>(&dir), "directory")
    ;

  po::variables_map vm;
  po::store(po::parse_command_line(argc, argv, desc), vm);
  po::notify(vm);

  if (vm.count("help")) {
    std::cout << desc << "\n";
    return 1;
  }

  if (!vm.count("dir")) {
    std::cerr << "--dir was not set.\n";
    return 1;
  }

  {
    boost::shared_ptr<Controller> slide_show(
        new SlideshowController("../dir/"));
    boost::shared_ptr<Controller> animation(
        new AnimatingController(slide_show));
    boost::shared_ptr<FlamComponent> c(new FlamComponent(slide_show));
    view = new FlamGLView(c);
  }

  // init GLUT and create Window
  glutInit(&argc, argv);
  glutInitDisplayMode(GLUT_DEPTH | GLUT_DOUBLE | GLUT_RGBA);
  glutInitWindowPosition(100,100);
  glutInitWindowSize(1024,768);
  glutCreateWindow("FLAM");


  view->Init();
  changeSize(1024, 768);

  // register callbacks
  glutDisplayFunc(renderScene);
  glutIdleFunc(renderScene);
  glutReshapeFunc(changeSize);
  glutKeyboardFunc(processNormalKeys);

  // enter GLUT event processing cycle
  glutMainLoop();

  return 1;
}

