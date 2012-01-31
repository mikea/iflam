cmake_minimum_required(VERSION 2.8)
project(iflam_cmd)

include("../CMakeFunctions.txt")
include_directories("../core")

ADD_BOOST()

find_package(GLUT REQUIRED)
find_package(OpenGL REQUIRED)

add_executable(iflamgl main.cpp)
target_link_libraries(iflamgl core ${GLUT_LIBRARIES} ${OPENGL_LIBRARIES})


