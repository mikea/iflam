#include "component.h"

#include "controller.h"
#include "genome.h"
#include "renderer.h"

FlamComponent::FlamComponent(boost::shared_ptr<Controller> controller)
: controller_(controller),
model_(controller_->model()),
width_(0),
height_(0) {
}

void FlamComponent::Tick() {
    {
        controller_->Tick();
        boost::shared_ptr<Genome> newGenome(model_->genome());
        if (genome_ != newGenome) {
            Reset(newGenome);
        }
    }
    {
        double start = WallTime();

        while (WallTime() - start < 1 / 25.0) {
            state_->Iterate(10000);
        }
    }
}

void FlamComponent::SetSize(size_t width, size_t height) {
    if (width_ == width && height_ == height) {
        return;
    }
    width_ = width;
    height_ = height;
    buffer_.reset(new RenderBuffer(width_, height_));
    Reset(genome_);
}

void FlamComponent::Reset(boost::shared_ptr<Genome> genome) {
    genome_ = genome;
    if (genome_.get()) {
        buffer_->Reset();
        state_.reset(new RenderState(*genome_, buffer_.get()));
    }
}

