#!/bin/bash -i
set -e

start_time=$SECONDS

ANDROID_API_LEVEL=26
export ANDROID_NDK_HOME=${NDK_ROOT}

if [[ "$OSTYPE" == "darwin"* ]]; then
    STRIP_TOOL=${NDK_ROOT}/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-strip
else
    STRIP_TOOL=${NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip
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
    LOG_FILE=/dev/stdout # Ensure you use a full path
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

    BUILD_DIR=build-dir/chat/${ABI}
    LIB_OUTPUT_DIR=../jniLibs/${ABI}
    mkdir -p "${LIB_OUTPUT_DIR}"

    echo "* Configuring MEGA Chat SDK - ${ABI}"

    cmake -S megachat/sdk --preset mega-android \
        -B ${BUILD_DIR} \
        -DSDK_DIR=mega/sdk \
        -DCMAKE_BUILD_TYPE=RelWithDebInfo \
        -DCMAKE_ANDROID_NDK=${NDK_ROOT} \
        -DCMAKE_ANDROID_ARCH_ABI=${ABI} \
        -DVCPKG_ROOT=${VCPKG_ROOT} \
        -DCMAKE_ANDROID_API=${ANDROID_API_LEVEL} \
        -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
        -DUSE_WEBRTC=ON &>>${LOG_FILE}

    echo "* Building MEGA Chat SDK - ${ABI}"
    cmake --build "${BUILD_DIR}" -j "${JOBS}" &>>${LOG_FILE}

    cp -fv ${BUILD_DIR}/bindings/java/libmega.so ${LIB_OUTPUT_DIR} &>>${LOG_FILE}
    cp -fv ${BUILD_DIR}/bindings/java/nz/mega/sdk/*.java ../java/nz/mega/sdk &>>${LOG_FILE}

    mkdir -p megachat/webrtc
    cp -fv `find ${BUILD_DIR} -name "libwebrtc.jar"` megachat/webrtc/ &>> ${LOG_FILE}

    ${STRIP_TOOL} "../jniLibs/${ABI}/libmega.so" &>> ${LOG_FILE}

done

end_time=$SECONDS
total_time=$((end_time - start_time))
echo "* Task finished OK"
echo "Total execution time: ${total_time} seconds"
