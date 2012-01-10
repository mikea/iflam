#ifndef __COMMON_H__
#define __COMMON_H__

#include <boost/array.hpp>
#include <boost/exception/all.hpp>
#include <boost/math/constants/constants.hpp>
#include <boost/random/mersenne_twister.hpp>
#include <boost/random/uniform_real_distribution.hpp>
#include <boost/random/uniform_int_distribution.hpp>


// Initializing array
template<typename T, std::size_t N>
class array : public boost::array<T, N> {
  public:
    array() {
      memset(this->elems, 0, sizeof(this->elems));
    }
};

struct error : virtual boost::exception, virtual std::exception { };
typedef boost::error_info<struct tag_error_message, std::string> error_message;

const double kPI = boost::math::constants::pi<double>();
const double kEpsilon = 1e-10;

class Random {
  public:
    static double rnd();
    static double crnd();
    static bool brnd();

    static boost::random::mt19937 rng_;
};

#endif

