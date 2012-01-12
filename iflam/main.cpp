#include <assert.h>
#include <boost/program_options.hpp>
#include "genome.h"
#include "renderer.h"
#include <boost/gil/gil_all.hpp>
#include <boost/gil/extension/io/png_io.hpp>

namespace po = boost::program_options;
using std::string;

void UnhandledExceptionHandler()
{
   std::cerr << "Unhandled exception:\n";
   std::cerr << boost::diagnostic_information(boost::current_exception());
}

int main(int argc, char *argv[]) {
  std::set_terminate(UnhandledExceptionHandler);

  int iterations;
  int width;
  int height;

  po::options_description desc("Allowed options");
  desc.add_options()
    ("help", "produce help message")
    ("file", po::value<string>(), "flam3 file")
    ("iterations", po::value<int>(&iterations)->default_value(1000000), "number of iterations")
    ("width", po::value<int>(&width)->default_value(1024), "render width")
    ("height", po::value<int>(&height)->default_value(768), "render height")
    ;

  po::variables_map vm;
  po::store(po::parse_command_line(argc, argv, desc), vm);
  po::notify(vm);

  if (vm.count("help")) {
    std::cout << desc << "\n";
    return 1;
  }

  if (!vm.count("file")) {
    std::cout << "-file was not set.\n";
    return 1;
  }

  Genome genome;
  genome.Read(vm["file"].as<string>());

  RenderBuffer render_buffer(genome, width, height);
  RenderState state(genome, &render_buffer);
  {
    Stopwatch sw("Iterations took:", iterations);
    state.Iterate(iterations);
  }

  boost::scoped_array<uint8_t> buffer(new uint8_t[width * height * 4]);
  {
    Stopwatch sw("Rendering took:", width * height, "px");
    render_buffer.Render(buffer.get());
  }

  typedef boost::gil::rgb8_view_t view_t;
  typedef view_t::reference pixel_t_ref;

  boost::gil::rgb8_image_t img(width, height);
  boost::gil::rgb8_view_t v(view(img));
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      int offset = (x + (height - y) * width) * 4;
      pixel_t_ref pixel = v(x, y);
      pixel[0] = buffer[offset];
      pixel[1] = buffer[offset + 1];
      pixel[2] = buffer[offset + 2];
    }
  }
  
  boost::gil::png_write_view("render.png", v);

  return 0;
}

