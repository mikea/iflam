cmake_minimum_required(VERSION 2.8)

project(iflam_core)

include(../CMakeFunctions.txt)

ADD_BOOST()
########################################################
add_definitions(-DTIXML_USE_STL)

add_library(tinyxml
  tinyxml/tinyxml.cpp
  tinyxml/tinystr.cpp
  tinyxml/tinyxmlerror.cpp
  tinyxml/tinyxmlparser.cpp
  )

add_library(core
  common.cpp
  color.cpp
  genome.cpp
  renderer.cpp
  )

target_link_libraries(core tinyxml ${Boost_LIBRARIES})

add_executable(color_test color_test.cpp)
target_link_libraries(color_test core)

