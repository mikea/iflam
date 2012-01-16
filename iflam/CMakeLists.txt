cmake_minimum_required(VERSION 2.6)

project(iflam)

if (APPLE)
  set(CMAKE_CXX_COMPILER clang++)
  set(CMAKE_C_COMPILER clang)
endif()

set(CMAKE_CXX_FLAGS "-g -Wall -O3")
add_definitions(-DTIXML_USE_STL)

########################################################

set(Boost_USE_STATIC_LIBS ON)
# set(Boost_DEBUG ON)
set(BOOST_ROOT "$ENV{HOME}/Packages/boost_1_48_0")
set(BOOST_LIBRARYDIR "${BOOST_ROOT}/stage/lib")
find_package(Boost 1.48 REQUIRED exception program_options unit_test_framework)

if (Boost_FOUND)
  message(STATUS "Boost include dir: ${Boost_INCLUDE_DIR}")
  message(STATUS "Boost library dir: ${Boost_INCLUDE_DIRS}")
  message(STATUS "Boost libraries: ${Boost_LIBRARIES}")
  include_directories(${Boost_INCLUDE_DIR})
  link_directories(${Boost_LIBRARY_DIRS})
endif()

########################################################

add_library(tinyxml
  tinyxml/tinyxml.cpp
  tinyxml/tinystr.cpp
  tinyxml/tinyxmlerror.cpp
  tinyxml/tinyxmlparser.cpp
  )

add_library(impl
  common.cpp
  color.cpp
  genome.cpp
  renderer.cpp
  )

add_executable(iflam
  main.cpp
  )

target_link_libraries(iflam impl tinyxml ${Boost_LIBRARIES})

# Tests

add_executable(color_test
  color_test.cpp
  )
target_link_libraries(color_test impl tinyxml ${Boost_LIBRARIES})

add_executable(perf_test
  perf_test.cpp
  )
target_link_libraries(perf_test impl tinyxml ${Boost_LIBRARIES})

find_package(PNG REQUIRED)

set(PNG_INCLUDE_DIR "/usr/X11/include/")
if (PNG_FOUND)
  message(STATUS "PNG include dir: ${PNG_INCLUDE_DIR}")

  include_directories(${PNG_INCLUDE_DIR})
  target_link_libraries(iflam ${PNG_LIBRARIES})
endif()

set(ENV{HDF5_ROOT} "$ENV{HOME}/Packages/hdf5")

if (UNIX AND (NOT APPLE))
  find_package(HDF5 1.8.8 REQUIRED)
endif()

if (HDF5_FOUND)
  message(STATUS "HDF5 include dir: ${HDF5_INCLUDE_DIR}")
  message(STATUS "HDF5 libraries: ${HDF5_LIBRARIES}")

  add_executable(hdf5_render hdf5_main.cpp)
  target_link_libraries(hdf5_render impl
    ${HDF5_LIBRARIES} ${PNG_LIBRARIES} tinyxml ${Boost_LIBRARIES})
  include_directories(${HDF5_INCLUDE_DIR})
endif()

if (APPLE)
  add_definitions(-D__ASSERTMACROS__)
  add_executable(
    iflamapp
    MACOSX_BUNDLE
    main.m
    iflamAppDelegate.m
    FlamView.mm
    )

  set_target_properties(iflamapp PROPERTIES
    MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/iflam-Info.plist"
    MACOSX_BUNDLE_GUI_IDENTIFIER "com.mikea.iflam")

  target_link_libraries(iflamapp impl tinyxml "-framework Cocoa")

  set(iflamapp_xibs "en.lproj/MainMenu")

  FIND_PROGRAM( IBTOOL ibtool HINTS "/usr/bin" "${OSX_DEVELOPER_ROOT}/usr/bin" )
  if ( ${IBTOOL} STREQUAL "IBTOOL-NOTFOUND" )
      MESSAGE( SEND_ERROR "ibtool can not be found" )
  ENDIF()

  foreach(xib ${iflamapp_xibs})
    set(_nib_file ${CMAKE_CURRENT_BINARY_DIR}/${CMAKE_CFG_INTDIR}/iflamapp.app/Contents/Resources/${xib}.nib)
    add_custom_command (TARGET iflamapp POST_BUILD
      COMMAND mkdir -p `dirname ${_nib_file}` &&
      ${IBTOOL} --errors --warnings --notices --output-format human-readable-text
      --compile ${_nib_file}
      ${CMAKE_CURRENT_SOURCE_DIR}/${xib}.xib
      COMMENT "Compiling ${CMAKE_CURRENT_SOURCE_DIR}/${xib}.xib")

  endforeach()

endif()
