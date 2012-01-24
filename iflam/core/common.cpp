#include "common.h"
#include <boost/math/special_functions/fpclassify.hpp>
#include <sys/time.h>

namespace {
boost::random::uniform_real_distribution<> rndDist(0, 1);
boost::random::uniform_real_distribution<> crndDist(-1, 1);
boost::random::uniform_int_distribution<> brndDist(0, 1);
}

Random::Random() : rng_(std::time(0)) { }

Float Random::rnd() {
  return rndDist(rng_);
}

Float Random::crnd() {
  return crndDist(rng_);
}

bool Random::brnd() {
  return brndDist(rng_) == 1;
}

double WallTime() {
  timeval tv;

  if (gettimeofday(&tv, NULL)) {
    BOOST_THROW_EXCEPTION(error());
  }

  return double(tv.tv_sec) + tv.tv_usec / 1e6;
}


Stopwatch::Stopwatch(const std::string& message, long count, std::string unit)
  : message_(message),
    count_(count),
    unit_(unit == "" ? "it" : unit),
    start_time_(WallTime()) {
}

Stopwatch::~Stopwatch() {
  double total_time = WallTime() - start_time_;

  std::cout << message_ << " " << total_time << " sec";
  if (count_ > 0) {
    std::cout << " (" << (count_ / total_time) << " " << unit_ << "/sec)";
  }
  std::cout << "\n";
}

void UnhandledExceptionHandler() {
   std::cerr << "Unhandled exception:\n";
   std::cerr << boost::diagnostic_information(boost::current_exception());
}

