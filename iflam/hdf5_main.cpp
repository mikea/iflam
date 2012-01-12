#include <boost/program_options.hpp>
#include<exception>
#include <hdf5.h>

#include "common.h"
#include "genome.h"
#include "renderer.h"

namespace po = boost::program_options;
using std::string;

double* SliceBuffer(const RenderBuffer& buffer, int channel) {
  size_t width = buffer.width();
  size_t height = buffer.height();
  double* result = new double[width * height];
  const Float* const accum = buffer.accum();

  for (size_t y = 0; y < height; ++y) {
    for (size_t x = 0; x < width; ++x) {
      result[x * height + y] =
        accum[(x + y * width) * 4 + channel];
    }
  }

  return result;
}

int main(int argc, char *argv[]) {
  std::set_terminate(UnhandledExceptionHandler);

  // TODO: dedup with main.cpp

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

  hid_t file_id = H5Fcreate("render.h5", H5F_ACC_TRUNC, H5P_DEFAULT, H5P_DEFAULT);

  hsize_t dimensions[2];
  dimensions[0] = width;
  dimensions[1] = height;

  hid_t dataspace_id = H5Screate_simple(2, dimensions, NULL);
  BOOST_ASSERT(dataspace_id >= 0);

  {
    hid_t dataset_id = H5Dcreate(file_id, "/d", H5T_NATIVE_DOUBLE, dataspace_id,
      H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT);

    boost::scoped_array<double> t_slice(SliceBuffer(render_buffer, 3));

    BOOST_VERIFY(
        !H5Dwrite(dataset_id, H5T_NATIVE_DOUBLE, H5S_ALL, H5S_ALL, H5P_DEFAULT,
          t_slice.get()));

    BOOST_VERIFY(!H5Dclose(dataset_id));
  }

  {
    hid_t dataset_id = H5Dcreate(file_id, "/r", H5T_NATIVE_DOUBLE, dataspace_id,
      H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT);

    boost::scoped_array<double> t_slice(SliceBuffer(render_buffer, 0));

    BOOST_VERIFY(
        !H5Dwrite(dataset_id, H5T_NATIVE_DOUBLE, H5S_ALL, H5S_ALL, H5P_DEFAULT,
          t_slice.get()));

    BOOST_VERIFY(!H5Dclose(dataset_id));
  }

  {
    hid_t dataset_id = H5Dcreate(file_id, "/g", H5T_NATIVE_DOUBLE, dataspace_id,
      H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT);

    boost::scoped_array<double> t_slice(SliceBuffer(render_buffer, 1));

    BOOST_VERIFY(
        !H5Dwrite(dataset_id, H5T_NATIVE_DOUBLE, H5S_ALL, H5S_ALL, H5P_DEFAULT,
          t_slice.get()));

    BOOST_VERIFY(!H5Dclose(dataset_id));
  }

  {
    hid_t dataset_id = H5Dcreate(file_id, "/b", H5T_NATIVE_DOUBLE, dataspace_id,
      H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT);

    boost::scoped_array<double> t_slice(SliceBuffer(render_buffer, 2));

    BOOST_VERIFY(
        !H5Dwrite(dataset_id, H5T_NATIVE_DOUBLE, H5S_ALL, H5S_ALL, H5P_DEFAULT,
          t_slice.get()));

    BOOST_VERIFY(!H5Dclose(dataset_id));
  }


  BOOST_VERIFY(!H5Sclose(dataspace_id));
  BOOST_VERIFY(!H5Fclose(file_id));
}


