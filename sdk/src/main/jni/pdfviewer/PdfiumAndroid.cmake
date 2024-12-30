# build PdfiumAndroid
include(ExternalProject)

# Variables
set(SUBPROJECT_REPO "https://github.com/barteksc/PdfiumAndroid.git")
set(SUBPROJECT_TAG "pdfium-android-1.9.0")
set(CUSTOM_CMAKE_SOURCE ${CMAKE_SOURCE_DIR}/pdfviewer/CMakeLists.txt)
set(SUBPROJECT_SOURCE_DIR ${CMAKE_BINARY_DIR}/pdfium_download/src/PdfiumAndroid)
set(BUILD_DIR ${CMAKE_BINARY_DIR}/builddir)
set(LIB_OUTPUT_DIR ${CMAKE_SOURCE_DIR}/../jniLibs)

message(STATUS "BUILD_DIR: ${BUILD_DIR}")
message(STATUS "LIB_OUTPUT_DIR: ${LIB_OUTPUT_DIR}")
message(STATUS "ANDROID_ABI: ${ANDROID_ABI}")

# Ensure the output directories exist
file(MAKE_DIRECTORY ${LIB_OUTPUT_DIR})
file(MAKE_DIRECTORY ${LIB_OUTPUT_DIR}/${ANDROID_ABI})

# Step 1: Clone the sub-project and checkout the desired tag
ExternalProject_Add(PdfiumAndroid
        PREFIX ${CMAKE_BINARY_DIR}/pdfium_download
        GIT_REPOSITORY ${SUBPROJECT_REPO}
        GIT_TAG ${SUBPROJECT_TAG}

        # Step 2: Copy custom CMakeLists.txt into the cloned source
        CONFIGURE_COMMAND ${CMAKE_COMMAND} -E copy ${CUSTOM_CMAKE_SOURCE} ${SUBPROJECT_SOURCE_DIR}/CMakeLists.txt
        && ${CMAKE_COMMAND} -B ${BUILD_DIR}
        -S ${SUBPROJECT_SOURCE_DIR}
        -DCMAKE_BUILD_TYPE=Release
        -DANDROID_NDK=$ENV{NDK_ROOT}
        -DCMAKE_ANDROID_NDK=$ENV{NDK_ROOT}
        -DCMAKE_SYSTEM_NAME=Android
        -DCMAKE_ANDROID_ARCH_ABI=${ANDROID_ABI}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=android-26


        # Step 3: Build the project
        BUILD_COMMAND ${CMAKE_COMMAND} --build ${BUILD_DIR}

        INSTALL_COMMAND ${CMAKE_COMMAND} -E copy ${BUILD_DIR}/libjniPdfium.so ${LIB_OUTPUT_DIR}/${ANDROID_ABI}/
        && ${CMAKE_COMMAND} -E copy ${SUBPROJECT_SOURCE_DIR}/src/main/jni/lib/${ANDROID_ABI}/libc++_shared.so ${LIB_OUTPUT_DIR}/${ANDROID_ABI}/libc++_shared.so
        && ${CMAKE_COMMAND} -E copy ${SUBPROJECT_SOURCE_DIR}/src/main/jni/lib/${ANDROID_ABI}/libmodft2.so ${LIB_OUTPUT_DIR}/${ANDROID_ABI}/libmodft2.so
        && ${CMAKE_COMMAND} -E copy ${SUBPROJECT_SOURCE_DIR}/src/main/jni/lib/${ANDROID_ABI}/libmodpdfium.so ${LIB_OUTPUT_DIR}/${ANDROID_ABI}/libmodpdfium.so
        && ${CMAKE_COMMAND} -E copy ${SUBPROJECT_SOURCE_DIR}/src/main/jni/lib/${ANDROID_ABI}/libmodpng.so ${LIB_OUTPUT_DIR}/${ANDROID_ABI}/libmodpng.so
        TEST_COMMAND ""
)
