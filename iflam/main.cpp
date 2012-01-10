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

  po::options_description desc("Allowed options");
  desc.add_options()
    ("help", "produce help message")
    ("file", po::value<string>(), "flam3 file")
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

  int width = 800 * 4;
  int height = 592 * 4;

  RenderBuffer buffer(genome, width, height);
  RenderState state(genome, &buffer);
  state.Iterate();

  boost::gil::rgb8_image_t img(width, height);
  boost::gil::rgb8_view_t v(view(img));
  buffer.Render(&v);
  boost::gil::png_write_view("render.png", v);

  return 0;
}

