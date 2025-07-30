#!/bin/bash -i
set -e

start_time=$SECONDS

export ANDROID_API_LEVEL=26
export ANDROID_NDK_HOME=${NDK_ROOT}

if [[ "$OSTYPE" == "darwin"* ]]; then
    export STRIP_TOOL=${NDK_ROOT}/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-strip
else
    export STRIP_TOOL=${NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip
fi

# Check if NDK_ROOT is set.
if [ -z "${NDK_ROOT}" ]; then
    echo "NDK_ROOT is not set. Exiting."
    exit 1
fi

# LIST OF ARCHS TO BE BUILT.
if [ -z "${BUILD_ARCHS}" ]; then
    # If no environment variable is defined, use all archs.
    BUILD_ARCHS="x86 armeabi-v7a x86_64 arm64-v8a"
fi

if [ -z "${LOG_FILE}" ]; then
    # If no build log variable is defined, use below value.
    LOG_FILE=/dev/null # Ensure you use a full path
fi

# Check if VCPKG_ROOT is set.
if [ -z "${VCPKG_ROOT}" ]; then
    echo "VCPKG_ROOT is not set. Exiting."
    echo "Please run 'git clone https://github.com/microsoft/vcpkg.git' and set VCPKG_ROOT to the path of the cloned repository."
    echo "VCPKG is suggested to be installed out of the project directory."
    exit 1
else
  if [ ! -f "${VCPKG_ROOT}/bootstrap-vcpkg.sh" ]; then
      echo "vcpkg installation not found. Exiting."
      echo "Please ensure you have cloned the vcpkg repository('git clone https://github.com/microsoft/vcpkg.git')."
      echo "VCPKG is suggested to be installed out of the project directory."
      exit 1
  fi
fi

if [[ "$OSTYPE" == "darwin"* ]]; then
    JOBS=$(sysctl -n hw.ncpu)
else
    JOBS=$(nproc)
fi
echo "-- JOBS: ${JOBS}"

echo "-- BUILD_ARCHS: ${BUILD_ARCHS}"
echo "-- VCPKG_ROOT: ${VCPKG_ROOT}"
echo "-- ANDROID_NDK_HOME: ${ANDROID_NDK_HOME}"

for ABI in ${BUILD_ARCHS}; do
    export BUILD_DIR=build-dir/pdfium/${ABI}

    case ${ABI} in
      armeabi-v7a)
        export VCPKG_TRIPLET='arm-android-mega'
        ;;
      arm64-v8a)
        export VCPKG_TRIPLET='arm64-android-mega'
        ;;
      x86)
        export VCPKG_TRIPLET='x86-android-mega'
        ;;
      x86_64)
        export VCPKG_TRIPLET='x64-android-mega'
        ;;
      *)
        echo "Unsupported architecture: ${ABI}" && exit 1
        ;;
    esac

    echo "* Configuring PdfiumAndroid - ${ABI}"
    cmake -B ${BUILD_DIR} \
         -S . \
         -DCMAKE_BUILD_TYPE=RelWithDebInfo \
         -DANDROID_NDK=${NDK_ROOT} \
         -DCMAKE_ANDROID_NDK=${NDK_ROOT} \
         -DCMAKE_SYSTEM_NAME=Android \
         -DCMAKE_ANDROID_ARCH_ABI=${ABI} \
         -DANDROID_ABI=${ABI} \
         -DANDROID_PLATFORM=android-${ANDROID_API_LEVEL} \
         -DVCPKG_ROOT=${VCPKG_ROOT} \
         -DVCPKG_TARGET_TRIPLET=${VCPKG_TRIPLET} \
         -DCMAKE_ANDROID_API=${ANDROID_API_LEVEL} \
         -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON  &>> ${LOG_FILE}
    echo "* Building PdfiumAndroid - ${ABI}"
    cmake --build ${BUILD_DIR} -j "${JOBS}" &>> ${LOG_FILE}
done

for ABI in ${BUILD_ARCHS}; do

    export BUILD_DIR=build-dir/sdk/${ABI}
    export LIB_OUTPUT_DIR=../jniLibs/${ABI}

    case ${ABI} in
      armeabi-v7a)
        export VCPKG_TRIPLET='arm-android-mega'
        ;;
      arm64-v8a)
        export VCPKG_TRIPLET='arm64-android-mega'
        ;;
      x86)
        export VCPKG_TRIPLET='x86-android-mega'
        ;;
      x86_64)
        export VCPKG_TRIPLET='x64-android-mega'
        ;;
      *)
        echo "Unsupported architecture: ${ABI}" && exit 1
        ;;
    esac

    echo "* Configuring MEGA SDK - ${ABI}"
    cmake --preset mega-android \
        -B ${BUILD_DIR} \
        -S mega/sdk \
        -DCMAKE_BUILD_TYPE=RelWithDebInfo \
        -DBUILD_SHARED_LIBS=ON \
        -DANDROID_NDK=${NDK_ROOT} \
        -DCMAKE_ANDROID_NDK=${NDK_ROOT} \
        -DCMAKE_SYSTEM_NAME=Android \
        -DCMAKE_ANDROID_ARCH_ABI=${ABI} \
        -DANDROID_ABI=${ABI} \
        -DANDROID_PLATFORM=android-${ANDROID_API_LEVEL} \
        -DVCPKG_ROOT=${VCPKG_ROOT} \
        -DVCPKG_TARGET_TRIPLET=${VCPKG_TRIPLET} \
        -DCMAKE_ANDROID_API=${ANDROID_API_LEVEL} &>> ${LOG_FILE}
    echo "* Building MEGA SDK - ${ABI}"
    cmake --build "${BUILD_DIR}" -j "${JOBS}" &>> ${LOG_FILE}

    cp -fv ${BUILD_DIR}/libSDKlib.so ${LIB_OUTPUT_DIR}  &>> ${LOG_FILE}
    cp -fv ${BUILD_DIR}/bindings/java/libSDKJavaBindings.so ${LIB_OUTPUT_DIR}  &>> ${LOG_FILE}
    cp -frv ${BUILD_DIR}/bindings/java/nz/mega/sdk/*.java ../java/nz/mega/sdk  &>> ${LOG_FILE}

    echo "* Stripping debug symbols from libraries - ${ABI}"
    ${STRIP_TOOL} "../jniLibs/${ABI}/libSDKJavaBindings.so" &>> ${LOG_FILE}
    ${STRIP_TOOL} "../jniLibs/${ABI}/libSDKlib.so" &>> ${LOG_FILE}
done

for ABI in ${BUILD_ARCHS}; do

    export BUILD_DIR=build-dir/chat/${ABI}
    export LIB_OUTPUT_DIR=../jniLibs/${ABI}

    case ${ABI} in
      armeabi-v7a)
        export VCPKG_TRIPLET='arm-android-mega'
        ;;
      arm64-v8a)
        export VCPKG_TRIPLET='arm64-android-mega'
        ;;
      x86)
        export VCPKG_TRIPLET='x86-android-mega'
        ;;
      x86_64)
        export VCPKG_TRIPLET='x64-android-mega'
        ;;
      *)
        echo "Unsupported architecture: ${ABI}" && exit 1
        ;;
    esac

    echo "* Configuring MEGA Chat SDK - ${ABI}"

    cmake -S megachat/sdk --preset mega-android \
        -B ${BUILD_DIR} \
        -DSDK_DIR=mega/sdk \
        -DCMAKE_BUILD_TYPE=RelWithDebInfo \
        -DBUILD_SHARED_LIBS=ON \
        -DANDROID_NDK=${NDK_ROOT} \
        -DCMAKE_ANDROID_NDK=${NDK_ROOT} \
        -DCMAKE_SYSTEM_NAME=Android \
        -DCMAKE_ANDROID_ARCH_ABI=${ABI} \
        -DUSE_WEBRTC=ON \
        -DANDROID_ABI=${ABI} \
        -DANDROID_PLATFORM=android-${ANDROID_API_LEVEL} \
        -DVCPKG_ROOT=${VCPKG_ROOT} \
        -DVCPKG_TARGET_TRIPLET=${VCPKG_TRIPLET} \
        -DCMAKE_ANDROID_API=${ANDROID_API_LEVEL} &>> ${LOG_FILE}

    echo "* Building MEGA Chat SDK - ${ABI}"
    cmake --build "${BUILD_DIR}" -j "${JOBS}" &>> ${LOG_FILE}

    cp -fv ${BUILD_DIR}/src/libCHATlib.so ${LIB_OUTPUT_DIR} &>> ${LOG_FILE}
    cp -fv ${BUILD_DIR}/bindings/java/libMEGAchatJavaBindings.so ${LIB_OUTPUT_DIR} &>> ${LOG_FILE}
    cp -fv ${BUILD_DIR}/bindings/java/nz/mega/sdk/*.java ../java/nz/mega/sdk &>> ${LOG_FILE}

    ${STRIP_TOOL} "../jniLibs/${ABI}/libCHATlib.so" &>> ${LOG_FILE}
    ${STRIP_TOOL} "../jniLibs/${ABI}/libMEGAchatJavaBindings.so" &>> ${LOG_FILE}
done

end_time=$SECONDS
total_time=$((end_time - start_time))
echo "* Task finished OK"
echo "Total execution time: ${total_time} seconds"
