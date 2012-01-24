#ifndef __COMMON_H__
#define __COMMON_H__

#include <boost/array.hpp>
#include <boost/exception/all.hpp>
#include <boost/math/constants/constants.hpp>
#include <boost/random/mersenne_twister.hpp>
#include <boost/random/uniform_real_distribution.hpp>
#include <boost/random/uniform_int_distribution.hpp>

void UnhandledExceptionHandler();

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
    Random();

    Float rnd();
    Float crnd();
    bool brnd();

    boost::random::mt19937 rng_;
};

double WallTime();

class Stopwatch {
  public:
    Stopwatch(const std::string& name, long count = -1, std::string unit = "");
    ~Stopwatch();

  private:
    std::string message_;
    long count_;
    std::string unit_;
    double start_time_;
};

#define BOOST_ASSERT_RANGE(x, a, b)  BOOST_ASSERT_MSG((a) <= (x) && (x) <= (b),\
    (std::string("Bad value for ") + #x + ": " + \
     boost::lexical_cast<std::string>(x)).c_str())

#if defined(BOOST_DISABLE_ASSERTS) || ( !defined(BOOST_ENABLE_ASSERT_HANDLER) && defined(NDEBUG) )
#define BOOST_VERIFY_MSG(expr, msg) ((void)(expr))
#else
#define BOOST_VERIFY_MSG(expr, msg) BOOST_ASSERT_MSG(expr, msg)

#define VERIFY_OSSTATUS_MSG(exp, msg) \
    do { \
       OSStatus __err = (exp); \
       BOOST_VERIFY_MSG(__err == 0, (std::string(msg) + ", OSStatus=" + boost::lexical_cast<std::string>(__err)).c_str()); \
    } while(0)

#define VERIFY_OSSTATUS(exp) \
    do { \
       OSStatus __err = (exp); \
       BOOST_VERIFY_MSG(__err == 0, (std::string("OSStatus=") + boost::lexical_cast<std::string>(__err)).c_str()); \
    } while(0)

#endif

#endif

