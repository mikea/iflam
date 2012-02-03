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

FILE(GLOB core_boost_srcs
  "${BOOST_ROOT}/libs/exception/src/*.cpp"
  "${BOOST_ROOT}/libs/system/src/*.cpp"
  )

add_library(core_boost ${core_boost_srcs})


add_library(core
  common.cpp common.h
  color.cpp color.h
  genome.cpp genome.h
  renderer.cpp renderer.h
  animator.cpp animator.h
  controller.cpp controller.h
  )
target_link_libraries(core tinyxml core_boost)

# target_link_libraries(core tinyxml ${Boost_LIBRARIES})

# add_executable(color_test color_test.cpp)
# target_link_libraries(color_test core)
#
# enable_testing()
# add_test(color_test color_test)
