cmake_minimum_required(VERSION 2.8)
project(iflam_osx)


SET(TARGETSDK iPhoneSimulator4.3.sdk)
#SET(CMAKE_OSX_SYSROOT /Developer/Platforms/iPhoneOS.platform/Developer/SDKs/${TARGETSDK})
SET(CMAKE_OSX_SYSROOT /Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/${TARGETSDK})
# SET(CMAKE_OSX_SYSROOT iPhoneOS)
# set(CMAKE_OSX_ARCHITECTURES $(ARCHS_STANDARD_64_BIT))
SET(CMAKE_OSX_ARCHITECTURES "i386")
#SET(CMAKE_C_COMPILER "/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin/llvm-gcc-4.2")
#SET(CMAKE_CXX_COMPILER "/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin/llvm-gcc-4.2")

SET(CMAKE_C_COMPILER "/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin/gcc-4.2")
SET(CMAKE_CXX_COMPILER "/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin/gcc-4.2")
# SET(CMAKE_C_COMPILER "/Users/aizatsky/Temp/gcc.sh")
# SET(CMAKE_CXX_COMPILER "/Users/aizatsky/Temp/gcc.sh")

# -include ${CMAKE_CURRENT_SOURCE_DIR}/flam-Prefix.pch
#####SET(SHARED_C_FLAGS " -pipe -Wno-trigraphs -fpascal-strings -Os -mdynamic-no-pic -Wreturn-type -Wunused-variable -fexceptions -fasm-blocks -mmacosx-version-min=10.6 -gdwarf-2 -fvisibility=hidden -fobjc-abi-version=2 -fobjc-legacy-dispatch -D__IPHONE_OS_VERSION_MIN_REQUIRED=40300 ")
# add_definitions(-std=gnu99)
add_definitions(-D__IPHONE_OS_VERSION_MIN_REQUIRED=40300)
#add_definitions(-Wno-trigraphs)
#add_definitions(-Wreturn-type)
#add_definitions(-Wunused-variable)
#add_definitions(-fasm-blocks)
#add_definitions(-fexceptions)
#add_definitions(-fmessage-length=0)
add_definitions(-fobjc-abi-version=2)
add_definitions(-fobjc-legacy-dispatch)
#add_definitions(-fpascal-strings)
#add_definitions(-fvisibility=hidden)
#add_definitions(-gdwarf-2)
#add_definitions(-mdynamic-no-pic)
add_definitions(-mmacosx-version-min=10.6)
#add_definitions(-include ${CMAKE_CURRENT_SOURCE_DIR}/flam-Prefix.pch)
#add_definitions(-DNS_BLOCK_ASSERTIONS=1)


# SET(SHARED_C_FLAGS "-fpascal-strings -fexceptions -fasm-blocks -mmacosx-version-min=10.6 -gdwarf-2 -fvisibility=hidden -fobjc-legacy-dispatch  ")

#SET(CMAKE_C_FLAGS "-x objective-c ${SHARED_C_FLAGS}")
#SET(CMAKE_CXX_FLAGS "-x objective-c ${SHARED_C_FLAGS}")

#include_directories("/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator4.3.sdk/usr/include/")

SET(ENV{MACOSX_DEPLOYMENT_TARGET} "10.6")
add_executable(
  flam
  MACOSX_BUNDLE
  main.m
  flamAppDelegate.m flamAppDelegate.h
  flamViewController.m flamViewController.h
  flam-Info.plist)

set_target_properties(flam PROPERTIES
  MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/flam-Info.plist"
  MACOSX_BUNDLE_GUI_IDENTIFIER "com.mikea.flam")

target_link_libraries(flam "-framework UIKit -framework Foundation -framework CoreGraphics")
set(CMAKE_EXE_LINKER_FLAGS "-Xlinker -objc_abi_version -Xlinker 2")
