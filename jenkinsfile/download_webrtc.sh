
# download cache folders
ROOT_DOWNLOAD_FOLDER=$WORKSPACE/../mega_android_ci_download
WEBRTC_DOWNLOAD_FOLDER=${ROOT_DOWNLOAD_FOLDER}/webrtc_download

# SDK build script path
BUILD_SH=${WORKSPACE}/sdk/src/main/jni/build.sh

# SHA1 checksum target file
WEBRTC_LIB_FILE=${WEBRTC_DOWNLOAD_FOLDER}/webrtc/libwebrtc_arm64.a

# target paths in MEGA Chat SDK
MEGACHAT_SDK_ROOT_FOLDER=${WORKSPACE}/sdk/src/main/jni/megachat
TARGET_FOLDER=${MEGACHAT_SDK_ROOT_FOLDER}/webrtc

function download_webrtc {

    # remove webrtc from target folder
    echo "  Deleting webrtc from target folder"
    rm -fr "${TARGET_FOLDER}"
    echo "  Deleting webrtc from cache folder"
    cd "${WEBRTC_DOWNLOAD_FOLDER}" || exit 1
    rm -fr *

    URL=$1
    echo "  WebRTC URL: $URL"
    echo "### downloading..."
    mega-get $URL
    echo
    echo "### unzipping..."
    unzip -q *.zip

    FOLDER=$(find . -iname \*web\* -type d -maxdepth 1 -print | head -n1)
    echo
    # echo "unzipped content: $FOLDER"
    if test -f "$FOLDER/libwebrtc_arm64.a"; then
        echo "### WebRTC folder found!"
        echo "  $(pwd)"
        echo "### copying WebRTC to target folder"
        cp -fr "webrtc"  "${MEGACHAT_SDK_ROOT_FOLDER}"
    else
        echo "### WebRTC not in root of zip file. Go down to subfolder...."
        cd $FOLDER || exit 1
        SUB_FOLDER=$(find . -iname \*web\* -type d -maxdepth 1 -print | head -n1)
        if test -f "${SUB_FOLDER}/libwebrtc_arm64.a"; then
            echo "  $(pwd)"
            echo "### Copying webrtc to target folder"
            cp -fr "webrtc"  "${MEGACHAT_SDK_ROOT_FOLDER}"
        else
            echo "### cannot found webrtc in downloaded structure. Aborting...."
            exit 1
        fi
    fi

    echo
    echo ">> Latest WebRTC download completed!"
}

mkdir -p "${WEBRTC_DOWNLOAD_FOLDER}"
echo "### Reading WebRTC info from build.sh"
EXPECTED_CHECKSUM=$(cat "${BUILD_SH}" | grep -E ^"WEBRTC_SHA1".* | cut -d'=' -f2)
DOWNLOAD_URL=$(cat "${BUILD_SH}" | grep -E ^"WEBRTC_DOWNLOAD_URL".* | cut -d'=' -f2)
echo "  DOWNLOAD_URL=${DOWNLOAD_URL}"
echo "  EXPECTED_CHECKSUM=${EXPECTED_CHECKSUM}"

if test -f "${WEBRTC_LIB_FILE}"; then
    ACTUAL_CHECKSUM=$(shasum "${WEBRTC_LIB_FILE}" | cut -d " " -f 1)
    echo "  Sample_Lib_File_Exist=${WEBRTC_LIB_FILE}"
    echo "  Actual_SHA1=${ACTUAL_CHECKSUM}"
    echo

    if [[  "$ACTUAL_CHECKSUM" == $EXPECTED_CHECKSUM  ]]; then
        echo "### Expected WebRTC already in cache. Copying into target folder"
        rm -fr "${TARGET_FOLDER}"
        cp -fr "${WEBRTC_DOWNLOAD_FOLDER}/webrtc"  "${MEGACHAT_SDK_ROOT_FOLDER}"
        echo "## Latest WebRTC update completed!"
        exit 0
    else
        echo "### WebRTC lib exists, but not the latest. Deleting old one and start downloading new one"
        download_webrtc "${DOWNLOAD_URL}"
    fi
else
    echo "### WebRTC lib not found in target folder! Download new webrtc lib file, unzip and copy to target folder"
    download_webrtc "${DOWNLOAD_URL}"
fi
