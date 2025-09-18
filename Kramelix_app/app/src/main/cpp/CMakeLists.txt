cmake_minimum_required(VERSION 3.22)
project(whisper_android)
set(CMAKE_CXX_STANDARD 17)

# Where whisper.cpp lives (vendored)
set(WHISPER_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/whisper.cpp)
message(STATUS "Using WHISPER_CPP_DIR = ${WHISPER_CPP_DIR}")

# Build whisper.cpp as a subproject
add_subdirectory(${WHISPER_CPP_DIR} ${CMAKE_BINARY_DIR}/whisper_build)

# Our JNI shim
add_library(whisper_jni SHARED whisper_jni.cpp)

# Android log library
find_library(log-lib log)

# Include paths so "whisper.h" resolves
target_include_directories(whisper_jni PRIVATE
        ${WHISPER_CPP_DIR}
        ${WHISPER_CPP_DIR}/include
)

# Link against whisper (exported by the subproject) + log
target_link_libraries(whisper_jni PRIVATE whisper ${log-lib})

# Some builds need this for file I/O functions on Android
if (ANDROID)
    target_compile_definitions(whisper_jni PRIVATE -DGGML_USE_CPU)
endif()
