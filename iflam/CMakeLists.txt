cmake_minimum_required(VERSION 2.6)

#set(CMAKE_CXX_COMPILER clang++)
#set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_FLAGS "-g -Wall -O0")
add_definitions(-DTIXML_USE_STL)

project(iflam)
add_executable(iflam genome.cpp main.cpp
  tinyxml/tinyxml.cpp
  tinyxml/tinystr.cpp
  tinyxml/tinyxmlerror.cpp
  tinyxml/tinyxmlparser.cpp)

set(Boost_DEBUG ON)
set(BOOST_ROOT "$ENV{HOME}/Packages/boost_1_48_0")
find_package(Boost 1.48 REQUIRED program_options)

if (Boost_FOUND)
  message(STATUS "Boost include dir: ${Boost_INCLUDE_DIR}")
  message(STATUS "Boost library dir: ${Boost_INCLUDE_DIRS}")
  message(STATUS "Boost libraries: ${_boost_LIBRARIES}")

  set(Boost_USE_STATIC_LIBS ON)
  include_directories(${Boost_INCLUDE_DIR})
  link_directories(${Boost_LIBRARY_DIRS})
  target_link_libraries(iflam ${Boost_LIBRARIES})
endif()

