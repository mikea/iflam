cmake_minimum_required(VERSION 2.8)
project(iflam_cmd)

include("../CMakeFunctions.txt")
include_directories("../core")
include_directories("/usr/X11/include")

ADD_BOOST()

add_executable(bin2c bin2c.c)

macro(SHADER _from)
  PARSE_ARGUMENTS(SHADER
    "DEPENDS"
    ""
    ${ARGN})
  CPP(${CMAKE_CURRENT_SOURCE_DIR}/${_from} ${_from}.i DEPENDS ${SHADER_DEPENDS})
  BIN2C(${_from}.i ${_from}.cpp ${_from})
endmacro(SHADER)

SHADER(render.vertex)
SHADER(render_main.fragment
  DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/render.fragment)

FILE(GLOB gl_boost_srcs
  )

add_definitions(-DGL_GLEXT_PROTOTYPES)

find_package(GLUT REQUIRED)
find_package(OpenGL REQUIRED)

add_executable(iflamgl main.cpp render.vertex.cpp render_main.fragment.cpp ${gl_boost_srcs})
target_link_libraries(iflamgl core ${GLUT_LIBRARIES} ${OPENGL_LIBRARIES})


