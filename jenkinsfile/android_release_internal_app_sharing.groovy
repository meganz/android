/**
 *  This script is to build and upload Android AAB to Google Play Store Internal App Sharing
 */

BUILD_STEP = ''

/**
 * Folder to contain build outputs, including APK, AAB and symbol files
 */
ARCHIVE_FOLDER = "archive"
ARTIFACTORY_BUILD_INFO = "buildinfo.txt"

/**
 * common.groovy file with common methods
 */
def common

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
        JAVA_HOME = '/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx'
        ANDROID_HOME = '/opt/buildtools/android-sdk'

        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        CONSOLE_LOG_FILE = 'console.txt'

        // CD pipeline uses this environment variable to assign version code
        APK_VERSION_CODE_FOR_CD = "${new Date().format('yyDDDHHmm', TimeZone.getTimeZone("GMT"))}"

        // Channel to add in the suffix of the version name
        APK_VERSION_NAME_CHANNEL_FOR_CD = "-internal-appsharing"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
    }
    post {
        failure {
            script {
                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                if (hasGitLabMergeRequest()) {
                    String link = uploadFileToGitLab(CONSOLE_LOG_FILE)

                    def message = ""
                    if (triggeredByDeliverInternalAppSharing()) {
                        message = common.releaseFailureMessage("<br/>") +
                                "<br/>Build Log:\t${link}"
                    }
                    common.sendToMR(message)
                } else {
                    slackSend color: 'danger', message: common.releaseFailureMessage("\n")
                    slackUploadFile filePath: CONSOLE_LOG_FILE, initialComment: 'Jenkins Log'
                }
            }
        }
        success {
            script {
                if (hasGitLabMergeRequest()) {
                    common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                    String link = uploadFileToGitLab(CONSOLE_LOG_FILE)

                    if (triggeredByDeliverInternalAppSharing()) {
                        def message = releaseSuccessMessage("<br/>", common) +
                                "<br/>Build Log:\t${link}"
                        common.sendToMR(message)

                        common.sendToMR(getBuildVersionInfo(common))

                        slackSend color: "good", message: releaseSuccessMessage("\n", common)
                    }
                }
            }
        }
        cleanup {
            // delete whole workspace after each build, to save Jenkins storage
            cleanWs(cleanWhenFailure: true)
        }
    }
    stages {
        stage('Load Common Script') {
            steps {
                script {
                    BUILD_STEP = 'Load Common Script'

                    // load the common library script
                    common = load('jenkinsfile/common.groovy')
                }
            }
        }
        stage('Preparation') {
            when {
                expression { triggeredByDeliverInternalAppSharing() }
            }
            steps {
                script {
                    BUILD_STEP = 'Preparation'

                    // send command acknowledgement to MR
                    common.sendToMR(":runner: Android CD Release to Internal App Sharing pipeline has started!!!" +
                            "<br/><b>Command</b>: ${env.gitlabTriggerPhrase}"
                    )

                    sh("rm -frv $ARCHIVE_FOLDER")
                    sh("mkdir -p ${WORKSPACE}/${ARCHIVE_FOLDER}")
                    sh("rm -fv ${CONSOLE_LOG_FILE}")
                    sh('set')
                }
            }
        }
        stage('Apply Google Map API Key') {
            when {
                expression { triggeredByDeliverInternalAppSharing() }
            }
            steps {
                script {
                    BUILD_STEP = 'Apply Google Map API Key'
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
        stage('Enable Permanent Logging') {
            steps {
                script {
                    BUILD_STEP = 'Enable Permanent Logging'

                    def featureFlagFile = "app/src/main/assets/featuretoggle/feature_flags.json"
                    common.setFeatureFlag(featureFlagFile, "PermanentLogging", true)
                    sh("cat $featureFlagFile")
                }
            }
        }
        stage('Build GMS APK') {
            when {
                expression { triggeredByDeliverInternalAppSharing() }
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
                expression { triggeredByDeliverInternalAppSharing() }
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
                        String gmsApkOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${common.readAppVersion2()}-gms-release.apk"
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
                expression { triggeredByDeliverInternalAppSharing() }
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
                expression { triggeredByDeliverInternalAppSharing() }
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
                        String gmsAabOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${common.readAppVersion2()}-gms-release.aab"
                        println("input = $gmsAabInput \noutput = $gmsAabOutput")
                        withEnv([
                                "GMS_AAB_INPUT=${gmsAabInput}",
                                "GMS_AAB_OUTPUT=${gmsAabOutput}"
                        ]) {
                            sh('jarsigner -keystore ${ANDROID_PRD_GMS_AAB_KEYSTORE} -storepass "${ANDROID_PRD_GMS_AAB_PASSWORD}" -signedjar ${GMS_AAB_OUTPUT} ${GMS_AAB_INPUT} megaandroid-upload')
                        }
                        println("Finish signing GMS AAB. ($gmsAabOutput) generated!")
                    }
                }
            }
        }
        stage('Archive files') {
            when {
                expression { triggeredByDeliverInternalAppSharing() }
            }
            steps {
                script {
                    BUILD_STEP = 'Archive files'
                    println("Uploading files to Artifactory repo....")

                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN')
                    ]) {

                        String targetPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/internal-appsharing/${common.artifactoryUploadPath()}/"

                        withEnv([
                                "TARGET_PATH=${targetPath}"
                        ]) {
                            common.createBriefBuildInfoFile()

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
        stage('Deploy to Google Play Internal App Sharing') {
            when {
                expression { triggeredByDeliverInternalAppSharing() }
            }
            steps {
                script {
                    BUILD_STEP = 'Deploy to Google Play Internal App Sharing'

                    // Upload the AAB to Google Play
                    androidApkUpload googleCredentialsId: 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL',
                            filesPattern: 'archive/*-gms-release.aab',
                            trackName: 'internal-app-sharing'
                }
            }
        }
    }
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
 * compose the success message of "deliver_internalAppSharing" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @param common The common functions loaded from common.groovy
 * @return The success message to be sent
 */
private String releaseSuccessMessage(String lineBreak, Object common) {
    return ":rocket: Android Release uploaded to Google Play Internal App Sharing successfully!" +
            "${lineBreak}Version:\t${common.readAppVersion1()}" +
            "${lineBreak}Last Commit Msg:\t${common.lastCommitMessage()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

/**
 * Generate a message with all key release information. This message can be posted to MR and then
 * directly published by Release Process.
 * @param common The common functions loaded from common.groovy
 * @return the composed message
 */
private String getBuildVersionInfo(Object common) {
    println("entering getBuildVersionInfo")
    String artifactoryUrl = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/internal-appsharing/${common.artifactoryUploadPath()}"
    String artifactVersion = common.readAppVersion2()

    String gmsAabUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.aab"
    String gmsApkUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.apk"

    String sdkVersion = common.readPrebuiltSdkVersion()
    String appCommitLink = "${env.GITLAB_BASE_URL}/mobile/android/android/-/commit/" + common.appCommitId()
    String sdkCommitLink = "${env.GITLAB_BASE_URL}/sdk/sdk/-/commit/" + common.queryPrebuiltSdkProperty("sdk-commit", sdkVersion)
    String chatCommitLink = "${env.GITLAB_BASE_URL}/megachat/MEGAchat/-/commit/" + common.queryPrebuiltSdkProperty("chat-commit", sdkVersion)

    String appBranch = env.gitlabSourceBranch

    def message = """
    Internal App Sharing Version: ${common.readAppVersion1()} <br/>
    Internal App Sharing URL: ${getInternalAppSharingUrl()} <br/>
    Internal App Sharing Bundles and APKs: <br/>
       - Google (GMS):  [AAB](${gmsAabUrl}) | [APK](${gmsApkUrl}) <br/>
    Internal App Sharing Build info: <br/>
       - [Android commit](${appCommitLink}) (`${appBranch}`) <br/>
       - [SDK commit](${sdkCommitLink}) (`${common.queryPrebuiltSdkProperty("sdk-branch", sdkVersion)}`) <br/>
       - [Karere commit](${chatCommitLink}) (`${common.queryPrebuiltSdkProperty("chat-branch", sdkVersion)}`) <br/>
    """
    return message
}

private String getInternalAppSharingUrl() {
    println("Entering getInternalSharingUrl()")
    String internalAppSharingUrl = sh(script: "grep 'play.google.com' ${CONSOLE_LOG_FILE}", returnStdout: true).trim()
    if (!internalAppSharingUrl.isEmpty()) {
        return internalAppSharingUrl
    }
    return "Internal-AppSharing Url Not available"
}

/**
 * Check if the branch is a release branch
 * @return true if triggered branch is 'release/*', otherwise return false.
 */
private boolean isOnReleaseBranch() {
    return env.gitlabSourceBranch != null && env.gitlabSourceBranch.startsWith("release/")
}

/**
 * Check if build is triggered by 'deliver_internalAppSharing' command.
 * @return true if build is triggered by 'deliver_internalAppSharing' command. Otherwise return false.
 */
private boolean triggeredByDeliverInternalAppSharing() {
    return !isOnReleaseBranch() &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase == "deliver_internalAppSharing"
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
        final String response = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${fileName} ${env.GITLAB_BASE_URL}/api/v4/projects/199/uploads", returnStdout: true).trim()
        link = new groovy.json.JsonSlurperClassic().parseText(response).markdown
        return link
    }
    return link
}

