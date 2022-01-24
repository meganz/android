
LIB_DOWNLOAD_ROOT="~/mega_build_download"
WEBRTC_DOWNLOAD_PATH=""

WORKSPACE="/Users/robin/work/android"
BUILD_SH="${WORKSPACE}/app/src/main/jni/build.sh"

mkdir -p "${LIB_DOWNLOAD_ROOT}"



function get_property_from_build_script {
    FILE_PATH=$1
    PROPERTY_KEY=$2
    VALUE=$(cat "${FILE_PATH}" | grep -E ^"${PROPERTY_KEY}".* | cut -d'=' -f2)
    echo $VALUE
}

WEBRTC_LIB_SHA1=$(get_property_from_build_script "${BUILD_SH}"  "WEBRTC_SHA1")
WEBRTC_DOWNLOAD_URL=$(get_property_from_build_script "${BUILD_SH}"  "WEBRTC_DOWNLOAD_URL")

# echo $WEBRTC_LIB_SHA1
# echo $WEBRTC_DOWNLOAD_URL

WEBRTC_LIB_FILE=${WORKSPACE}/app/src/main/jni/megachat/webrtc/libwebrtc_arm64.a
if test -f "${WEBRTC_LIB_FILE}"; then
    # echo "file exist: ${WEBRTC_LIB_FILE}"
    TMP_WEBRTC_SHA1=`shasum ${WEBRTC_LIB_FILE} | cut -d " " -f 1`
    # echo "TMP_WEBRTC_SHA1=${TMP_WEBRTC_SHA1}"
    if [[  "$TMP_WEBRTC_SHA1"  == $WEBRTC_LIB_SHA1  ]]; then
        echo "Already using latest webrtc lib. No need to download"
        exit 0
    else
        echo "WEBRTC lib exists, but not the latest. Deleting old one and start downloading new one"
        echo "rm -fr ${WORKSPACE}/app/src/main/jni/megachat/webrtc"
    fi
else
    echo "file NOT exist: ${WEBRTC_LIB_FILE}"
fi
