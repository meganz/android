# Set the minimum required version of CMake
cmake_minimum_required(VERSION 3.18)

# Set the project name and language
project(jniPdfium LANGUAGES CXX)

message(STATUS "ANDROID_NDK: ${ANDROID_NDK}")
message(STATUS "CMAKE_ANDROID_NDK: ${CMAKE_ANDROID_NDK}")
message(STATUS "CMAKE_SYSTEM_NAME: ${CMAKE_SYSTEM_NAME}")
message(STATUS "CMAKE_ANDROID_ARCH_ABI: ${CMAKE_ANDROID_ARCH_ABI}")
message(STATUS "ANDROID_ABI: ${ANDROID_ABI}")
message(STATUS "ANDROID_PLATFORM: ${ANDROID_PLATFORM}")


# Add the source files for the library
set(SOURCES
        src/main/jni/src/mainJNILib.cpp
        src/main/jni/src/util.hpp
)

# Create the shared library target
add_library(jniPdfium SHARED ${SOURCES})

# Specify include directories if necessary
target_include_directories(jniPdfium
        PRIVATE ${CMAKE_SOURCE_DIR}/src/main/jni/include
)

# Specify the dependent dynamic libraries to link against
target_link_libraries(jniPdfium
        PRIVATE ${CMAKE_SOURCE_DIR}/src/main/jni/lib/${ANDROID_ABI}/libmodft2.so
        PRIVATE ${CMAKE_SOURCE_DIR}/src/main/jni/lib/${ANDROID_ABI}/libmodpdfium.so
        PRIVATE ${CMAKE_SOURCE_DIR}/src/main/jni/lib/${ANDROID_ABI}/libmodpng.so
        PRIVATE ${CMAKE_SOURCE_DIR}/src/main/jni/lib/${ANDROID_ABI}/libc++_shared.so
)

# Specify additional compile options if required
target_compile_options(jniPdfium PRIVATE
        -Wall -Wextra -DHAVE_PTHREADS
)

# Specify additional linker flags if necessary
target_link_options(jniPdfium PRIVATE
        -Wl,--no-undefined -llog -landroid -ljnigraphics
)
