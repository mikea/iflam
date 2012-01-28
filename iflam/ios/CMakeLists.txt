cmake_minimum_required(VERSION 2.8)
project(iflam_osx)

SET(CMAKE_OSX_ARCHITECTURES "i386")
SET(CMAKE_OSX_SYSROOT /Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/${TARGETSDK})

#include("../CMakeFunctions.txt")
# add_subdirectory("../core" "core")
# include_directories("../core")
#ADD_BOOST()


#FILE(GLOB BOOST_src "${BOOST_ROOT}/libs/exception/src/*.cpp")

add_executable(
  flam
  MACOSX_BUNDLE
  main.m
  flamAppDelegate.m flamAppDelegate.h
  flamViewController.m flamViewController.h
  # FlamView.mm FlamView.h
  flam-Info.plist
  #${BOOST_src}
  )

set_target_properties(flam PROPERTIES
  MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/flam-Info.plist"
  MACOSX_BUNDLE_GUI_IDENTIFIER "com.mikea.flam")

# target_link_libraries(flam core)
target_link_libraries(flam "-framework UIKit -framework Foundation -framework CoreGraphics")
set(CMAKE_EXE_LINKER_FLAGS "-Xlinker -objc_abi_version -Xlinker 2")
# target_link_libraries(flam ${Boost_LIBRARIES})

#COMPILE_XIB(flam "MainWindow.xib")
#COMPILE_XIB(flam "flamViewController.xib")



