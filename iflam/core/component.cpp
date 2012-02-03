#include "component.h"

#include "controller.h"
#include "genome.h"
#include "renderer.h"

Component::Component(boost::shared_ptr<Controller> controller)
  : controller_(controller),
    model_(controller_->model()),
    width_(0),
    height_(0) { }

void Component::Tick() {
  {
    controller_->Tick();
    boost::shared_ptr<Genome> newGenome(model_->genome());
    if (genome_ != newGenome) {
      Reset(newGenome);
    }
  }
  {
    double start = WallTime();

    while (WallTime() - start < 1/25.0) {
      state_->Iterate(10000);
    }
  }
}

void Component::SetSize(size_t width, size_t height) {
  width_ = width;
  height_ = height;
  Reset(genome_);
}

void Component::Reset(boost::shared_ptr<Genome> genome) {
  genome_ = genome;
  if (genome_.get()) {
    buffer_.reset(new RenderBuffer(*genome_, width_, height_));
    state_.reset(new RenderState(*genome_, buffer_.get()));
  }
}

