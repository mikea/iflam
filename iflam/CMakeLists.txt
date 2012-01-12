cmake_minimum_required(VERSION 2.6)

project(iflam)

if (APPLE)
  set(CMAKE_CXX_COMPILER clang++)
  set(CMAKE_C_COMPILER clang)
endif()

set(CMAKE_CXX_FLAGS "-g -Wall -O3")
add_definitions(-DTIXML_USE_STL)

########################################################

set(Boost_USE_STATIC_LIBS ON)
set(Boost_DEBUG ON)
set(BOOST_ROOT "$ENV{HOME}/Packages/boost_1_48_0")
set(BOOST_LIBRARYDIR "${BOOST_ROOT}/stage/lib")
find_package(Boost 1.48 REQUIRED exception program_options unit_test_framework)

if (Boost_FOUND)
  message(STATUS "Boost include dir: ${Boost_INCLUDE_DIR}")
  message(STATUS "Boost library dir: ${Boost_INCLUDE_DIRS}")
  message(STATUS "Boost libraries: ${Boost_LIBRARIES}")
  include_directories(${Boost_INCLUDE_DIR})
  link_directories(${Boost_LIBRARY_DIRS})
endif()

########################################################


add_library(tinyxml
  tinyxml/tinyxml.cpp
  tinyxml/tinystr.cpp
  tinyxml/tinyxmlerror.cpp
  tinyxml/tinyxmlparser.cpp
  )

add_library(impl
  common.cpp
  color.cpp
  genome.cpp
  renderer.cpp
  )

add_executable(iflam
  main.cpp
  )

target_link_libraries(iflam impl tinyxml ${Boost_LIBRARIES})

# Tests

add_executable(color_test
  color_test.cpp
  )
target_link_libraries(color_test impl tinyxml ${Boost_LIBRARIES})

add_executable(perf_test
  perf_test.cpp
  )
target_link_libraries(perf_test impl tinyxml ${Boost_LIBRARIES})

find_package(PNG REQUIRED)

set(PNG_INCLUDE_DIR "/usr/X11/include/")
if (PNG_FOUND)
  message(STATUS "PNG include dir: ${PNG_INCLUDE_DIR}")

  include_directories(${PNG_INCLUDE_DIR})
  target_link_libraries(iflam ${PNG_LIBRARIES})
endif()
