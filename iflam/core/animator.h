#ifndef __ANIMATOR_H__
#define __ANIMATOR_H__

#include <vector>
#include <ostream>

class Genome;
class Random;

class Signal {
  public:
    Signal(double time, double max_vol)
      : time_(time), max_vol_(max_vol) { }

    double max_vol() const { return max_vol_; }
  private:
    friend class Animator;
    double time_;
    double max_vol_;
};


class PrimitiveAnimator {
  public:
    PrimitiveAnimator();
    virtual ~PrimitiveAnimator();
    virtual void Animate(const Signal& signal, Genome* genome) const = 0;
    virtual void Randomize(const Genome& genome, Random* random) = 0;
    virtual std::ostream& Print(std::ostream &os) const = 0;
};

inline std::ostream& operator<< (std::ostream& os, const PrimitiveAnimator& a) {
  return a.Print(os);
}

class Animator {
  public:
    Animator() { }
    ~Animator() { }

    void Animate(const Signal& signal, Genome* genome) const;

    void Randomize(const Genome& genome);
  private:
    // todo; ptr_vector
    std::vector<PrimitiveAnimator*> animators_;
};

#endif

