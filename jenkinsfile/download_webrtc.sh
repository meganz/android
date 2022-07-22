
# local variables
LIB_DOWNLOAD_ROOT="$WORKSPACE/mega_android_ci_download"
WEBRTC_DOWNLOAD_PATH="${LIB_DOWNLOAD_ROOT}/webrtc_download"
BUILD_SH="${WORKSPACE}/sdk/src/main/jni/build.sh"
SAMPLE_LIB_FILE="libwebrtc_arm64.a"
WEBRTC_LIB_FILE=${WORKSPACE}/sdk/src/main/jni/megachat/webrtc/${SAMPLE_LIB_FILE}

mkdir -p "${WEBRTC_DOWNLOAD_PATH}"

function download_webrtc {

    # remove webrtc from target folder
    echo "  Deleting webrtc from target folder"
    rm -fr ${WORKSPACE}/sdk/src/main/jni/megachat/webrtc

    URL=$1
    cd ${WEBRTC_DOWNLOAD_PATH}
    rm -fr *
    echo "  WebRTC URL: $URL"
    echo
    echo ">>> downloading..."
    mega-get $URL
    echo
    echo ">>> unzipping..."
    unzip -q *.zip

    FOLDER=$(find . -iname \*web\* -type d -maxdepth 1 -print | head -n1)
    echo
    # echo "unzipped content: $FOLDER"
    if test -f "$FOLDER/libwebrtc_arm64.a"; then
        echo ">>> WebRTC folder found!"
        echo "  $(pwd)"
        echo ">>> copying WebRTC to target folder"
        cp -fr "webrtc"  ${WORKSPACE}/sdk/src/main/jni/megachat/
    else
        echo ">>> WebRTC not in root of zip file. Go down to subfolder...."
        cd $FOLDER
        SUB_FOLDER=$(find . -iname \*web\* -type d -maxdepth 1 -print | head -n1)
        if test -f "${SUB_FOLDER}/libwebrtc_arm64.a"; then
            echo "  $(pwd)"
            echo ">>> Copying webrtc to target folder"
            cp -fr "webrtc"  ${WORKSPACE}/sdk/src/main/jni/megachat/
        else
            echo ">>> cannot found webrtc in downloaded structure. Aborting...."
            exit 1
        fi
    fi

    echo
    echo ">> Latest WebRTC download completed!"
}

echo
echo ">>> Reading WebRTC info from build.sh"
WEBRTC_LIB_SHA1=$(cat "${BUILD_SH}" | grep -E ^"WEBRTC_SHA1".* | cut -d'=' -f2)
WEBRTC_DOWNLOAD_URL=$(cat "${BUILD_SH}" | grep -E ^"WEBRTC_DOWNLOAD_URL".* | cut -d'=' -f2)
echo "  WEBRTC_DOWNLOAD_URL=${WEBRTC_DOWNLOAD_URL}"
echo "  WEBRTC_LIB_SHA1=${WEBRTC_LIB_SHA1}"

if test -f "${WEBRTC_LIB_FILE}"; then
    TMP_WEBRTC_SHA1=`shasum ${WEBRTC_LIB_FILE} | cut -d " " -f 1`
    echo "  Sample_Lib_File_Exist=${WEBRTC_LIB_FILE}"
    echo "  Actual_SHA1=${TMP_WEBRTC_SHA1}"
    echo
    # echo "TMP_WEBRTC_SHA1=${TMP_WEBRTC_SHA1}"
    if [[  "$TMP_WEBRTC_SHA1"  == $WEBRTC_LIB_SHA1  ]]; then
        echo ">>> Sample Lib SHA1 matches! Already using latest webrtc lib. No need to download. Bye."
        exit 0
    else
        echo ">>> WebRTC lib exists, but not the latest. Deleting old one and start downloading new one"
        download_webrtc ${WEBRTC_DOWNLOAD_URL}
    fi
else
    echo ">>> WebRTC lib not found in target folder! Download new webrtc lib file, unzip and copy to target folder"
    download_webrtc ${WEBRTC_DOWNLOAD_URL}
fi
