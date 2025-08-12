#!/bin/bash -i
set -e

if [ ${BASH_VERSINFO:-0} -lt 4 ] ; then
    echo
    echo "Bash 4.0 or higher is required to run this script."
    echo
    exit 1
fi

##############################################################
# SET THE PATH TO YOUR ANDROID NDK, SDK and JAVA DIRECTORIES #
##############################################################
if [ -z "$NDK_ROOT" ]; then
    NDK_ROOT=${HOME}/android-ndk
fi
if [ -z "$ANDROID_HOME" ]; then
    export ANDROID_HOME=${HOME}/android-sdk
fi
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=${HOME}/android-java
fi
##################################################
##################################################
# LIST OF ARCHS TO BE BUILT.
if [ -z "${BUILD_ARCHS}" ]; then
    # If no environment variable is defined, use all archs.
    BUILD_ARCHS="x86 armeabi-v7a x86_64 arm64-v8a"
fi
##################################################
if [ ! -d "${NDK_ROOT}" ]; then
    echo "* NDK_ROOT not set. Please download NDK 21 and export NDK_ROOT variable or create a link at ${HOME}/android-ndk to point to your Android NDK installation path and try again."
    exit 1
fi
if [ ! -d "${ANDROID_HOME}" ]; then
    echo "* ANDROID_HOME not set. Please download Android SDK and export ANDROID_HOME variable or create a link at ${HOME}/android-sdk to point to your Android SDK installation path and try again."
    exit 1
fi
if [ ! -d "${JAVA_HOME}" ]; then
    echo "* JAVA_HOME not set. Please download JDK and export JAVA_HOME variable or create a link at ${HOME}/android-jdk to point to your JDK installation path and try again."
    exit 1
fi

#This is only for support to build using Mac with Apple Silicon. Please remove this once NDK provides support to M1.
if [[ `uname -m` == 'arm64' ]]; then
    NDK_BUILD="arch -x86_64 ${NDK_ROOT}/ndk-build"
else
    NDK_BUILD=${NDK_ROOT}/ndk-build
fi

BASE_PATH=`pwd`
LIBDIR=${BASE_PATH}/../obj/local/armeabi
TARGET_LIB_DIR=../jniLibs
JAVA_OUTPUT_PATH=${BASE_PATH}/../java
APP_PLATFORM=`grep APP_PLATFORM Application.mk | cut -d '=' -f 2`
API_LEVEL=`echo ${APP_PLATFORM} | cut -d'-' -f2`
if [[ "$OSTYPE" == "darwin"* ]]; then
    JOBS=$(sysctl -n hw.ncpu)
else
    JOBS=$(nproc)
fi

if [ -z "${LOG_FILE}" ]; then
    # If no build log variable is defined, use below value.
    LOG_FILE=/dev/null # Ensure you use a full path
fi

CRYPTOPP=cryptopp
CRYPTOPP_VERSION=820
CRYPTOPP_VERSION2=$(echo "$CRYPTOPP_VERSION" | sed 's/\(.\)/_\1/g')  # convert version "820" to "_8_2_0"
CRYPTOPP_SOURCE_FILE=cryptopp${CRYPTOPP_VERSION}.zip
CRYPTOPP_SOURCE_FOLDER=${CRYPTOPP}/${CRYPTOPP}
CRYPTOPP_DOWNLOAD_URL=https://github.com/weidai11/cryptopp/releases/download/CRYPTOPP${CRYPTOPP_VERSION2}/${CRYPTOPP_SOURCE_FILE}
CRYPTOPP_SHA1="b042d2f0c93410abdec7c12bcd92787d019f8da1"

SQLITE=sqlite
SQLITE_VERSION=3380500
SQLITE_YEAR=2022
SQLITE_BASE_NAME=sqlite-amalgamation-${SQLITE_VERSION}
SQLITE_SOURCE_FILE=${SQLITE_BASE_NAME}.zip
SQLITE_SOURCE_FOLDER=${SQLITE}/${SQLITE}
SQLITE_DOWNLOAD_URL=https://www.sqlite.org/${SQLITE_YEAR}/${SQLITE_SOURCE_FILE}
SQLITE_SHA1="350fa5ccedc70f4979d7f954fba9525542809ba2"

CURL=curl
CURL_VERSION=8.1.1
CURL_EXTRA="--disable-dict --disable-file --disable-ftp --disable-gopher --disable-imap --disable-ldap --disable-ldaps --disable-mime --disable-netrc --disable-pop3 --disable-proxy --disable-rtsp --disable-smb --disable-smtp --disable-telnet --disable-tftp --disable-manual"
CURL_SOURCE_FILE=curl-${CURL_VERSION}.tar.gz
CURL_SOURCE_FOLDER=curl-${CURL_VERSION}
CURL_DOWNLOAD_URL=http://curl.haxx.se/download/${CURL_SOURCE_FILE}
CURL_SHA1="5ff2ecaa4a68ecc06434644ce76d9837e99e7d1d"

C_ARES_VERSION=1.19.1
C_ARES_VERSION2=1_19_1
ARES_SOURCE_FILE=c-ares-${C_ARES_VERSION}.tar.gz
ARES_SOURCE_FOLDER=c-ares-${C_ARES_VERSION}
ARES_CONFIGURED=${CURL}/${ARES_SOURCE_FOLDER}/Makefile.inc
ARES_DOWNLOAD_URL=https://github.com/c-ares/c-ares/releases/download/cares-${C_ARES_VERSION2}/${ARES_SOURCE_FILE}
ARES_SHA1="99566278e4ed4b261891aa62c8b88227bf1a2823"

ZSTD_VERSION=1.5.7
ZSTD_SOURCE_FILE=zstd-${ZSTD_VERSION}.tar.gz
ZSTD_SOURCE_FOLDER=zstd-${ZSTD_VERSION}
ZSTD_DOWNLOAD_URL=https://github.com/facebook/zstd/releases/download/v${ZSTD_VERSION}/${ZSTD_SOURCE_FILE}
ZSTD_SHA1="6c7dd9c829561ac5475b72592a079a01ab7f3eab"

CRASHLYTICS=crashlytics
CRASHLYTICS_DOWNLOAD_URL=https://raw.githubusercontent.com/firebase/firebase-android-sdk/master/firebase-crashlytics-ndk/src/main/jni/libcrashlytics/include/crashlytics/external/crashlytics.h
CRASHLYTICS_DOWNLOAD_URL_C=https://raw.githubusercontent.com/firebase/firebase-android-sdk/8f02834e94f8b24a7cf0f777562cad73c6b9a40f/firebase-crashlytics-ndk/src/main/jni/libcrashlytics/include/crashlytics/external/crashlytics.h
CRASHLYTICS_SOURCE_FILE=crashlytics.h
CRASHLYTICS_SOURCE_FILE_C=crashlyticsC.h
CRASHLYTICS_DEST_PATH=mega/sdk/third_party

SODIUM=sodium
SODIUM_VERSION=1.0.18
SODIUM_SOURCE_FILE=libsodium-${SODIUM_VERSION}.tar.gz
SODIUM_SOURCE_FOLDER=libsodium-${SODIUM_VERSION}
SODIUM_DOWNLOAD_URL=https://download.libsodium.org/libsodium/releases/${SODIUM_SOURCE_FILE}
SODIUM_SHA1="795b73e3f92a362fabee238a71735579bf46bb97"

LIBUV=libuv
LIBUV_VERSION=1.42.0
LIBUV_SOURCE_FILE=libuv-${LIBUV_VERSION}.tar.gz
LIBUV_SOURCE_FOLDER=libuv-${LIBUV_VERSION}
LIBUV_DOWNLOAD_URL=https://github.com/libuv/libuv/archive/refs/tags/v${LIBUV_VERSION}.tar.gz
LIBUV_SHA1="d1750da0846b91289bcd4f0128ebcaea6c70a272"

MEDIAINFO=mediainfo
MEDIAINFO_VERSION=4ee7f77c087b29055f48d539cd679de8de6f9c48
MEDIAINFO_SOURCE_FILE=${MEDIAINFO_VERSION}.zip
MEDIAINFO_SOURCE_FOLDER=MediaInfoLib-${MEDIAINFO_VERSION}
MEDIAINFO_DOWNLOAD_URL=https://github.com/meganz/MediaInfoLib/archive/${MEDIAINFO_SOURCE_FILE}
MEDIAINFO_SHA1="30927c761418e807d8d3b64e171a6c9ab9659c2e"

ZENLIB=ZenLib
ZENLIB_VERSION=6694a744d82d942c4a410f25f916561270381889
ZENLIB_SOURCE_FILE=${ZENLIB_VERSION}.zip
ZENLIB_SOURCE_FOLDER=ZenLib-${ZENLIB_VERSION}
ZENLIB_DOWNLOAD_URL=https://github.com/MediaArea/ZenLib/archive/${ZENLIB_SOURCE_FILE}
ZENLIB_SHA1="1af04654c9618f54ece624a0bad881a3cfef3692"

LIBWEBSOCKETS=libwebsockets
LIBWEBSOCKETS_VERSION=4.2-stable
LIBWEBSOCKETS_SOURCE_FILE=libwebsockets-v${LIBWEBSOCKETS_VERSION}.zip
LIBWEBSOCKETS_SOURCE_FOLDER=libwebsockets-${LIBWEBSOCKETS_VERSION}
LIBWEBSOCKETS_DOWNLOAD_URL=https://github.com/warmcat/libwebsockets/archive/refs/heads/v${LIBWEBSOCKETS_VERSION}.zip

ICU=icu
ICU_VERSION=71_1
ICU_SOURCE_FILE=icu4c-${ICU_VERSION}.zip
ICU_SOURCE_FOLDER=icu-${ICU_VERSION}
ICU_DOWNLOAD_URL=https://github.com/unicode-org/icu/releases/download/release-71-1/icu4c-71_1-src.zip
ICU_SHA1="0b6a02293a81ccfb2a743ce1faa009770ed8a12c"
ICU_SOURCE_VERSION=icuSource-${ICU_VERSION}

WEBRTC_DOWNLOAD_URL=https://mega.nz/file/N2k2XRaA#bS9iudrjiULmMaGbBKErsYosELbnU22b8Zj213Ti1nE
# Expected SHA1 checksum of "megachat/webrtc/libwebrtc_arm64.a" in downloaded WebRTC library
WEBRTC_SHA1=c4442ce8bf7567d3980e14941a313311cf0483cb

function setupEnv()
{
    local ABI="${1}"

    if [[ "$OSTYPE" == "darwin"* ]]; then
        local TOOLCHAIN="${NDK_ROOT}/toolchains/llvm/prebuilt/darwin-x86_64"
    else
        local TOOLCHAIN="${NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64"
    fi
    export AR=$TOOLCHAIN/bin/llvm-ar
    export LD=$TOOLCHAIN/bin/ld
    export RANLIB=$TOOLCHAIN/bin/llvm-ranlib
    export STRIP=$TOOLCHAIN/bin/llvm-strip

    if [ "${ABI}" == "armeabi-v7a" ]; then
        export TARGET_HOST="armv7a-linux-androideabi"
    elif [ "${ABI}" == "arm64-v8a" ]; then
        export TARGET_HOST="aarch64-linux-android"
    elif [ "${ABI}" == "x86" ]; then
        export TARGET_HOST="i686-linux-android"
    elif [ "${ABI}" == "x86_64" ]; then
        export TARGET_HOST="x86_64-linux-android"
    fi

    export CC=$TOOLCHAIN/bin/${TARGET_HOST}${API_LEVEL}-clang
    export AS=$CC
    export CXX=$TOOLCHAIN/bin/${TARGET_HOST}${API_LEVEL}-clang++
}

function cleanEnv()
{
    unset AR
    unset LD
    unset RANLIB
    unset STRIP
    unset TARGET_HOST
    unset CC
    unset AS
    unset CXX
}

function downloadCheckAndUnpack()
{
    local URL=$1
    local FILENAME=$2
    local SHA1=$3
    local TARGETPATH=$4
    
    if [[ -f ${FILENAME} ]]; then
        echo "* Already downloaded: '${FILENAME}'"
        local CURRENTSHA1=`sha1sum ${FILENAME} | cut -d " " -f 1`
        if [ "${SHA1}" != "${CURRENTSHA1}" ]; then
            echo "* Invalid hash. Redownloading..."
            # if wget fails, try with curl
            wget --no-check-certificate -O ${FILENAME} ${URL} &>> ${LOG_FILE} || \
                curl -L -o ${FILENAME} ${URL} &>> ${LOG_FILE}
        fi
    else
        echo "* Downloading '${FILENAME}' ..."
        # if wget fails, try with curl
        wget --no-check-certificate -O ${FILENAME} ${URL} &>> ${LOG_FILE} || \
            curl -L -o ${FILENAME} ${URL} &>> ${LOG_FILE}
    fi

    local NEWSHA1=`sha1sum ${FILENAME} | cut -d " " -f 1`
    if [ "${SHA1}" != "${NEWSHA1}" ]; then
        echo "* Invalid hash. It is ${NEWSHA1} but it should be ${SHA1}. Aborting..."
        exit 1
    fi

    if [[ "${FILENAME}" =~ \.tar\.[^\.]+$ ]]; then
        echo "* Extracting TAR file..."
        tar --overwrite -xf ${FILENAME} -C ${TARGETPATH} &>> ${LOG_FILE}
    elif [[ "${FILENAME}" =~ \.zip$ ]]; then
        echo "* Extracting ZIP file..."
    	unzip -o ${FILENAME} -d ${TARGETPATH} &>> ${LOG_FILE}
    else
        echo "* Dont know how to extract '${FILENAME}'"
        exit 1
    fi

    echo "* Extraction finished"
}

function downloadAndUnpack()
{
    local URL=$1
    local FILENAME=$2
    local TARGETPATH=$3

    if [[ -f ${FILENAME} ]]; then
        echo "* Already downloaded: '${FILENAME}'"
    else
        echo "* Downloading '${FILENAME}' ..."
        wget --no-check-certificate -O ${FILENAME} ${URL} &>> ${LOG_FILE} || \
            curl -L -o ${FILENAME} ${URL} &>> ${LOG_FILE}
    fi

    if [[ "${FILENAME}" =~ \.tar\.[^\.]+$ ]]; then
        echo "* Extracting TAR file..."
        tar --overwrite -xf ${FILENAME} -C ${TARGETPATH} &>> ${LOG_FILE}
    elif [[ "${FILENAME}" =~ \.zip$ ]]; then
        echo "* Extracting ZIP file..."
        unzip -o ${FILENAME} -d ${TARGETPATH} &>> ${LOG_FILE}
    else
        echo "* Dont know how to extract '${FILENAME}'"
        exit 1
    fi

    echo "* Extraction finished"
}

function createMEGABindings
{
    echo "* Creating MEGA Java bindings"
    mkdir -p ../java/nz/mega/sdk
    swig -c++ -Imega/sdk/include -java -package nz.mega.sdk -outdir ${JAVA_OUTPUT_PATH}/nz/mega/sdk -o bindings/megasdk.cpp -DHAVE_LIBUV -DENABLE_SYNC mega/sdk/bindings/megaapi.i &>> ${LOG_FILE}
}

function createMEGAchatBindings
{
    echo "* Creating MEGAchat Java bindings"
    mkdir -p ../java/nz/mega/sdk
    swig -c++ -Imega/sdk/include -Imegachat/sdk/src/ -java -package nz.mega.sdk -outdir ${JAVA_OUTPUT_PATH}/nz/mega/sdk/ -o bindings/megachat.cpp megachat/sdk/bindings/megachatapi.i &>> ${LOG_FILE}
    pushd megachat/sdk/src &>> ${LOG_FILE}
    cmake -P genDbSchema.cmake
    popd &>> ${LOG_FILE}
}

# There is a random issue with the arm64-v8a target that sometimes the file is not generated correctly.
# This function checks if the file is smaller than 100 bytes and re-generates it if needed.
# This is a workaround until the issue is fixed.
function ensureArm64TargetExists {
  echo "* Ensuring arm64-v8a target exists" &>>${LOG_FILE}
  echo sync  &>>${LOG_FILE}
  sync

  local TARGET_FILE=${TARGET_LIB_DIR}/arm64-v8a/libmega.so
  local SOURCE_FILE=../obj/local/arm64-v8a/libmega.so

  # Get file size in bytes (works on both macOS and Linux)
  if [[ "$OSTYPE" == "darwin"* ]]; then
    FILE_SIZE=$(stat -f %z "$TARGET_FILE")
  else
    FILE_SIZE=$(stat -c %s "$TARGET_FILE")
  fi

  echo ls -l ${TARGET_FILE} &>>${LOG_FILE}
  ls -l ${TARGET_FILE} &>>${LOG_FILE}

  if [ "$FILE_SIZE" -le 100 ]; then
      echo "File ${TARGET_FILE} is smaller than 100 bytes. Re-generating..." &>>${LOG_FILE}

      echo file ${SOURCE_FILE} &>>${LOG_FILE}
      file ${SOURCE_FILE} &>>${LOG_FILE}

      echo sha1sum ${SOURCE_FILE} &>>${LOG_FILE}
      sha1sum ${SOURCE_FILE} &>>${LOG_FILE}

      if [[ "$OSTYPE" == "darwin"* ]]; then
        LLVM_STRIP=${NDK_ROOT}/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-strip
      else
        LLVM_STRIP=${NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip
      fi

      echo ${LLVM_STRIP} --strip-unneeded -o ${TARGET_FILE} ${SOURCE_FILE} &>>${LOG_FILE}
      ${LLVM_STRIP} --strip-unneeded -o ${TARGET_FILE} ${SOURCE_FILE} &>>${LOG_FILE}

      echo ls -l ${TARGET_FILE} &>>${LOG_FILE}
      ls -l ${TARGET_FILE} &>>${LOG_FILE}

      # run file command to check if the file is an ELF 64-bit LSB file
      echo file ${TARGET_FILE} &>>${LOG_FILE}
      output=$(file ${TARGET_FILE})
      echo "$output" &>>${LOG_FILE}

      # if target file is still invalid, exit with failure
      if [[ "$output" != *"ELF 64-bit"* ]]; then
          echo "Error: ${TARGET_FILE} is not an ELF 64-bit LSB file." &>>${LOG_FILE}
          exit 1
      fi
  else
      echo "File ${TARGET_FILE} is larger than 100 bytes. Do nothing." &>>${LOG_FILE}
  fi
}

if (($# != 1)); then
  echo "Usage: $0 <all | bindings | clean | clean_mega>"
  exit 0
fi

if [ "$1" == "bindings" ]; then
    createMEGAchatBindings
    createMEGABindings
    echo "* Bindings ready!"
    echo "* Running ndk-build"
    ${NDK_BUILD} -j${JOBS}
    echo "* ndk-build finished"
    echo "* Task finished OK"
    exit 0
fi

if [ "$1" == "clean_mega" ]; then
    echo "* Deleting Java bindings"
    make -C mega -f MakefileBindings clean JAVA_BASE_OUTPUT_PATH=${JAVA_OUTPUT_PATH} &>> ${LOG_FILE}
    rm -rf ${JAVA_OUTPUT_PATH}/nz/mega/sdk/*.java
    rm -rf megachat/megachat.cpp megachat/megachat.h
    echo "* Deleting tarballs"
    rm -rf ../obj/local/armeabi
    rm -rf ../obj/local/x86
    rm -rf ../obj/local/arm64-v8a
    rm -rf ../obj/local/x86_64
    echo "* Task finished OK"
    exit 0
fi

if [ "$1" == "clean" ]; then
    echo "* Deleting Java bindings"
    make -C mega -f MakefileBindings clean JAVA_BASE_OUTPUT_PATH=${JAVA_OUTPUT_PATH} &>> ${LOG_FILE}
    rm -rf ${JAVA_OUTPUT_PATH}/nz/mega/sdk/*.java
    rm -rf megachat/megachat.cpp megachat/megachat.h
    
    echo "* Deleting source folders"    
    rm -rf ${CRYPTOPP_SOURCE_FOLDER}
    rm -rf ${SQLITE_SOURCE_FOLDER} ${SQLITE}/${SQLITE_BASE_NAME}
    rm -rf ${CURL}/${CURL_SOURCE_FOLDER}
    rm -rf ${CURL}/${CURL}
    rm -rf ${CURL}/${ARES_SOURCE_FOLDER}
    rm -rf ${CURL}/ares
    rm -rf ${CURL}/zstd
    rm -rf ${CRASHLYTICS_DEST_PATH}/${CRASHLYTICS_SOURCE_FILE}
    rm -rf ${CRASHLYTICS_DEST_PATH}/${CRASHLYTICS_SOURCE_FILE_C}
    rm -rf ${SODIUM}/${SODIUM_SOURCE_FOLDER}
    rm -rf ${SODIUM}/${SODIUM}
    rm -rf ${LIBUV}/${LIBUV_SOURCE_FOLDER}
    rm -rf ${LIBUV}/${LIBUV}
    rm -rf ${MEDIAINFO}/${ZENLIB_SOURCE_FOLDER}
    rm -rf ${MEDIAINFO}/${ZENLIB}
    rm -rf ${MEDIAINFO}/${MEDIAINFO_SOURCE_FOLDER}
    rm -rf ${MEDIAINFO}/${MEDIAINFO}
    rm -rf ${LIBWEBSOCKETS}/${LIBWEBSOCKETS_SOURCE_FOLDER}
    rm -rf ${LIBWEBSOCKETS}/${LIBWEBSOCKETS}
    rm -rf ${ICU}/${ICU_SOURCE_VERSION}
    rm -rf ${ICU}/${ICU_SOURCE_FILE}

    echo "* Deleting tarballs"
    rm -rf ${CRYPTOPP}/${CRYPTOPP_SOURCE_FILE}
    rm -rf ${CRYPTOPP}/${CRYPTOPP_SOURCE_FILE}.ready
    rm -rf ${SQLITE}/${SQLITE_SOURCE_FILE}
    rm -rf ${SQLITE}/${SQLITE_SOURCE_FILE}.ready
    rm -rf ${CURL}/${CURL_SOURCE_FILE}
    rm -rf ${CURL}/${ARES_SOURCE_FILE}
    rm -rf ${CURL}/${CURL_SOURCE_FILE}.ready
    rm -rf ${CURL}/${CRASHLYTICS_SOURCE_FILE}.ready
    rm -rf ${SODIUM}/${SODIUM_SOURCE_FILE}
    rm -rf ${SODIUM}/${SODIUM_SOURCE_FILE}.ready
    rm -rf ${LIBUV}/${LIBUV_SOURCE_FILE}
    rm -rf ${LIBUV}/${LIBUV_SOURCE_FILE}.ready
    rm -rf ${MEDIAINFO}/${ZENLIB_SOURCE_FILE}
    rm -rf ${MEDIAINFO}/${ZENLIB_SOURCE_FILE}.ready
    rm -rf ${MEDIAINFO}/${MEDIAINFO_SOURCE_FILE}
    rm -rf ${MEDIAINFO}/${MEDIAINFO_SOURCE_FILE}.ready
    rm -rf ${LIBWEBSOCKETS}/${LIBWEBSOCKETS_SOURCE_FILE}
    rm -rf ${LIBWEBSOCKETS}/${LIBWEBSOCKETS_SOURCE_FILE}.ready
    rm -rf ${ICU}/${ICU_SOURCE_FILE}.ready

    echo "* Deleting object files"
    rm -rf ../obj/local/armeabi-v7a
    rm -rf ../obj/local/arm64-v8a
    rm -rf ../obj/local/x86
    rm -rf ../obj/local/x86_64
    
    echo "* Deleting libraries"
    rm -rf ${TARGET_LIB_DIR}/armeabi-v7a
    rm -rf ${TARGET_LIB_DIR}/arm64-v8a
    rm -rf ${TARGET_LIB_DIR}/x86
    rm -rf ${TARGET_LIB_DIR}/x86_64

    rm -rf ${TARGET_LIB_DIR}/armeabi-v7a
    rm -rf ${TARGET_LIB_DIR}/arm64-v8a
    rm -rf ${TARGET_LIB_DIR}/x86
    rm -rf ${TARGET_LIB_DIR}/x86_64

    echo "* Task finished OK"
    exit 0
fi

if [ "$1" != "all" ]; then
    echo "Usage: $0 <all | bindings | clean | clean_mega>";
    exit 1
fi

echo "* Building ${BUILD_ARCHS} arch(s)"

createMEGAchatBindings
echo "* MEGAchat is ready"

echo "* Setting up MEGA"
createMEGABindings
echo "* MEGA is ready"

echo "* Setting up libsodium"
if [ ! -f ${SODIUM}/${SODIUM_SOURCE_FILE}.ready ]; then
    downloadCheckAndUnpack ${SODIUM_DOWNLOAD_URL} ${SODIUM}/${SODIUM_SOURCE_FILE} ${SODIUM_SHA1} ${SODIUM}
    ln -sf ${SODIUM_SOURCE_FOLDER} ${SODIUM}/${SODIUM}
    pushd ${SODIUM}/${SODIUM} &>> ${LOG_FILE}
    export ANDROID_NDK_HOME=${NDK_ROOT}
    export NDK_PLATFORM=${APP_PLATFORM}
    patch autogen.sh < ../autogen.patch
    ./autogen.sh &>> ${LOG_FILE}
    echo "#include <limits.h>" >>  src/libsodium/include/sodium/export.h
    sed -i 's/enable-minimal/enable-minimal --disable-pie/g' dist-build/android-build.sh
    
    if [ -n "`echo ${BUILD_ARCHS} | grep -w armeabi-v7a`" ]; then
        echo "* Prebuilding libsodium for ARMv7"
        dist-build/android-armv7-a.sh &>> ${LOG_FILE}
        ln -sf libsodium-android-armv7-a libsodium-android-armeabi-v7a
    fi
    
    if [ -n "`echo ${BUILD_ARCHS} | grep -w arm64-v8a`" ]; then
        echo "* Prebuilding libsodium for ARMv8"
        dist-build/android-armv8-a.sh &>> ${LOG_FILE}
        ln -sf libsodium-android-armv8-a libsodium-android-arm64-v8a
    fi
    
    if [ -n "`echo ${BUILD_ARCHS} | grep -w x86`" ]; then
        echo "* Prebuilding libsodium for x86"
        dist-build/android-x86.sh &>> ${LOG_FILE}
        ln -sf libsodium-android-i686 libsodium-android-x86
    fi
    
    if [ -n "`echo ${BUILD_ARCHS} | grep -w x86_64`" ]; then
        echo "* Prebuilding libsodium for x86_64"
        dist-build/android-x86_64.sh &>> ${LOG_FILE}
        ln -sf libsodium-android-westmere libsodium-android-x86_64
    fi
    
    popd &>> ${LOG_FILE}
    touch ${SODIUM}/${SODIUM_SOURCE_FILE}.ready
fi
echo "* libsodium is ready"

echo "* Setting up Crypto++"
if [ ! -f ${CRYPTOPP}/${CRYPTOPP_SOURCE_FILE}.ready ]; then
    mkdir -p ${CRYPTOPP}/${CRYPTOPP}
    downloadCheckAndUnpack ${CRYPTOPP_DOWNLOAD_URL} ${CRYPTOPP}/${CRYPTOPP_SOURCE_FILE} ${CRYPTOPP_SHA1} ${CRYPTOPP}/${CRYPTOPP}
    cp ${NDK_ROOT}/sources/android/cpufeatures/cpu-features.h ${CRYPTOPP}/${CRYPTOPP}/
    touch ${CRYPTOPP}/${CRYPTOPP_SOURCE_FILE}.ready
fi
echo "* Crypto++ is ready"

echo "* Setting up SQLite"
if [ ! -f ${SQLITE}/${SQLITE_SOURCE_FILE}.ready ]; then
    downloadCheckAndUnpack ${SQLITE_DOWNLOAD_URL} ${SQLITE}/${SQLITE_SOURCE_FILE} ${SQLITE_SHA1} ${SQLITE}
    ln -fs ${SQLITE_BASE_NAME} ${SQLITE_SOURCE_FOLDER}
    touch ${SQLITE}/${SQLITE_SOURCE_FILE}.ready
fi
echo "* SQLite is ready"

echo "* Setting up libuv"
if [ ! -f ${LIBUV}/${LIBUV_SOURCE_FILE}.ready ]; then
    downloadCheckAndUnpack ${LIBUV_DOWNLOAD_URL} ${LIBUV}/${LIBUV_SOURCE_FILE} ${LIBUV_SHA1} ${LIBUV}
    ln -sf ${LIBUV_SOURCE_FOLDER} ${LIBUV}/${LIBUV}

    for ABI in ${BUILD_ARCHS}; do
        echo "* Prebuilding libuv for ${ABI}"

        setupEnv "${ABI}"

        pushd ${LIBUV}/${LIBUV} &>> ${LOG_FILE}
        ./autogen.sh &>> ${LOG_FILE}
        LDFLAGS+="-Wl,-z,max-page-size=16384" ./configure --host "${TARGET_HOST}" --with-pic --disable-shared --prefix="${BASE_PATH}/${LIBUV}/${LIBUV}"/libuv-android-${ABI} &>> ${LOG_FILE}
        make clean &>> ${LOG_FILE}
        make -j${JOBS} &>> ${LOG_FILE}
        make install &>> ${LOG_FILE}

        popd &>> ${LOG_FILE}

    done

    cleanEnv

    touch ${LIBUV}/${LIBUV_SOURCE_FILE}.ready
fi
echo "* libuv is ready"

echo "* Setting up ZenLib"
if [ ! -f ${MEDIAINFO}/${ZENLIB_SOURCE_FILE}.ready ]; then
    downloadCheckAndUnpack ${ZENLIB_DOWNLOAD_URL} ${MEDIAINFO}/${ZENLIB_SOURCE_FILE} ${ZENLIB_SHA1} ${MEDIAINFO}
    ln -sf ${ZENLIB_SOURCE_FOLDER} ${MEDIAINFO}/${ZENLIB}
    cp mega/sdk/include/mega/mega_glob.h ${MEDIAINFO}/${ZENLIB}/Source/ZenLib/glob.h
    touch ${MEDIAINFO}/${ZENLIB_SOURCE_FILE}.ready
fi
echo "* ZenLib is ready"

echo "* Setting up MediaInfo"
if [ ! -f ${MEDIAINFO}/${MEDIAINFO_SOURCE_FILE}.ready ]; then
    downloadCheckAndUnpack ${MEDIAINFO_DOWNLOAD_URL} ${MEDIAINFO}/${MEDIAINFO_SOURCE_FILE} ${MEDIAINFO_SHA1} ${MEDIAINFO}
    ln -sf ${MEDIAINFO_SOURCE_FOLDER} ${MEDIAINFO}/${MEDIAINFO}
    touch ${MEDIAINFO}/${MEDIAINFO_SOURCE_FILE}.ready
fi
echo "* MediaInfo is ready"

echo "* Checking WebRTC"
if grep ^DISABLE_WEBRTC Application.mk | grep --quiet false; then
    TMP_WEBRTC_SHA1=`sha1sum megachat/webrtc/libwebrtc_arm64.a | cut -d " " -f 1`

    if [ ! -d megachat/webrtc/include ] || [ "$TMP_WEBRTC_SHA1"  != $WEBRTC_SHA1 ]; then
        echo "ERROR: WebRTC not ready. Please download it from this link: ${WEBRTC_DOWNLOAD_URL}"
        echo "and uncompress it in megachat/webrtc"
        exit 1
    else
        echo "* WebRTC is ready"
    fi
else
    echo "* WebRTC is not needed"
fi

echo "* Setting up crashlytics"
if [ ! -f ${CURL}/${CRASHLYTICS_SOURCE_FILE}.ready ]; then
    wget ${CRASHLYTICS_DOWNLOAD_URL} -O ${CRASHLYTICS_DEST_PATH}/${CRASHLYTICS_SOURCE_FILE} &>> ${LOG_FILE} || \
        curl -L -o ${CRASHLYTICS_DEST_PATH}/${CRASHLYTICS_SOURCE_FILE} ${CRASHLYTICS_DOWNLOAD_URL} &>> ${LOG_FILE}
    wget ${CRASHLYTICS_DOWNLOAD_URL_C} -O ${CRASHLYTICS_DEST_PATH}/${CRASHLYTICS_SOURCE_FILE_C} &>> ${LOG_FILE} || \
        curl -L -o ${CRASHLYTICS_DEST_PATH}/${CRASHLYTICS_SOURCE_FILE_C} ${CRASHLYTICS_DOWNLOAD_URL_C} &>> ${LOG_FILE}

    touch ${CURL}/${CRASHLYTICS_SOURCE_FILE}.ready
fi
echo "* crashlytics is ready"

echo "* Setting up cURL"
if [ ! -f ${CURL}/${CURL_SOURCE_FILE}.ready ]; then
    echo "* Setting up cURL"
    downloadCheckAndUnpack ${CURL_DOWNLOAD_URL} ${CURL}/${CURL_SOURCE_FILE} ${CURL_SHA1} ${CURL}
    ln -sf ${CURL_SOURCE_FOLDER} ${CURL}/${CURL}

    echo "* Setting up c-ares"
    downloadCheckAndUnpack ${ARES_DOWNLOAD_URL} ${CURL}/${ARES_SOURCE_FILE} ${ARES_SHA1} ${CURL}
    ln -sf ${ARES_SOURCE_FOLDER} ${CURL}/ares

    echo "* Setting up zstd"
    mkdir -p ${CURL}/zstd
    if [ ! -f ${CURL}/zstd/${ZSTD_SOURCE_FILE}.ready ]; then
        downloadCheckAndUnpack ${ZSTD_DOWNLOAD_URL} ${CURL}/zstd/${ZSTD_SOURCE_FILE} ${ZSTD_SHA1} ${CURL}/zstd
        ln -sf ${ZSTD_SOURCE_FOLDER} ${CURL}/zstd/zstd
        for ABI in ${BUILD_ARCHS}; do
            echo "* Prebuilding zstd for ${ABI}"
            setupEnv "${ABI}"
            pushd ${CURL}/zstd/zstd/build/cmake &>> ${LOG_FILE}
            rm -rf build-android-${ABI}
            mkdir build-android-${ABI}
            cd build-android-${ABI}
            cmake -DCMAKE_TOOLCHAIN_FILE=${NDK_ROOT}/build/cmake/android.toolchain.cmake \
                  -DANDROID_ABI=${ABI} \
                  -DANDROID_PLATFORM=${APP_PLATFORM} \
                  -DZSTD_BUILD_PROGRAMS=OFF \
                  -DZSTD_BUILD_SHARED=OFF \
                  -DZSTD_BUILD_STATIC=ON \
                  -DCMAKE_BUILD_TYPE=RelWithDebInfo \
                  ..
            make -j${JOBS}
            popd &>> ${LOG_FILE}
        done
        cleanEnv
        touch ${CURL}/zstd/${ZSTD_SOURCE_FILE}.ready
    fi
    echo "* zstd is ready"

    for ABI in ${BUILD_ARCHS}; do
        echo "* Prebuilding cURL for ${ABI}"

        setupEnv "${ABI}"

        if [ "${ABI}" == "armeabi-v7a" ]; then
            WEBRTC_SUFFIX="arm"
        elif [ "${ABI}" == "arm64-v8a" ]; then
            WEBRTC_SUFFIX="arm64"
        else
            WEBRTC_SUFFIX=${ABI}
        fi

        ZSTD_INCLUDE="${BASE_PATH}/${CURL}/zstd/zstd/lib"
        ZSTD_LIB="${BASE_PATH}/${CURL}/zstd/zstd/build/cmake/build-android-${ABI}/lib"

        pushd ${CURL}/ares &>> ${LOG_FILE}
        LDFLAGS+="-Wl,-z,max-page-size=16384" ./configure --host "${TARGET_HOST}" --with-pic --disable-shared --prefix="${BASE_PATH}/${CURL}"/ares/ares-android-${ABI} &>> ${LOG_FILE}
        make clean &>> ${LOG_FILE}
        make -j${JOBS} &>> ${LOG_FILE}
        make install &>> ${LOG_FILE}
        popd &>> ${LOG_FILE}

        pushd ${CURL}/${CURL} &>> ${LOG_FILE}
        rm -r boringssl &>> ${LOG_FILE} || :
        mkdir -p boringssl/lib
        ln -s ${BASE_PATH}/megachat/webrtc/include/third_party/boringssl/src/include/ boringssl/include
        ln -s ${BASE_PATH}/megachat/webrtc/libwebrtc_${WEBRTC_SUFFIX}.a boringssl/lib/libcrypto.a
        ln -s ${BASE_PATH}/megachat/webrtc/libwebrtc_${WEBRTC_SUFFIX}.a boringssl/lib/libssl.a

        LIBS=-lc++ ./configure --host "${TARGET_HOST}" --with-pic --disable-shared --prefix="${BASE_PATH}/${CURL}/${CURL}"/curl-android-${ABI} --with-ssl="${PWD}"/boringssl/ \
          --disable-ares --enable-threaded-resolver ${CURL_EXTRA} \
          --with-zstd="${ZSTD_LIB}" \
          CPPFLAGS="-I${ZSTD_INCLUDE}" \
          LDFLAGS="-L${ZSTD_LIB} -lzstd $LDFLAGS" \
          &>> ${LOG_FILE}
        make clean &>> ${LOG_FILE}
        make -j${JOBS} &>> ${LOG_FILE}
        make install &>> ${LOG_FILE}
        popd &>> ${LOG_FILE}
    done

    cleanEnv

    touch ${CURL}/${CURL_SOURCE_FILE}.ready
fi
echo "* cURL is ready"

echo "* Setting up libwebsockets"
if [ ! -f ${LIBWEBSOCKETS}/${LIBWEBSOCKETS_SOURCE_FILE}.ready ]; then
    downloadAndUnpack ${LIBWEBSOCKETS_DOWNLOAD_URL} ${LIBWEBSOCKETS}/${LIBWEBSOCKETS_SOURCE_FILE} ${LIBWEBSOCKETS}
    ln -sf ${LIBWEBSOCKETS_SOURCE_FOLDER} ${LIBWEBSOCKETS}/${LIBWEBSOCKETS}

    for ABI in ${BUILD_ARCHS}; do
        echo "* Prebuilding libwebsockets for ${ABI}"
        if [ "${ABI}" == "armeabi-v7a" ]; then
            WEBRTC_SUFFIX="arm"
            EXTRA_FLAGS="-DCMAKE_C_FLAGS=-Wno-sign-conversion -Wno-implicit-int-conversion"
        elif [ "${ABI}" == "arm64-v8a" ]; then
            WEBRTC_SUFFIX="arm64"
            EXTRA_FLAGS="-DCMAKE_C_FLAGS=-Wno-sign-conversion -Wno-shorten-64-to-32"
        elif [ "${ABI}" == "x86" ]; then
            WEBRTC_SUFFIX=${ABI}
            EXTRA_FLAGS="-DCMAKE_C_FLAGS=-Wno-sign-conversion -Wno-implicit-int-conversion"
        elif [ "${ABI}" == "x86_64" ]; then
            WEBRTC_SUFFIX=${ABI}
            EXTRA_FLAGS="-DCMAKE_C_FLAGS=-Wno-sign-conversion -Wno-shorten-64-to-32"
        fi

        rm -r ${LIBWEBSOCKETS}/${LIBWEBSOCKETS}/build &>> ${LOG_FILE} ||:
        mkdir -p ${LIBWEBSOCKETS}/${LIBWEBSOCKETS}/build &>> ${LOG_FILE}
        pushd ${LIBWEBSOCKETS}/${LIBWEBSOCKETS}/build &>> ${LOG_FILE}
        cmake -DCMAKE_INSTALL_PREFIX="${BASE_PATH}/${LIBWEBSOCKETS}/${LIBWEBSOCKETS}/libwebsockets-android-${ABI}" -DANDROID_ABI=${ABI} -DANDROID_PLATFORM=${APP_PLATFORM} \
        -DCMAKE_TOOLCHAIN_FILE=${NDK_ROOT}/build/cmake/android.toolchain.cmake -DLWS_WITH_SHARED=OFF -DLWS_WITH_STATIC=ON -DLWS_WITHOUT_TESTAPPS=ON \
        -DLWS_WITHOUT_SERVER=ON -DLWS_IPV6=ON -DLWS_STATIC_PIC=ON -DLWS_WITH_HTTP2=0 -DLWS_WITH_BORINGSSL=ON -DLWS_SSL_CLIENT_USE_OS_CA_CERTS=0 \
        -DLWS_OPENSSL_INCLUDE_DIRS="${BASE_PATH}/megachat/webrtc/include/third_party/boringssl/src/include" -DLWS_OPENSSL_LIBRARIES="${BASE_PATH}/megachat/webrtc/libwebrtc_${WEBRTC_SUFFIX}.a" \
        -DLWS_WITH_LIBUV=1 -DLWS_LIBUV_INCLUDE_DIRS="${BASE_PATH}/${LIBUV}/${LIBUV}/libuv-android-${ABI}/include" \
        -DLWS_LIBUV_LIBRARIES="${BASE_PATH}/${LIBUV}/${LIBUV}/libuv-android-${ABI}/lib/libuv.a" "${EXTRA_FLAGS}" \
        ../ &>> ${LOG_FILE}

        cmake --build . -- -j${JOBS} &>> ${LOG_FILE}
        cmake --build . --target install &>> ${LOG_FILE}
        popd &>> ${LOG_FILE}
    done

    touch ${LIBWEBSOCKETS}/${LIBWEBSOCKETS_SOURCE_FILE}.ready
fi
echo "* libwebsockets is ready"

echo "* Setting up ICU"
if [ ! -f ${ICU}/${ICU_SOURCE_FILE}.ready ]; then
    downloadCheckAndUnpack ${ICU_DOWNLOAD_URL} ${ICU}/${ICU_SOURCE_FILE} ${ICU_SHA1} ${ICU}/${ICU_SOURCE_VERSION}

    pushd "${ICU}/${ICU_SOURCE_VERSION}/icu" &>> ${LOG_FILE}
    sed -i -e 's/\r$//' source/runConfigureICU
    sed -i -e 's/\r$//' source/configure
    sed -i -e 's/\r$//' source/config.sub
    sed -i -e 's/\r$//' source/config.guess
    sed -i -e 's/\r$//' source/config/make2sh.sed
    sed -i -e 's/\r$//' source/mkinstalldirs

    mkdir -p linux && cd linux

    LDFLAGS+="-Wl,-z,max-page-size=16384" CONFIGURE_LINUX_OPTIONS="--enable-static --enable-shared=no --enable-extras=no --enable-strict=no --enable-icuio=no --enable-layout=no --enable-layoutex=no --enable-tools=yes --enable-tests=no --enable-samples=no --enable-dyload=no"
    ../source/runConfigureICU Linux CFLAGS="-Os" CXXFLAGS="--std=c++11" ${CONFIGURE_LINUX_OPTIONS} &>> ${LOG_FILE}

    make -j${JOBS} &>> ${LOG_FILE}

    export CROSS_BUILD_DIR=$(realpath .)
    export ANDROID_NDK=${NDK_ROOT}

    popd &>> ${LOG_FILE}

    for ABI in ${BUILD_ARCHS}; do
        echo "* Compiling ICU for ${ABI}"
        setupEnv "${ABI}"

        pushd "${ICU}/${ICU_SOURCE_VERSION}/icu" &>> ${LOG_FILE}

        mkdir -p ${ABI} && cd ${ABI}

        if [ "${ABI}" == "armeabi-v7a" ]; then
            HOST=arm-linux-androideabi
            ARCH=arm
        elif [ "${ABI}" == "arm64-v8a" ]; then
            HOST=aarch64-linux-android
            ARCH=arm64
        elif [ "${ABI}" == "x86" ]; then
            HOST=i686-linux-android
            ARCH=x86
        elif [ "${ABI}" == "x86_64" ]; then
            HOST=i686-linux-android
            ARCH=x86_64
        fi

        export ANDROID_TOOLCHAIN=$(pwd)/${ICU}/toolchain-${ABI}
        export PATH=$ANDROID_TOOLCHAIN/bin:$PATH

        rm -rf ${ANDROID_TOOLCHAIN} &>> ${LOG_FILE}
        $NDK_ROOT/build/tools/make_standalone_toolchain.py --arch=${ARCH} --api=${API_LEVEL} --install-dir=${ANDROID_TOOLCHAIN} &>> ${LOG_FILE}

        CONFIGURE_ANDROID_OPTIONS="--host=${HOST} --enable-static --enable-shared=no --enable-extras=no --enable-strict=no --enable-icuio=no --enable-layout=no --enable-layoutex=no --enable-tools=no --enable-tests=no --enable-samples=no --enable-dyload=no -with-cross-build=$CROSS_BUILD_DIR"

        ../source/configure CFLAGS="-Os -fPIC" CXXFLAGS="--std=c++11 -fPIC" ${CONFIGURE_ANDROID_OPTIONS}  &>> ${LOG_FILE}

        make -j${JOBS} &>> ${LOG_FILE}

        popd &>> ${LOG_FILE}
    done

    cleanEnv
    touch ${ICU}/${ICU_SOURCE_FILE}.ready
fi
echo "* ICU is ready"

echo "* All dependencies are prepared!"

rm -rf ../tmpLibs
mkdir ../tmpLibs
if [ -n "`echo ${BUILD_ARCHS} | grep -w x86`" ]; then
    echo "* Running ndk-build x86"
    ${NDK_BUILD} V=1 NDK_LIBS_OUT=${TARGET_LIB_DIR} -j${JOBS} APP_ABI=x86 &>> ${LOG_FILE}
    mv ${TARGET_LIB_DIR}/x86 ../tmpLibs/
    echo "* ndk-build finished for x86"
fi

if [ -n "`echo ${BUILD_ARCHS} | grep -w armeabi-v7a`" ]; then
    echo "* Running ndk-build arm 32bits"
    ${NDK_BUILD} V=1 NDK_LIBS_OUT=${TARGET_LIB_DIR} -j${JOBS} APP_ABI=armeabi-v7a &>> ${LOG_FILE}
    mv ${TARGET_LIB_DIR}/armeabi-v7a ../tmpLibs/
    echo "* ndk-build finished for arm 32bits"
fi

if [ -n "`echo ${BUILD_ARCHS} | grep -w x86_64`" ]; then
    echo "* Running ndk-build x86_64"
    ${NDK_BUILD} V=1 NDK_LIBS_OUT=${TARGET_LIB_DIR} -j${JOBS} APP_ABI=x86_64 &>> ${LOG_FILE}
    mv ${TARGET_LIB_DIR}/x86_64 ../tmpLibs/
    echo "* ndk-build finished for x86_64"
fi

if [ -n "`echo ${BUILD_ARCHS} | grep -w arm64-v8a`" ]; then
    echo "* Running ndk-build arm 64bits"
    ${NDK_BUILD} V=1 NDK_LIBS_OUT=${TARGET_LIB_DIR} -j${JOBS} APP_ABI=arm64-v8a &>> ${LOG_FILE}
  ensureArm64TargetExists
    echo "* ndk-build finished for arm 64bits"
    mv ${TARGET_LIB_DIR}/arm64-v8a ../tmpLibs/
fi
mv ../tmpLibs/* ${TARGET_LIB_DIR}
rm -fr ../tmpLibs/

echo "* Task finished OK"
