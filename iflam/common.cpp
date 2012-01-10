#include "common.h"
#include <boost/math/special_functions/fpclassify.hpp>

namespace {
boost::random::uniform_real_distribution<> rndDist(0, 1);
boost::random::uniform_real_distribution<> crndDist(-1, 1);
}

boost::random::mt19937 Random::rng_(std::time(0));

double Random::rnd() {
  return rndDist(rng_);
}

double Random::crnd() {
  return crndDist(rng_);
}

