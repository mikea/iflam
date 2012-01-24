#include "animator.h"
#include "genome.h"

PrimitiveAnimator::PrimitiveAnimator() { }
PrimitiveAnimator::~PrimitiveAnimator() { }

class CoordinateAnimator : public PrimitiveAnimator {
  public:
    CoordinateAnimator() : xform_(0), coef_(0) { }

    void Randomize(const Genome& genome, Random* random) {
      xform_ = random->irnd(genome.xforms().size());
      coef_ = random->irnd(6);
      amp_ = random->crnd();
    }

    virtual void Animate(const Signal& signal, Genome* genome) const {
      genome->mutable_xforms()->at(xform_).mutable_coefs()->at(coef_) *=
        (1 + signal.max_vol() * amp_);
    }

    virtual std::ostream& Print(std::ostream &os) const {
      os << "CoordinateAnimator["
         << "xform=" << xform_
         << ", coef=" << coef_
         << ", amp=" << amp_
         << "]\n";
      return os;
    }

  private:
    size_t xform_;
    size_t coef_;
    double amp_;
};

class PointRotator : public PrimitiveAnimator {
  public:
    PointRotator() : xform_(0) { }

    void Randomize(const Genome& genome, Random* random) {
      xform_ = random->irnd(genome.xforms().size());
      idx_ = random->irnd(3);
      amp_ = random->crnd();
    }

    virtual void Animate(const Signal& signal, Genome* genome) const {
      array<Float, 6>* coefs = genome->mutable_xforms()->at(xform_).mutable_coefs();

      double a = coefs->at(0);
      double b = coefs->at(2);
      double c = coefs->at(4);
      double d = coefs->at(1);
      double e = coefs->at(3);
      double f = coefs->at(5);

      double x = 0, y = 0;
      switch (idx_) {
        default: BOOST_ASSERT(false); break;
        case 0:
          x = a; y = d; break;
        case 1:
          x = b; y = e; break;
        case 2:
          x = c; y = f; break;
      }

      x *= (1 + signal.max_vol() * amp_);
      y *= (1 + signal.max_vol() * amp_);

      switch (idx_) {
        default: BOOST_ASSERT(false); break;
        case 0:
          a = x; d = y; break;
        case 1:
          b = x; e = y; break;
        case 2:
          c = x; f = y; break;
      }

      coefs->at(0) = a;
      coefs->at(2) = b;
      coefs->at(4) = c;
      coefs->at(1) = d;
      coefs->at(3) = e;
      coefs->at(5) = f;
    }

    virtual std::ostream& Print(std::ostream &os) const {
      os << "PointRotator["
         << "xform=" << xform_
         << ", idx=" << idx_
         << ", amp=" << amp_
         << "]\n";
      return os;
    }

  private:
    size_t xform_;
    size_t idx_;
    double amp_;
};

void Animator::Animate(const Signal& signal, Genome* genome) const {
  for (size_t i = 0; i < animators_.size(); ++i) {
    animators_[i]->Animate(signal, genome);
  }
//  genome->mutable_xforms()->at(0).mutable_coefs()->at(3) += log(1 + signal.max_vol_);
//  double t = signal.time_;
//  t /= 10;
//  //genome->Move(sin(t), cos(t));
//  genome->Magnify(- sin(t/10) * sin(t/10) / 10);
//  genome->Rotate(sin(t) * 180);
}

void Animator::Randomize(const Genome& genome) {
  animators_.clear();
  Random random;
  PrimitiveAnimator* animator;
  switch (random.irnd(2)) {
    case 0: animator = new CoordinateAnimator(); break;
    case 1: animator = new PointRotator(); break;
  }

  animator->Randomize(genome, &random);
  std::cout << "Animator: " << *animator;
  animators_.push_back(animator);
}

