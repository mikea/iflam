cmake_minimum_required(VERSION 2.8)

if (APPLE)
  set(CMAKE_CXX_COMPILER clang++)
  set(CMAKE_C_COMPILER clang)
endif()

project(iflam)

set(CMAKE_MODULE_PATH "${CMAKE_SOURCE_DIR}/cmake")
set(CMAKE_INCLUDE_PATH "$ENV{HOME}/homebrew/include")
set(CMAKE_LIBRARY_PATH "$ENV{HOME}/homebrew/lib")

set(CMAKE_CXX_FLAGS "-g -Wall -Wextra -O0 -fno-strict-aliasing -msse2 -ffast-math")

if (APPLE)
else(APPLE)
  set(CMAKE_CXX_FLAGS "-fassociative-math ${CMAKE_CXX_FLAGS}")
endif()

include("CMakeFunctions.txt")
ADD_BOOST()

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

