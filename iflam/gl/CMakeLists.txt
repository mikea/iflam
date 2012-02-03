cmake_minimum_required(VERSION 2.8)
project(iflam_cmd)

include("../CMakeFunctions.txt")
include_directories("../core")
include_directories("/usr/X11/include")

ADD_BOOST()

add_executable(bin2c bin2c.c)

BIN2C(blur.vertex)
BIN2C(blur.fragment)

FILE(GLOB gl_boost_srcs
  "${BOOST_ROOT}/libs/filesystem/v3/src/*.cpp"
  )

add_definitions(-DGL_GLEXT_PROTOTYPES)

find_package(GLUT REQUIRED)
find_package(OpenGL REQUIRED)

add_executable(iflamgl main.cpp blur.vertex.cpp blur.fragment.cpp ${gl_boost_srcs})
target_link_libraries(iflamgl core ${GLUT_LIBRARIES} ${OPENGL_LIBRARIES})


