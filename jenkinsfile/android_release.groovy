/**
 * This script is to build and upload Android AAB to Google Play Store
 */

BUILD_STEP = ''

// Below values will be read from MR description and are used to decide SDK versions
SDK_BRANCH = 'develop'
MEGACHAT_BRANCH = 'develop'

/**
 * Flag to decide whether we do clean before build SDK.
 * Possible values: yes|no
 */
REBUILD_SDK = "no"

/**
 * Flag to decide whether do clean up for SDK and Android code
 */
DO_CLEANUP = true

/**
 * Folder to contain build outputs, including APK, AAG and symbol files
 */
ARCHIVE_FOLDER = "archive"
NATIVE_SYMBOL_FILE = "symbols.zip"
ARTIFACTORY_BASE_URL = 'https://artifactory.developers.mega.co.nz/artifactory/android-mega/release'
ARTIFACTORY_BUILD_INFO = "buildinfo.txt"

/**
 * Default release notes content files
 */
RELEASE_NOTES = "default_release_notes.json"

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }
    options {
        // Stop the build early in case of compile or test failures
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
        timeout(time: 2, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }
    environment {
        LC_ALL = 'en_US.UTF-8'
        LANG = 'en_US.UTF-8'

        NDK_ROOT = '/opt/buildtools/android-sdk/ndk/21.3.6528147'
        JAVA_HOME = '/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx'
        ANDROID_HOME = '/opt/buildtools/android-sdk'

        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        CONSOLE_LOG_FILE = 'console.txt'

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
    }
    post {
        failure {
            script {
                downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                if (hasGitLabMergeRequest()) {
                    String link = uploadFileToGitLab(CONSOLE_LOG_FILE)

                    def message = ""
                    if (triggeredByDeliverAppStore()) {
                        message = releaseFailureMessage("<br/>") +
                                "<br/>Build Log:\t${link}"
                    } else if (triggeredByUploadSymbol()) {
                        message = uploadSymbolFailureMessage("<br/>") +
                                "<br/>Build Log:\t${link}"
                    }
                    sendToMR(message)
                } else {
                    slackSend color: 'danger', message: releaseFailureMessage("\n")
                    slackUploadFile filePath: CONSOLE_LOG_FILE, initialComment: 'Jenkins Log'
                }
            }
        }
        success {
            script {
                if (!isOnReleaseBranch()) {
                    sendToMR(skipMessage("<br/>"))
                } else if (hasGitLabMergeRequest()) {
                    downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                    String link = uploadFileToGitLab(CONSOLE_LOG_FILE)

                    if (triggeredByDeliverAppStore()) {
                        def message = releaseSuccessMessage("<br/>") +
                                "<br/>Build Log:\t${link}"
                        sendToMR(message)

                        sendToMR(getBuildVersionInfo())

                        slackSend color: "good", message: releaseSuccessMessage("\n")
                    } else if (triggeredByUploadSymbol()) {
                        def message = uploadSymbolSuccessMessage("<br/>") +
                                "<br/>Build Log:\t${link}"
                        sendToMR(message)

                        slackSend color: "good", message: uploadSymbolSuccessMessage("\n")
                    }

                }
            }
        }
    }
    stages {
        stage('Preparation') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByUploadSymbol() }
            }
            steps {
                script {
                    BUILD_STEP = 'Preparation'

                    // send command acknowledgement to MR
                    sendToMR(":runner: Android CD Release pipeline has started!!!" +
                            "<br/><b>Command</b>: ${env.gitlabTriggerPhrase}"
                    )

                    checkSDKVersion()

                    REBUILD_SDK = getValueInMRDescriptionBy("REBUILD_SDK")

                    sh("rm -frv $ARCHIVE_FOLDER")
                    sh("mkdir -p ${WORKSPACE}/${ARCHIVE_FOLDER}")
                    sh("rm -fv ${CONSOLE_LOG_FILE}")
                    sh('set')
                }
            }
        }
        stage('Fetch SDK Submodules') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByUploadSymbol() }
            }
            steps {
                script {
                    BUILD_STEP = 'Fetch SDK Submodules'
                }
                withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
                    script {
                        sh '''
                            cd ${WORKSPACE}
                            git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".url https://code.developers.mega.co.nz/sdk/sdk.git
                            git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".branch develop
                            git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".url https://code.developers.mega.co.nz/megachat/MEGAchat.git
                            git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".branch develop
                            git submodule sync
                            git submodule update --init --recursive --remote 
                            cd sdk/src/main/jni/mega/sdk
                            git fetch
                            cd ../../megachat/sdk
                            git fetch
                            cd ${WORKSPACE}
                        '''
                    }
                }

            }
        }
        stage('Select SDK Version') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByUploadSymbol() }
            }
            steps {
                script {
                    BUILD_STEP = 'Select SDK Version'
                }
                withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
                    script {
                        checkoutSdkByBranch(SDK_BRANCH)
                        checkoutMegaChatSdkByBranch(MEGACHAT_BRANCH)
                    }
                }
            }
        }

        stage('Download Dependency Lib for SDK') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByUploadSymbol() }
            }
            steps {
                script {
                    BUILD_STEP = 'Download Dependency Lib for SDK'
                    sh """

                        cd "${WORKSPACE}/jenkinsfile/"
                        bash download_webrtc.sh

                        mkdir -p "${BUILD_LIB_DOWNLOAD_FOLDER}"
                        cd "${BUILD_LIB_DOWNLOAD_FOLDER}"
                        pwd
                        ls -lh
                    """
                }

                withCredentials([
                        file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_DEBUG', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_DEBUG'),
                        file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_RELEASE', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_RELEASE')
                ]) {
                    script {
                        println("applying production google map api config... ")
                        sh 'mkdir -p app/src/debug/res/values'
                        sh 'mkdir -p app/src/release/res/values'
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_DEBUG} app/src/debug/res/values/google_maps_api.xml"
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_RELEASE} app/src/release/res/values/google_maps_api.xml"
                    }
                }
            }
        }
        stage('Build SDK') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByUploadSymbol() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build SDK'

                    cleanSdk()

                    sh """
                        echo "=== START SDK BUILD===="
                        cd ${WORKSPACE}/sdk/src/main/jni
                        bash build.sh all
                    """
                }
            }
        }
        stage('Build GMS APK') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build GMS APK'
                    sh './gradlew clean app:assembleGmsRelease'
                }
            }
        }
        stage('Sign GMS APK') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Sign GMS APK'
                }
                withCredentials([
                        file(credentialsId: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE', variable: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE'),
                        file(credentialsId: 'ANDROID_PRD_GMS_APK_KEYSTORE', variable: 'ANDROID_PRD_GMS_APK_KEYSTORE')
                ]) {
                    script {
                        println("signing GMS APK")
                        String tempAlignedGmsApk = "unsigned_gms_apk_aligned.apk"
                        String gmsApkInput = "${WORKSPACE}/app/build/outputs/apk/gms/release/app-gms-release-unsigned.apk"
                        String gmsApkOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${readAppVersion2()}-gms-release.apk"
                        println("input = $gmsApkInput \noutput = $gmsApkOutput")
                        sh """
                            rm -fv ${tempAlignedGmsApk}
                            zipalign -p 4 ${gmsApkInput} ${tempAlignedGmsApk}
                            apksigner sign --ks "${ANDROID_PRD_GMS_APK_KEYSTORE}" --ks-pass file:"${ANDROID_PRD_GMS_APK_PASSWORD_FILE}" --out ${gmsApkOutput} ${tempAlignedGmsApk}
                            ls -lh ${WORKSPACE}/${ARCHIVE_FOLDER}
                            rm -fv ${tempAlignedGmsApk}
                        """
                        println("Finish signing APK. ($gmsApkOutput) generated!")
                    }
                }
            }
        }
        stage('Build GMS AAB') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build GMS AAB'
                    sh './gradlew clean app:bundleGmsRelease'
                }
            }
        }

        stage('Sign GMS AAB') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Sign GMS AAB'
                }
                withCredentials([
                        string(credentialsId: 'ANDROID_PRD_GMS_AAB_PASSWORD', variable: 'ANDROID_PRD_GMS_AAB_PASSWORD'),
                        file(credentialsId: 'ANDROID_PRD_GMS_AAB_KEYSTORE', variable: 'ANDROID_PRD_GMS_AAB_KEYSTORE')
                ]) {
                    script {
                        println("signing GMS AAB")
                        String gmsAabInput = "${WORKSPACE}/app/build/outputs/bundle/gmsRelease/app-gms-release.aab"
                        String gmsAabOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${readAppVersion2()}-gms-release.aab"
                        println("input = $gmsAabInput \noutput = $gmsAabOutput")
                        withEnv([
                                "GMS_AAB_INPUT=${gmsAabInput}",
                                "GMS_AAB_OUTPUT=${gmsAabOutput}"
                        ]) {
                            sh('jarsigner -sigalg SHA1withRSA -digestalg SHA1 -keystore ${ANDROID_PRD_GMS_AAB_KEYSTORE} -storepass "${ANDROID_PRD_GMS_AAB_PASSWORD}" -signedjar ${GMS_AAB_OUTPUT} ${GMS_AAB_INPUT} megaandroid-upload')
                        }
                        println("Finish signing GMS AAB. ($gmsAabOutput) generated!")
                    }
                }
            }
        }
        stage('Upload Firebase Crashlytics symbol files') {
            when {
                expression { triggeredByUploadSymbol() }
            }
            steps {
                script {
                    BUILD_STEP = 'Upload Firebase Crashlytics symbol files'
                    sh """
                    cd $WORKSPACE
                    ./gradlew clean app:assembleGmsRelease app:uploadCrashlyticsSymbolFileGmsRelease
                    """
                }
            }
        }
        stage('Collect native symbol files') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Collect native symbol files'

                    deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/arm64-v8a",
                            "libmega.so")
                    deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/armeabi-v7a/",
                            "libmega.so")
                    deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/x86",
                            "libmega.so")
                    deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/x86_64",
                            "libmega.so")

                    sh """
                        cd ${WORKSPACE}/sdk/src/main/obj/local
                        rm -fv */.DS_Store
                        rm -fv .DS_Store
                        zip -r ${NATIVE_SYMBOL_FILE} .
                        mv -v ${NATIVE_SYMBOL_FILE} ${WORKSPACE}/${ARCHIVE_FOLDER}
                    """
                }
            }
        }
        stage('Archive files') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Archive files'
                    println("Uploading files to Artifactory repo....")

                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN')
                    ]) {

                        String targetPath = "$ARTIFACTORY_BASE_URL/${artifactoryUploadPath()}/"

                        withEnv([
                                "TARGET_PATH=${targetPath}"
                        ]) {
                            createBriefBuildInfoFile()

                            sh '''
                                cd ${WORKSPACE}/archive
                                ls -l ${WORKSPACE}/archive
    
                                echo Uploading APK files
                                for FILE in *.apk; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                                
                                echo Uploading AAB files
                                for FILE in *.aab; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                                
                                echo Uploading native symbol file
                                for FILE in *.zip; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                                
                                echo Uploading documentation
                                for FILE in *.txt; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                            '''
                        }

                    }
                }
            }
        }
        stage('Deploy to Google Play Alpha') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Deploy to Google Play Alpha'
                }
                script {
                    // Get the formatted release notes
                    String release_notes = releaseNotes(RELEASE_NOTES)
                    
                    // Upload the AAB to Google Play
                    androidApkUpload googleCredentialsId: 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL',
                            filesPattern: 'archive/*-gms-release.aab',
                            trackName: 'alpha',
                            rolloutPercentage: '0',
                            additionalVersionCodes: '476,485',
                            nativeDebugSymbolFilesPattern: "archive/${NATIVE_SYMBOL_FILE}",
                            recentChangeList: getRecentChangeList(release_notes)
                }
            }
        }
        stage('Clean up') {
            steps {
                script {
                    BUILD_STEP = 'Clean Up'

                    printWorkspaceSize("workspace size before clean:")
                    cleanAndroid()
                    cleanSdk()
                    printWorkspaceSize("workspace size after clean:")
                }
            }
        }
    }
}

/**
 * Compose the failure message of "deliver_appStore" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
private String releaseFailureMessage(String lineBreak) {
    String message = ":x: Android Release Failed!" +
            "${lineBreak}Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
    if (env.gitlabActionType == "PUSH") {
        message += "${lineBreak}Trigger Reason: git PUSH"
    } else if (env.gitlabActionType == "NOTE") {
        message += "${lineBreak}Trigger Reason: MR comment (${gitlabTriggerPhrase})"
    }
    return message
}

/**
 * compose the success message of "upload_symbol" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
private String uploadSymbolFailureMessage(String lineBreak) {
    return ":x: Android Firebase Crashlytics symbol upload Failed!" +
            "${lineBreak}Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

/**
 * Get the value from GitLab MR description by key
 * @param key the key to check and read
 * @return actual value of key if key is specified. null otherwise.
 */
String getValueInMRDescriptionBy(String key) {
    if (key == null || key.isEmpty()) return null
    def description = env.gitlabMergeRequestDescription
    if (description == null) return null
    String[] lines = description.split('\n')
    for (String line : lines) {
        line = line.trim()
        if (line.startsWith(key)) {
            String value = line.substring(key.length() + 1)
            print("getValueInMRDescriptionBy(): " + key + " ==> " + value)
            return value
        }
    }
    return null
}

/**
 * check if a certain value is defined by checking the tag value
 * @param value value of tag
 * @return true if tag has a value. false if tag is null or zero length
 */
static boolean isDefined(String value) {
    return value != null && !value.isEmpty()
}

/**
 * checkout SDK by branch
 * @param sdkBranch the branch to checkout
 */
private void checkoutSdkByBranch(String sdkBranch) {
    sh "echo checkoutSdkByBranch"
    sh "cd \"$WORKSPACE\""
    sh 'git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".url https://code.developers.mega.co.nz/sdk/sdk.git'
    sh "git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".branch \"$sdkBranch\""
    sh 'git submodule sync'
    sh 'git submodule update --init --recursive --remote'
}

/**
 * checkout MEGAchat SDK by branch
 * @param megaChatBranch the branch to checkout
 */
private void checkoutMegaChatSdkByBranch(String megaChatBranch) {
    sh "echo checkoutMegaChatSdkByBranch"
    sh "cd \"$WORKSPACE\""
    sh 'git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".url https://code.developers.mega.co.nz/megachat/MEGAchat.git'
    sh "git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".branch \"${megaChatBranch}\""
    sh 'git submodule sync'
    sh 'git submodule update --init --recursive --remote'
}

/**
 * Check if this build is triggered by a GitLab Merge Request.
 * @return true if this build is triggerd by a GitLab MR. False if this build is triggerd
 * by a plain git push.
 */
private boolean hasGitLabMergeRequest() {
    return env.gitlabMergeRequestIid != null && !env.gitlabMergeRequestIid.isEmpty()
}

/**
 * send message to GitLab MR comment
 * @param message message to send
 */
private void sendToMR(String message) {
    if (hasGitLabMergeRequest()) {
        def mrNumber = env.gitlabMergeRequestIid
        withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
            env.MARKDOWN_LINK = message
            env.MERGE_REQUEST_URL = "https://code.developers.mega.co.nz/api/v4/projects/199/merge_requests/${mrNumber}/notes"
            sh 'curl --request POST --header PRIVATE-TOKEN:$TOKEN --form body=\"${MARKDOWN_LINK}\" ${MERGE_REQUEST_URL}'
        }
    }
}

/**
 * compose the success message of "deliver_appStore" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
private String releaseSuccessMessage(String lineBreak) {
    return ":rocket: Android Release uploaded to Google Play Alpha channel successfully!" +
            "${lineBreak}Version:\t${readAppVersion1()}" +
            "${lineBreak}Last Commit Msg:\t${lastCommitMessage()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

/**
 * compose the success message of "upload_symbol" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
private String uploadSymbolSuccessMessage(String lineBreak) {
    return ":rocket: Firebase Crashlytics symbol uploaded successfully!" +
            "${lineBreak}Version:\t${readAppVersion1()}" +
            "${lineBreak}Last Commit Msg:\t${lastCommitMessage()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

private String sdkCommitId() {
    String commitId = sh(
            script: """
                cd ${WORKSPACE}/sdk/src/main/jni/mega/sdk
                git rev-parse HEAD
                """,
            returnStdout: true).trim()
    println("sdk commit id = ${commitId}")
    return commitId
}


private String appCommitId() {
    String commitId = sh(
            script: """
                cd ${WORKSPACE}
                git rev-parse HEAD
                """,
            returnStdout: true).trim()
    println("Android commit id = ${commitId}")
    return commitId
}

private String megaChatSdkCommitId() {
    String commitId = sh(
            script: """
                cd ${WORKSPACE}/sdk/src/main/jni/megachat/sdk
                git rev-parse HEAD
                """,
            returnStdout: true).trim()
    println("chat sdk commit id = ${commitId}")
    return commitId
}

/**
 * Generate a message with all key release information. This message can be posted to MR and then
 * directly published by Release Process.
 * @return the composed message
 */
private String getBuildVersionInfo() {

    String artifactoryUrl = "${ARTIFACTORY_BASE_URL}/${artifactoryUploadPath()}"
    String artifactVersion = readAppVersion2()

    String gmsAabUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.aab"
    String gmsApkUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.apk"

    String appCommitLink = "https://code.developers.mega.co.nz/mobile/android/android/-/commit/" + appCommitId()
    String sdkCommitLink = "https://code.developers.mega.co.nz/sdk/sdk/-/commit/" + sdkCommitId()
    String chatCommitLink = "https://code.developers.mega.co.nz/megachat/MEGAchat/-/commit/" + megaChatSdkCommitId()

    String appBranch = env.gitlabSourceBranch
    String sdkBranch = SDK_BRANCH
    String chatBranch = MEGACHAT_BRANCH

    def message = """
    Version: ${readAppVersion1()} <br/>
    App Bundles and APKs: <br/>
       - Google (GMS):  [AAB](${gmsAabUrl}) | [APK](${gmsApkUrl}) <br/>
    Build info: <br/>
       - [Android commit](${appCommitLink}) (`${appBranch}`) <br/>
       - [SDK commit](${sdkCommitLink}) (`${sdkBranch}`) <br/>
       - [Karere commit](${chatCommitLink}) (`${chatBranch}`) <br/>
    """
    return message
}


/**
 * create a build info file with key version information of build.
 * This file will be uploaded to Artifactory repo.
 *
 */
def createBriefBuildInfoFile() {
    def content = """
Version: v${readAppVersion1()}
Upload Time: ${new Date().toString()}
Android: branch(${env.gitlabSourceBranch}) - commit(${appCommitId()})
SDK: branch(${SDK_BRANCH}) - commit(${sdkCommitId()})
Karere: branch(${MEGACHAT_BRANCH}) - commit(${megaChatSdkCommitId()})
"""
    sh "rm -fv ${ARTIFACTORY_BUILD_INFO}"
    sh "echo \"${content}\" >> ${WORKSPACE}/${ARCHIVE_FOLDER}/${ARTIFACTORY_BUILD_INFO}"
}

private String skipMessage(String lineBreak) {
    return ":raising_hand: Android Release Upload skipped!" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Build can only be triggered on release branch by MR command."
}

/**
 * Read SDK versions from MR description and assign the values into environment.
 */
private void checkSDKVersion() {
    SDK_BRANCH = getValueInMRDescriptionBy("SDK_BRANCH")
    if (!isDefined(SDK_BRANCH)) {
        SDK_BRANCH = "develop"
    }

    MEGACHAT_BRANCH = getValueInMRDescriptionBy("MEGACHAT_BRANCH")
    if (!isDefined(MEGACHAT_BRANCH)) {
        MEGACHAT_BRANCH = "develop"
    }
}

/**
 * read the version name and version code from source code(build.gradle)
 * @return a tuple of version code and version name
 */
def readAppVersion() {
    String versionCode = sh(script: "grep versionCode build.gradle | awk -F= '{print \$2}'", returnStdout: true).trim()
    String versionName = sh(script: "grep appVersion build.gradle | awk -F= '{print \$2}'", returnStdout: true).trim().replaceAll("\"", "")
    return [versionName, versionCode]
}

/**
 * get app version in a format like "6.6(433)"
 * @return version string
 */
private String readAppVersion1() {
    def (versionName, versionCode) = readAppVersion()
    return versionName + "(" + versionCode + ")"
}

/**
 * get app version in a format like "437_6_9" (for v6.9(437))
 * @return version string
 */
private String readAppVersion2() {
    def (versionName, versionCode) = readAppVersion()
    return "${versionCode}_${versionName.replaceAll("\\.", "_")}"
}

/**
 * read the last git commit message
 * @return last git commit message
 */
private String lastCommitMessage() {
    return sh(script: "git log --pretty=format:\"%x09%s\" -1", returnStdout: true).trim()
}

/**
 * Check if the branch is a release branch
 * @return true if triggered branch is 'release/*', otherwise return false.
 */
private boolean isOnReleaseBranch() {
    return env.gitlabSourceBranch != null && env.gitlabSourceBranch.startsWith("release/")
}

/**
 * Check if build is triggered by 'deliver_appStore' command.
 * @return true if build is triggered by 'deliver_appStore' command. Otherwise return false.
 */
private boolean triggeredByDeliverAppStore() {
    return isOnReleaseBranch() &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase == "deliver_appStore"
}

/**
 * Check if build is triggered by 'upload_symbol' command.
 * @return true if build is triggered by 'upload_symbol' command. Otherwise return false.
 */
private boolean triggeredByUploadSymbol() {
    return isOnReleaseBranch() &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase == "upload_symbol"
}

private void deleteAllFilesExcept(String folder, String except) {
    println("Deleting all files except ${except} in folder ${folder}")
    sh """
        cd ${folder}
        mv -v ${except} /tmp/
        rm -fr *
        mv -v /tmp/${except} .
    """
}

/**
 * download jenkins build console log and save to file.
 */
private void downloadJenkinsConsoleLog(String downloaded) {
    withCredentials([usernameColonPassword(credentialsId: 'Jenkins-Login', variable: 'CREDENTIALS')]) {
        sh "curl -u $CREDENTIALS ${BUILD_URL}/consoleText -o ${downloaded}"
    }
}

/**
 * upload file to GitLab and return the GitLab link
 * @param fileName the local file to be uploaded
 * @return file link on GitLab
 */
private String uploadFileToGitLab(String fileName) {
    String link = ""
    withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
        // upload Jenkins console log to GitLab and get download link
        final String response = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${fileName} https://code.developers.mega.co.nz/api/v4/projects/199/uploads", returnStdout: true).trim()
        link = new groovy.json.JsonSlurperClassic().parseText(response).markdown
        return link
    }
    return link
}

/**
 * get relative path of artifactory folder
 * @return relative path.
 */
private String artifactoryUploadPath() {
    def (versionName, versionCode) = readAppVersion()
    return "v${versionName}/${versionCode}"
}


/**
 * clean SDK
 */
private void cleanSdk() {
    println("clean SDK")
    sh """
        cd $WORKSPACE/sdk/src/main/jni
        bash build.sh clean
    """
}

/**
 * clean Android project
 */
private void cleanAndroid() {
    println("clean Android code")
    sh """
        cd $WORKSPACE
        ./gradlew clean
    """
}

/**
 * print the size of workspace.
 * @param prompt a prompt message can be printed before the size value.
 */
private void printWorkspaceSize(String prompt) {
    println(prompt)
    sh """
        cd ${WORKSPACE}
        du -sh
    """
}

/**
 * Get the list of recent changes (release note) json string input
 * and return a formatted list following below example
 *[
 * [language: 'en-GB', text: "Please test the changes from Jenkins build ${env.BUILD_NUMBER}."],
 * [language: 'de-DE', text: "Bitte die Änderungen vom Jenkins Build ${env.BUILD_NUMBER} testen."]
 *]
 *
 * @param input the json string to parse
 * @return the list of recent changes formatted
 */
def getRecentChangeList(input) {
    def map = []
    def languages = new groovy.json.JsonSlurperClassic().parseText(input)
    def keyList = languages.keySet()
    keyList.each { language ->
        def languageMap = [:]
        languageMap["language"] = "${language}"
        languageMap["text"] = "${languages[language]}"
        map.add(languageMap)
    }
    return map
}

/**
 * Get release notes content from releaseNoteFile
 * releaseNoteFile should be in json format
 *
 * @return a String with the content of releaseNoteFile
 */
private String releaseNotes(releaseNoteFile) {
    String release_notes = sh(
            script: """
                cd ${WORKSPACE}/jenkinsfile/
                cat $releaseNoteFile
                """,
            returnStdout: true).trim()
    return release_notes
}


