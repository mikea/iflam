cmake_minimum_required(VERSION 2.8)

project(iflam_core)

include(../CMakeFunctions.txt)

ADD_BOOST()

if (APPLE)
  FIND_PACKAGE(OPENCL REQUIRED)
endif()

########################################################
add_definitions(-DTIXML_USE_STL)

add_library(tinyxml
  tinyxml/tinyxml.cpp
  tinyxml/tinystr.cpp
  tinyxml/tinyxmlerror.cpp
  tinyxml/tinyxmlparser.cpp
  )


add_library(core
  common.cpp common.h
  color.cpp color.h
  genome.cpp genome.h
  renderer.cpp renderer.h
  animator.cpp animator.h
  controller.cpp controller.h
  component.cpp component.h
  )
target_link_libraries(core tinyxml ${Boost_LIBRARIES})

# target_link_libraries(core tinyxml ${Boost_LIBRARIES})

# add_executable(color_test color_test.cpp)
# target_link_libraries(color_test core)
#
# enable_testing()
# add_test(color_test color_test)
