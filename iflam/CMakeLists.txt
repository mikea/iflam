cmake_minimum_required(VERSION 2.8)
project(iflam)

include("CMakeFunctions.txt")
ADD_BOOST()

if (APPLE)
  #  set(CMAKE_CXX_COMPILER clang++)
  #  set(CMAKE_C_COMPILER clang)
endif()

add_subdirectory(core)

########################################################
if (IPHONE)
  add_subdirectory(ios)
else (IPHONE)
  set(CMAKE_CXX_FLAGS "-g -Wall -Wextra -O3 -fno-strict-aliasing")
  enable_testing()

  add_subdirectory(cmd)

  if (APPLE)
    add_subdirectory(osx)
  endif()
endif (IPHONE)

