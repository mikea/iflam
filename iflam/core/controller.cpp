#include "controller.h"

#include <vector>

#include "genome.h"

void SlideshowController::Tick() {
  double duration = 60;

  if (WallTime() - last_change_ > duration) {
    LoadRandomSheep();
  }
}

void SlideshowController::LoadRandomSheep() {
  last_change_ = WallTime();
  std::vector<boost::filesystem::path> paths;
  std::copy(
      boost::filesystem::directory_iterator(dir_),
      boost::filesystem::directory_iterator(),
      std::back_inserter(paths));
  while (true) {
    boost::filesystem::path p = paths[rnd_.irnd(paths.size())];
    if (boost::filesystem::is_regular_file(p) &&
        boost::filesystem::extension(p) == ".flam3") {
      boost::shared_ptr<Genome> g(new Genome());
      g->Read(p.native());
      model_->set_genome(g);
      current_path_ = p.native();
      return;
    }
  }
}

