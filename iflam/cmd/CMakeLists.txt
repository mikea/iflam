cmake_minimum_required(VERSION 2.8)
project(iflam_cmd)

include("../CMakeFunctions.txt")
include_directories("../core")

ADD_BOOST()
ADD_PNG()

#############################################

FILE(GLOB cmd_boost_srcs
  "${BOOST_ROOT}/libs/program_options/src/*.cpp"
  )

add_executable(iflam-render
  main.cpp
  ${cmd_boost_srcs}
  )

target_link_libraries(iflam-render core ${Boost_LIBRARIES} ${PNG_LIBRARIES})
target_link_libraries(iflam-render ${PNG_LIBRARIES})

###############################################
# HDF export

set(ENV{HDF5_ROOT} "$ENV{HOME}/Packages/hdf5")

if (UNIX AND (NOT APPLE))
  ADD_HDF5()
  add_executable(hdf5_render hdf5_main.cpp)
  target_link_libraries(hdf5_render core
    ${HDF5_LIBRARIES} ${PNG_LIBRARIES} ${Boost_LIBRARIES})
endif()

######################################
# Perf test

add_executable(perf_test perf_test.cpp)
target_link_libraries(perf_test core ${Boost_LIBRARIES})

