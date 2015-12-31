// Needed for GIL
#define png_infopp_NULL (png_infopp)NULL
#define int_p_NULL (int*)NULL

#include <assert.h>
#include <boost/make_shared.hpp>
#include <boost/program_options.hpp>
#include "genome.h"
#include "renderer.h"
#include <boost/gil/gil_all.hpp>
#include <boost/gil/extension/io/png_io.hpp>
#include <iostream>

namespace po = boost::program_options;
using std::string;

class Rgb8Image {
  public:
    Rgb8Image(boost::gil::rgb8_view_t* view) : view_(view) { }
    typedef boost::gil::rgb8_view_t::reference pixel_t_ref;

    void Set(int x, int y, Float r, Float g, Float b, Float /* a */) {
      pixel_t_ref pixel = (*view_)(x, y);
      pixel[0] = r;
      pixel[1] = g;
      pixel[2] = b;
    }

  private:
    boost::gil::rgb8_view_t* view_;
};

int main(int argc, char *argv[]) {
  std::set_terminate(UnhandledExceptionHandler);

  int iterations;
  int width;
  int height;
  std::string in_file;
  std::string out_file;

  po::options_description desc("Allowed options");
  desc.add_options()
    ("help", "produce help message")
    ("file", po::value<string>(&in_file), "flam3 file")
    ("iterations", po::value<int>(&iterations)->default_value(1000000), "number of iterations")
    ("width", po::value<int>(&width)->default_value(1024), "render width")
    ("height", po::value<int>(&height)->default_value(768), "render height")
    ("out", po::value<std::string>(&out_file)->default_value("render.png"), "output file name")
    ;

  po::variables_map vm;
  po::store(po::parse_command_line(argc, argv, desc), vm);
  po::notify(vm);

  if (vm.count("help")) {
    std::cout << desc << "\n";
    return 1;
  }

  if (!vm.count("file")) {
    std::cout << "--file was not set.\n";
    return 1;
  }

  std::cout << "Rendering " << in_file << " to " << out_file << "\n";

  boost::shared_ptr<Genome> genome = boost::make_shared<Genome>();
  genome->Read(in_file);

  RenderBuffer render_buffer(width, height);
  RenderState state(*genome, &render_buffer);
  {
    Stopwatch sw("Iterations took:", iterations);
    state.Iterate(iterations);
  }

  boost::gil::rgb8_image_t img(width, height);
  boost::gil::rgb8_view_t v(view(img));

  {
    Stopwatch sw("Rendering took:", width * height, "px");
    Rgb8Image rgb8_image(&v);
    render_buffer.Render(*genome, &rgb8_image);
  }

  boost::gil::png_write_view(out_file, v);

  return 0;
}
