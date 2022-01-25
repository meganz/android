

# env variables from Jenkins
WORKSPACE="/Users/robin/work/android"

# local variables
LIB_DOWNLOAD_ROOT="/Users/robin/Desktop/mega_android_ci_download"
WEBRTC_DOWNLOAD_PATH="${LIB_DOWNLOAD_ROOT}/webrtc_download"
BUILD_SH="${WORKSPACE}/app/src/main/jni/build.sh"
WEBRTC_LIB_FILE=${WORKSPACE}/app/src/main/jni/megachat/webrtc/libwebrtc_arm64.a

mkdir -p "${WEBRTC_DOWNLOAD_PATH}"

function get_property_from_build_script {
    FILE_PATH=$1
    PROPERTY_KEY=$2
    VALUE=$(cat "${FILE_PATH}" | grep -E ^"${PROPERTY_KEY}".* | cut -d'=' -f2)
    echo $VALUE
}

function download_webrtc {
    URL=$1
    cd ${WEBRTC_DOWNLOAD_PATH}
    #rm -fr *
    echo "download_webrtc url = $URL"
    echo ">>> downloading..."
    #mega-get $URL
    echo
    echo ">>> unzipping..."
    #unzip *.zip

    FOLDER=$(find . -iname \*web\* -type d -maxdepth 1 -print | head -n1)
    echo
    echo "unzipped content: $FOLDER"
    if test -f "$FOLDER/libwebrtc_arm64.a"; then
        echo ">>> webrtc folder found!"
        pwd
        echo ">>> copying webrtc to target folder"
        cp -frv "webrtc"  ${WORKSPACE}/app/src/main/jni/megachat/
    else
        echo ">>> webrtc not in root of zip file. Go down to subfolder...."
        cd $FOLDER
        SUB_FOLDER=$(find . -iname \*web\* -type d -maxdepth 1 -print | head -n1)
        if test -f "${SUB_FOLDER}/libwebrtc_arm64.a"; then
            pwd
            echo ">>> copying webrtc to target folder"
            cp -frv "webrtc"  ${WORKSPACE}/app/src/main/jni/megachat/
        else
            echo ">>> cannot found webrtc in downloaded structure. Aborting...."
            exit 1
        fi
    fi

}

echo ">>> Reading WebRTC from build.sh"
WEBRTC_LIB_SHA1=$(get_property_from_build_script "${BUILD_SH}"  "WEBRTC_SHA1")
WEBRTC_DOWNLOAD_URL=$(get_property_from_build_script "${BUILD_SH}"  "WEBRTC_DOWNLOAD_URL")
echo "WEBRTC_DOWNLOAD_URL=${WEBRTC_DOWNLOAD_URL}"
echo "WEBRTC_LIB_SHA1=${WEBRTC_LIB_SHA1}"

if test -f "${WEBRTC_LIB_FILE}"; then
    TMP_WEBRTC_SHA1=`shasum ${WEBRTC_LIB_FILE} | cut -d " " -f 1`
    echo "File exist=${WEBRTC_LIB_FILE}"
    echo "Actual_SHA1=${TMP_WEBRTC_SHA1}"
    echo
    # echo "TMP_WEBRTC_SHA1=${TMP_WEBRTC_SHA1}"
    if [[  "$TMP_WEBRTC_SHA1"  == $WEBRTC_LIB_SHA1  ]]; then
        echo ">>> Already using latest webrtc lib. No need to download"
        cd ${WEBRTC_DOWNLOAD_PATH}
        echo mega-get ${WEBRTC_DOWNLOAD_URL}
        exit 0
    else
        echo ">>> WEBRTC lib exists, but not the latest. Deleting old one and start downloading new one"
        echo "rm -fr ${WORKSPACE}/app/src/main/jni/megachat/webrtc"
        cd ${LIB_DOWNLOAD_ROOT}
        echo mega-get ${WEBRTC_DOWNLOAD_URL}/webrtc_folder
        echo
    fi
else
    echo
    echo ">>> WebRTC lib not found in target folder!"
    echo ">>> download new webrtc lib file, unzip and copy to target folder"
    UNZIPPED_FOLDER=$(download_webrtc ${WEBRTC_DOWNLOAD_URL})
    echo $UNZIPPED_FOLDER

fi
