cmake_minimum_required(VERSION 2.6)

set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_FLAGS "-g -Wall -DTIXML_USE_STL -O0")


project(iflam)
add_executable(iflam genome.cpp main.cpp
  tinyxml/tinyxml.cpp
  tinyxml/tinystr.cpp
  tinyxml/tinyxmlerror.cpp
  tinyxml/tinyxmlparser.cpp)

set(BOOST_ROOT "$ENV{HOME}/Packages/boost_1_48_0")
find_package(Boost 1.48 REQUIRED)

include_directories(${Boost_INCLUDE_DIR})
target_link_libraries(iflam ${Boost_LIBRARIES})


