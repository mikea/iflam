cmake_minimum_required(VERSION 2.8)
project(iflam)
set(CMAKE_CXX_FLAGS "-g -Wall -Wextra -O0 -fno-strict-aliasing -msse2 -ffast-math")


include("CMakeFunctions.txt")
ADD_BOOST()

if (APPLE)
  #  set(CMAKE_CXX_COMPILER clang++)
  #  set(CMAKE_C_COMPILER clang)
else(APPLE)
  set(CMAKE_CXX_FLAGS "-fassociative-math ${CMAKE_CXX_FLAGS}")
endif()

add_subdirectory(core)

########################################################
if (IPHONE)
  add_subdirectory(ios)
else (IPHONE)
  enable_testing()

  add_subdirectory(cmd)
  add_subdirectory(gl)

  if (APPLE)
    add_subdirectory(osx)
  endif()
endif (IPHONE)

