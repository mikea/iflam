#include "controller.h"
#include <iostream>

#include <vector>

#include "genome.h"
#include "animator.h"

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
            std::cout << "Loading: " << p << "\n";
            boost::shared_ptr<Genome> g(new Genome());
            g->Read(p.native());
            model_->set_genome(g);
            current_path_ = p.native();
            return;
        }
    }
}

AnimatingController::AnimatingController(
        boost::shared_ptr<Controller> delegate)
: delegate_(delegate),
animator_(new Animator()) {
}


void AnimatingController::Tick() {
    delegate_->Tick();
    {
        boost::shared_ptr<Genome> genome = delegate_->model()->genome();

        if (genome_.get() != genome.get()) {
            genome_ = genome;
            model_->set_genome(genome_);
            animator_->Randomize(*genome_);
        }
    }

    boost::shared_ptr<Genome> genome(new Genome(*genome_));
    Signal signal(WallTime(), 0);
    animator_->Animate(signal, genome.get());
    model_->set_genome(genome);
}

void AnimatingController::Next() {
    delegate_->Next();
}

std::string AnimatingController::GetWindowTitle() {
    return delegate_->GetWindowTitle();
}
