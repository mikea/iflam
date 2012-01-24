cmake_minimum_required(VERSION 2.8)
project(iflam)

include("CMakeFunctions.txt")
ADD_BOOST()

if (APPLE)
  #  set(CMAKE_CXX_COMPILER clang++)
  #  set(CMAKE_C_COMPILER clang)
endif()

set(CMAKE_CXX_FLAGS "-g -Wall -Wextra -O3 -fno-strict-aliasing")
enable_testing()

add_subdirectory(core)
add_subdirectory(cmd)

########################################################

if (APPLE)
  add_definitions(-D__ASSERTMACROS__)
  include_directories("CA/PublicUtility" "CA/AUBase" "CA/Utility")

  add_executable(
    iflamapp
    MACOSX_BUNDLE
    main.m
    AppDelegate.mm
    FlamView.mm
    fft/FFTBufferManager.cpp
    fft/SpectrumAnalysis.cpp
    fft/rad2fft.c
    "CA/PublicUtility/CAComponent.cpp"
    "CA/PublicUtility/CAAudioUnit.cpp"
    "CA/PublicUtility/CAHALAudioSystemObject.cpp"
    "CA/PublicUtility/CAHALAudioDevice.cpp"
    "CA/PublicUtility/CAAudioChannelLayout.cpp"
    "CA/PublicUtility/CAAudioChannelLayoutObject.cpp"
    "CA/PublicUtility/CAHALAudioObject.cpp"
    "CA/PublicUtility/CAComponentDescription.cpp"
    "CA/PublicUtility/CAStreamBasicDescription.cpp"
    "CA/PublicUtility/CAHALAudioStream.cpp"
    "CA/PublicUtility/CASpectralProcessor.cpp"
    "CA/PublicUtility/CABufferList.cpp"
    )

  set_target_properties(iflamapp PROPERTIES
    MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/iflam-Info.plist"
    MACOSX_BUNDLE_GUI_IDENTIFIER "com.mikea.iflam")

  target_link_libraries(iflamapp core "-framework Cocoa -framework CoreAudio -framework AudioUnit -framework CoreFoundation -framework AudioToolbox -framework Accelerate")

  set(iflamapp_xibs "en.lproj/MainMenu")

  FIND_PROGRAM( IBTOOL ibtool HINTS "/usr/bin" "${OSX_DEVELOPER_ROOT}/usr/bin" )
  if ( ${IBTOOL} STREQUAL "IBTOOL-NOTFOUND" )
      MESSAGE( SEND_ERROR "ibtool can not be found" )
  ENDIF()

  foreach(xib ${iflamapp_xibs})
    set(_nib_file ${CMAKE_CURRENT_BINARY_DIR}/${CMAKE_CFG_INTDIR}/iflamapp.app/Contents/Resources/${xib}.nib)
    # TODO: add DEPENDS section
    # TODO: extract
    add_custom_command (TARGET iflamapp POST_BUILD
      COMMAND mkdir -p `dirname ${_nib_file}` &&
      ${IBTOOL} --errors --warnings --notices --output-format human-readable-text
      --compile ${_nib_file}
      ${CMAKE_CURRENT_SOURCE_DIR}/${xib}.xib
      COMMENT "Compiling ${CMAKE_CURRENT_SOURCE_DIR}/${xib}.xib")

  endforeach()
endif()
