#include <assert.h>
#include "genome.h"

int main(int argc, char *argv[]) {
  Genome genome;
  std::string error;
  bool result = genome.Read(
      "/Users/aizatsky/Projects/iflam/flam-java/flams/e_1.flam3",
      &error);
  if (!result) {
    std::cerr << "Parsing error: " << error << "\n";
    abort();
  }

  return 0;
}

