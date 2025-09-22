# AMY'S NOTE: Gradle needs this file to compile the native/NDK code (C/C++) we use.
# This file ensures that CMake can read (1) our source code, aka our JNI whisper_jni.cpp
# and the vendored whisper.cpp repo and (2) what to build in order for Java to load,
# aka the shared library libwhisper_jni.so.

cmake_minimum_required(VERSION 3.22) # pinning the minimum CMake ver. required for our build
project(whisper_android) # naming the native project for logs/IDE metadata
set(CMAKE_CXX_STANDARD 17) # declaring C++ bc whisper.cpp expects >= C++ 17

set(WHISPER_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/whisper.cpp) # locating whisper.cpp
message(STATUS "Using WHISPER_CPP_DIR = ${WHISPER_CPP_DIR}") # printing repo path for logs

add_subdirectory(${WHISPER_CPP_DIR} ${CMAKE_BINARY_DIR}/whisper_build) # targeting whisper.cpp to build it as a subproject
add_library(whisper_jni SHARED whisper_jni.cpp) # loading our JNI bridge as a shared lib.
find_library(log-lib log) # finding Android log lib for debugging C/C++ later, if needed

target_include_directories(whisper_jni PRIVATE
        ${WHISPER_CPP_DIR}
        ${WHISPER_CPP_DIR}/include
) # telling JNI target where to find `whisper.h` header
target_link_libraries(whisper_jni PRIVATE whisper ${log-lib}) # connecting whisper to our JNI bridge to Android's log lib.

# AMY'S NOTE: Our app is currently built to run on the phone's CPU, and in the whisper.cpp repo,
# certain code is flagged to run differently, based on whether the integrated app is going to run on
# CPU or GPU or smth else. For our app, we're explicitly declaring below that any of their code that's
# flagged for CPU purposes should run.
# FIXME OPTIMIZE: we might consider changing this later if we ever switch to smth else for our app to run faster
if (ANDROID)
    target_compile_definitions(whisper_jni PRIVATE -DGGML_USE_CPU)
endif()
