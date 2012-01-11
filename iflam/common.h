#ifndef __COMMON_H__
#define __COMMON_H__

#include <boost/array.hpp>
#include <boost/exception/all.hpp>
#include <boost/math/constants/constants.hpp>
#include <boost/random/mersenne_twister.hpp>
#include <boost/random/uniform_real_distribution.hpp>
#include <boost/random/uniform_int_distribution.hpp>

// type definition
typedef double Float;

// Initialized array
template<typename T, std::size_t N>
class array : public boost::array<T, N> {
  public:
    array() {
      memset(this->elems, 0, sizeof(this->elems));
    }
};

typedef array<Float, 3> Color;

// exceptions
struct error : virtual boost::exception, virtual std::exception { };
typedef boost::error_info<struct tag_error_message, std::string> error_message;

// constants
const Float kPI = boost::math::constants::pi<Float>();
const Float kEpsilon = 1e-10;

class Random {
  public:
    static Float rnd();
    static Float crnd();
    static bool brnd();

    static boost::random::mt19937 rng_;
};

#endif

