#include <assert.h>
#include <boost/program_options.hpp>
#include "genome.h"

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

  return 0;
}

