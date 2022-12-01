/**
 * This script serves 2 purposes:
 * 1. Build and upload Android APK to Firebase AppDistribution
 * 2. Build SDK and publish to Artifactory
 */
import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput

BUILD_STEP = ''

// Below values will be read from MR description and are used to decide SDK versions
SDK_BRANCH = 'develop'
MEGACHAT_BRANCH = 'develop'
SDK_COMMIT = ""
MEGACHAT_COMMIT = ""
SDK_TAG = ""
MEGACHAT_TAG = ""

/**
 * GitLab commands that can trigger this job.
 */
DELIVER_QA_CMD = "deliver_qa"
PUBLISH_SDK_CMD = "publish_sdk"

// The log file of publishing pre-built SDK to Artifatory
ARTIFACTORY_PUBLISH_LOG = "artifactory_publish.log"

/**
 * common.groovy file with common methods
 */
def common

/**
 * Flag to decide whether we do clean before build SDK.
 * Possible values: yes|no
 */
REBUILD_SDK = "no"

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }
    options {
        // Stop the build early in case of compile or test failures
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '1'))
        timeout(time: 1, unit: 'HOURS')
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

        APK_VERSION_NAME_FOR_CD = "_${new Date().format('MMddHHmm')}"

        // SDK build log. ${LOG_FILE} will be used by build.sh to export SDK build log.
        SDK_LOG_FILE_NAME = "sdk_build_log.txt"
        LOG_FILE = "${WORKSPACE}/${SDK_LOG_FILE_NAME}"
    }
    post {
        failure {
            script {

                common = load('jenkinsfile/common.groovy')

                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                if (triggerByDeliverQaCmd() || triggerByDeliverQaCmd()) {
                    if (common.hasGitLabMergeRequest()) {
                        // upload Jenkins console log
                        String jsonJenkinsLog = uploadFileToGitLab(CONSOLE_LOG_FILE)

                        String message = firebaseUploadFailureMessage("<br/>") +
                                "<br/>Build Log:\t${jsonJenkinsLog}"

                        common.sendToMR(message)
                    } else {
                        // if build is triggered by PUSH, send result only to Slack
                        slackSend color: 'danger', message: firebaseUploadFailureMessage("\n")
                        slackUploadFile filePath: 'console.txt', initialComment: 'Jenkins Log'
                    }
                } else if (triggerByPublishSdkCmd()) {
                    // upload Jenkins console log
                    String jsonJenkinsLog = uploadFileToGitLab(CONSOLE_LOG_FILE)

                    // upload SDK build log if SDK build fails
                    String sdkBuildMessage = ""
                    if (BUILD_STEP == "Build SDK") {
                        if (fileExists(SDK_LOG_FILE_NAME)) {
                            def jsonSdkLog = uploadFileToGitLab(SDK_LOG_FILE_NAME)
                            sdkBuildMessage = "<br/>SDK BuildLog:\t${jsonSdkLog}"
                        } else {
                            sdkBuildMessage = "<br/>SDK Build log not valid"
                        }
                    }

                    String message = publishSdkFailureMessage("<br/>") +
                            "<br/>Build Log:\t${jsonJenkinsLog}" +
                            sdkBuildMessage

                    common.sendToMR(message)

                    slackSend color: 'danger', message: publishSdkFailureMessage("\n")
                    slackUploadFile filePath: 'console.txt', initialComment: 'Jenkins Log'
                }
            }
        }
        success {
            script {
                common = load('jenkinsfile/common.groovy')

                if (triggerByDeliverQaCmd() || triggerByPush()) {
                    slackSend color: "good", message: firebaseUploadSuccessMessage("\n")
                    common.sendToMR(firebaseUploadSuccessMessage("<br/>"))
                } else if (triggerByPublishSdkCmd()) {
                    slackSend color: "good", message: publishSdkSuccessMessage("\n")
                    common.sendToMR(publishSdkSuccessMessage("<br/>"))
                }
            }
        }
        cleanup {
            // delete whole workspace after each successful build, to save Jenkins storage
            // We do not clean workspace if build fails, for a chance to investigate the crime scene.
            cleanWs(cleanWhenFailure: false)
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
                expression { triggerByPush() || triggerByDeliverQaCmd() || triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Preparation'
                    checkSDKVersion()
                    REBUILD_SDK = getValueInMRDescriptionBy("REBUILD_SDK")
                }
                gitlabCommitStatus(name: 'Preparation') {
                    sh("rm -fv ${CONSOLE_LOG_FILE}")
                    sh("rm -fv ${LOG_FILE}")  // sdk log file
                    sh('set')
                }
            }
        }
        stage('Fetch SDK Submodules') {
            when {
                expression { triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Fetch SDK Submodules'

                    common.fetchSdkSubmodules()
                }
            }
        }
        stage('Select SDK Version') {
            when {
                expression { triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Select SDK Version'
                }
                gitlabCommitStatus(name: 'Select SDK Version') {
                    withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
                        script {
                            String sdkCommit = parseCommandParameter()["sdk-commit"]
                            if (sdkCommit != null && sdkCommit.length() > 0) {
                                common.checkoutSdkByCommit(sdkCommit)
                            }

                            String chatCommit = parseCommandParameter()["chat-commit"]
                            if (chatCommit != null && chatCommit.length() > 0) {
                                common.checkoutMegaChatSdkByCommit(chatCommit)
                            }
                        }
                    }
                }
            }
        }

        stage('Download Dependency Lib for SDK') {
            when {
                expression { triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Download Dependency Lib for SDK'
                }
                gitlabCommitStatus(name: 'Download Dependency Lib for SDK') {
                    script {
                        sh """
                            cd "${WORKSPACE}/jenkinsfile/"
                            bash download_webrtc.sh
    
                            mkdir -p "${BUILD_LIB_DOWNLOAD_FOLDER}"
                            cd "${BUILD_LIB_DOWNLOAD_FOLDER}"
                            pwd
                            ls -lh
                        """
                    }
                }
            }
        }
        stage('Download Google Map API Key') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Download Google Map API Key'
                }
                gitlabCommitStatus(name: 'Download Google Map API Key') {

                    withCredentials([
                            file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_QA', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_QA')
                    ]) {
                        script {
                            println("applying production google map api config... ")
                            sh 'mkdir -p app/src/debug/res/values'
                            sh 'mkdir -p app/src/release/res/values'
                            sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_QA} app/src/debug/res/values/google_maps_api.xml"
                            sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_QA} app/src/release/res/values/google_maps_api.xml"
                        }
                    }
                }
            }
        }

        stage('Build SDK') {
            when {
                expression { triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build SDK'
                    String buildArchs = "x86 armeabi-v7a x86_64 arm64-v8a"
                    withEnv(["BUILD_ARCHS=${buildArchs}"]) {
                        sh """
                                cd ${WORKSPACE}/sdk/src/main/jni
                                echo CLEANING SDK
                                bash build.sh clean
                            
                                echo "=== START SDK BUILD===="
                                bash build.sh all
                            """
                    }
                }

            }
        }
        stage('Publish SDK to Artifactory') {
            when {
                expression { triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Publish SDK to Artifactory'

                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN'),
                    ]) {
                        withEnv([
                                "ARTIFACTORY_USER=${ARTIFACTORY_USER}",
                                "ARTIFACTORY_ACCESS_TOKEN=${ARTIFACTORY_ACCESS_TOKEN}",
                                "SDK_PUBLISH_TYPE=${getSdkPublishType()}",
                                "SDK_COMMIT=${getSdkGitHash()}",
                                "CHAT_COMMIT=${getMegaChatSdkGitHash()}"
                        ]) {
                            sh """
                                cd ${WORKSPACE}
                                ./gradlew sdk:artifactoryPublish 2>&1  | tee ${ARTIFACTORY_PUBLISH_LOG}
                            """
                        }
                    }
                }
            }
        }
        stage('Clean Android build') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Clean Android'
                    sh './gradlew clean'
                }
            }
        }
        stage('Enable Permanent Logging') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Enable Permanent Logging'

                    def featureFlagFile = "app/src/main/assets/featuretoggle/feature_flags.json"
                    setFeatureFlag(featureFlagFile, "PermanentLogging", true)
                    sh("cat $featureFlagFile")
                }
            }
        }
        stage('Build APK(GMS)') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build APK (GMS)'
                }

                gitlabCommitStatus(name: 'Build APK (GMS)') {
                    script {
                        sh './gradlew app:assembleGmsRelease'
                    }
                }
            }
        }
        stage('Sign APK(GMS)') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Sign APK(GMS)'
                }
                gitlabCommitStatus(name: 'Sign APK(GMS)') {
                    withCredentials([
                            string(credentialsId: 'ANDROID_QA_SIGN_PASSWORD', variable: 'ANDROID_QA_SIGN_PASSWORD'),
                            file(credentialsId: 'ANDROID_QA_KEYSTORE', variable: 'ANDROID_QA_KEYSTORE')
                    ]) {
                        script {
                            sh '''
                                cd app/build/outputs/apk/gms/release
                                zipalign -v -p 4 app-*-unsigned.apk app-gms-release-unsigned-aligned.apk
                                apksigner sign --ks "$ANDROID_QA_KEYSTORE" --ks-pass "pass:$ANDROID_QA_SIGN_PASSWORD" --out app-gms-release-signed.apk app-gms-release-unsigned-aligned.apk
                                ls -lh
                                rm -fv *unsigned*.apk
                                pwd
                                ls -lh
                                cd -
                            '''
                        }
                    }
                }
            }
        }
        stage('Upload APK(GMS) to Firebase') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Upload APK(GMS) to Firebase'
                }
                withCredentials([
                        file(credentialsId: 'android_firebase_credentials', variable: 'FIREBASE_CONFIG')
                ]) {
                    script {
                        withEnv([
                                "GOOGLE_APPLICATION_CREDENTIALS=$FIREBASE_CONFIG",
                                "RELEASE_NOTES_FOR_CD=${readReleaseNotes()}",
                                "TESTERS_FOR_CD=${parseCommandParameter()["tester"]}",
                                "TESTER_GROUP_FOR_CD=${parseCommandParameter()["tester-group"]}"
                        ]) {
                            println("Upload GMS APK, TESTERS_FOR_CD = ${env.TESTERS_FOR_CD}")
                            println("Upload GMS APK, RELEASE_NOTES_FOR_CD = ${env.RELEASE_NOTES_FOR_CD}")
                            sh './gradlew appDistributionUploadGmsRelease'
                        }
                    }
                }
            }
        }
        stage('Build QA APK(GMS)') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build QA APK(GMS)'
                    withEnv([
                            "APK_VERSION_NAME_FOR_CD=${APK_VERSION_NAME_FOR_CD}_QA"
                    ]) {
                        sh './gradlew app:assembleGmsQa'
                    }
                }
            }
        }

        stage('Upload QA APK(GMS) to Firebase') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Upload QA APK(GMS) to Firebase'
                }
                withCredentials([
                        file(credentialsId: 'android_firebase_credentials', variable: 'FIREBASE_CONFIG')
                ]) {
                    script {
                        withEnv([
                                "GOOGLE_APPLICATION_CREDENTIALS=$FIREBASE_CONFIG",
                                "RELEASE_NOTES_FOR_CD=${readReleaseNotes()}",
                                "TESTERS_FOR_CD=${parseCommandParameter()["tester"]}",
                                "TESTER_GROUP_FOR_CD=${parseCommandParameter()["tester-group"]}"
                        ]) {
                            sh './gradlew appDistributionUploadGmsQa'
                        }
                    }
                }
            }
        }

        stage('Clean up Android') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() || triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Clean Up Android'
                }
                gitlabCommitStatus(name: 'Clean Up Android') {
                    sh """                    
                        cd ${WORKSPACE}
                        ./gradlew clean
                    """
                }
            }
        }
        stage('Clean up SDK') {
            when {
                expression { triggerByPush() || triggerByDeliverQaCmd() || triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Clean Up SDK'
                }
                gitlabCommitStatus(name: 'Clean Up SDK') {
                    sh """
                        cd ${WORKSPACE}/sdk/src/main/jni
                        bash build.sh clean
                    """
                }
            }
        }
    }
}

/**
 * Create the build report of failed Firebase Upload
 *
 * @param lineBreak the line break used between the lines. For GitLab and Slack, different line break
 * can be provided. GitLab accepts HTML "<BR/>", and Slack accepts "\n"
 * @return failure message
 */
private String firebaseUploadFailureMessage(String lineBreak) {
    String message = ":x: Android Firebase Upload Build Failed!(BuildNumber: ${env.BUILD_NUMBER})" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
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
 * Create the build report of failed prebuilt SDK build
 *
 * @param lineBreak the line break used between the lines. For GitLab and Slack, different line break
 * can be provided. GitLab accepts HTML "<BR/>", and Slack accepts "\n"
 * @return failure message
 */
private String publishSdkFailureMessage(String lineBreak) {
    String message = ":x: Prebuilt SDK Creation Failed!(BuildNumber: ${env.BUILD_NUMBER})" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Trigger Reason:\t${gitlabTriggerPhrase}"
    return message
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
 * checkout SDK by git tag
 * @param sdkTag the tag to checkout
 */
private void checkoutSdkByTag(String sdkTag) {
    sh """
    echo checkoutSdkByTag
    cd $WORKSPACE
    cd sdk/src/main/jni/mega/sdk
    git checkout tags/$sdkTag
    cd $WORKSPACE
    """
}

/**
 * checkout MEGAchat SDK by git tag
 * @param megaChatTag the tag to checkout
 */
private void checkoutMegaChatSdkByTag(String megaChatTag) {
    sh """
    echo checkoutMegaChatSdkByTag
    cd $WORKSPACE
    cd sdk/src/main/jni/megachat/sdk
    git checkout tags/$megaChatTag
    cd $WORKSPACE
    """
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
 * compose the success message, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
private String firebaseUploadSuccessMessage(String lineBreak) {
    return ":rocket: Android APK Build uploaded successfully to Firebase AppDistribution!(${env.BUILD_NUMBER})" +
            "${lineBreak}Version:\t${readAppVersion()}${APK_VERSION_NAME_FOR_CD}" +
            "${lineBreak}Last Commit Msg:\t${lastCommitMessage()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}" +
            "${lineBreak}Trigger Reason: ${getTriggerReason()}"
}

/**
 * compose the success message, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
private String publishSdkSuccessMessage(String lineBreak) {
    common = load('jenkinsfile/common.groovy')
    return ":rocket: Prebuilt SDK is published to Artifactory Successfully!(${env.BUILD_NUMBER})" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}SDK Commit:\t${getSdkGitHash()}" +
            "${lineBreak}Chat SDK Commit:\t${getMegaChatSdkGitHash()}" +
            "${lineBreak}Version:\tnz.mega.sdk:sdk:${getSdkVersionText()}" +
            "${lineBreak}Trigger Reason:\t${gitlabTriggerPhrase}" +
            "${lineBreak}AAR Artifactory Page: ${getSdkAarArtifactoryPage()}"
}

/**
 * Check this build is triggered by PUSH to a branch
 * @return
 */
private boolean triggerByPush() {
    return env.gitlabActionType == "PUSH"
}

/**
 * Check if this build is triggered by a deliver_qa command
 * @return
 */
private boolean triggerByDeliverQaCmd() {
    return env.gitlabActionType == "NOTE" &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase.startsWith(DELIVER_QA_CMD)
}

/**
 * Check if this build is triggered by a publish_sdk command
 * @return
 */
private boolean triggerByPublishSdkCmd() {
    return env.gitlabActionType == "NOTE" &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase.startsWith(PUBLISH_SDK_CMD)
}

/**
 * get trigger reason
 * @return description for the trigger reason
 */
private String getTriggerReason() {
    if (env.gitlabActionType == "PUSH") {
        return "git PUSH into develop branch"
    } else if (env.gitlabActionType == "NOTE") {
        return "Manually by comment in GitLab MR(${gitlabTriggerPhrase})"
    } else {
        return "Other reasons${env.gitlabActionType}"
    }
}

/**
 * Parse the parameter of command that triggers this build task. Both 'deliver_qa' and 'publish_sdk'
 * are supported. Command examples:
 * "deliver_qa --tester tester1@gmail.com,tester2@gmail.com --tester-group internal_dev,other_group --notes AND-99999 this build fixes the problem of layout in xxx page"
 * "publish_sdk --type rel --sdk-commit 12345 --chat-commit 0987656"
 *
 * @return a map of the parsed parameters and values. Below parameters should be included.
 * For 'deliver_qa' command
 *     key "tester" - list of tester emails, separated by comma
 *     key "notes" - developer specified release notes.
 *     key "tester-group" - developer specified tester group, separated by comma
 *     If deliver_qa command is issued without parameters, then values of above keys are empty.
 * For 'publish_sdk' command
 *    key "sdk-type" - sdk build type. Possible values: "dev" or "rel"
 *    key "sdk-commit" - MEGA SDK commit SHA-1. Can be short or long format.
 *    key "chat-commit" - MEGAChat SDK commit SHA-1. Can be short or long format.
 *    If publish_sdk command is issued without parameters, then "sdk-type" returns "dev"
 *    , "sdk-commit" and "chat-commit" are empty.
 */
def parseCommandParameter() {
    // parameters in deliver_qa command
    final PARAM_NOTES = "--notes"

    // key in the returned dictionary - delivery_qa command
    final KEY_TESTER = "tester"
    final KEY_NOTES = "notes"
    final KEY_TESTER_GROUP = "tester-group"

    def result = [:]
    result[KEY_TESTER] = ""
    result[KEY_NOTES] = ""
    result[KEY_TESTER_GROUP] = ""

    String fullCommand = env.gitlabTriggerPhrase
    println("[DEBUG] parsing command parameters. \nuser input: $fullCommand")

    String command
    if (triggerByDeliverQaCmd()) {
        command = DELIVER_QA_CMD
    } else if (triggerByPublishSdkCmd()) {
        command = PUBLISH_SDK_CMD
    } else {
        return result
    }

    String params = fullCommand.substring(command.length()).trim()

    // get release notes param of deliver_qa command because it is always
    // the last parameter when it exists
    int notesPos = params.indexOf(PARAM_NOTES)
    if (notesPos >= 0) {
        String notes = params.substring(notesPos + PARAM_NOTES.length()).trim()
        result[KEY_NOTES] = notes
    }

    String otherParams
    if (notesPos >= 0) {
        otherParams = params.substring(0, notesPos).trim()
    } else {
        otherParams = params
    }

    if (otherParams.isEmpty()) {
        println("[DEBUG] parseCommandParameter() no extra params. Result = $result")
        return result
    }

    String[] paramList = otherParams.split(" +")

    if (paramList.length % 2 != 0) {
        println("[ERROR] invalid parameter in command! parameter name and values are not in pair.")
        println("[ERROR] parameter list = " + otherParams)
        sh("exit 1")
        return result
    }

    def counter = 0
    while (counter < paramList.length) {
        String word = paramList[counter]

        if (!word.startsWith("--")) {
            println("[ERROR] invalid parameter in command! Parameter not start with --")
            println("[ERROR] parsed parameters: $result")
            println("[ERROR] parameter \"$word\" is unknown!")
            sh("exit 1")
            return result
        }

        word = word.substring(2)
        String value = paramList[counter + 1]
        result[word] = value
        counter += 2
    }

    println("[DEBUG] parseParam params = $result")
    return result
}

String readReleaseNotes() {
    String baseRelNotes = "Triggered by: $gitlabUserName" +
            "\nTrigger Reason: ${getTriggerReason()}" +
            "\nBranch: $gitlabSourceBranch " +
            "\nLast 5 git commits:\n${sh(script: "git log --pretty=format:\"(%h,%an)%x09%s\" -5", returnStdout: true).trim()}"

    String customRelNotes = parseCommandParameter()["notes"]
    if (!customRelNotes.isEmpty()) {
        return customRelNotes + "\n" + baseRelNotes
    } else {
        return baseRelNotes
    }
}

/**
 * Read SDK versions from MR description and assign the values into environment.
 */
private void checkSDKVersion() {
    SDK_COMMIT = getValueInMRDescriptionBy("SDK_COMMIT")
    MEGACHAT_COMMIT = getValueInMRDescriptionBy("MEGACHAT_COMMIT")

    SDK_TAG = getValueInMRDescriptionBy("SDK_TAG")
    MEGACHAT_TAG = getValueInMRDescriptionBy("MEGACHAT_TAG")

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
 * read version name and version code from build.gradle
 * @return version name plus version code. Example: "6.6(433)"
 */
private String readAppVersion() {
    String versionCode = sh(script: "grep versionCode build.gradle | awk -F= '{print \$2}'", returnStdout: true).trim()
    String versionName = sh(script: "grep appVersion build.gradle | awk -F= '{print \$2}'", returnStdout: true).trim().replaceAll("\"", "")
    return versionName + "(" + versionCode + ")"
}

/**
 * read the last git commit message
 * @return last git commit message
 */
private String lastCommitMessage() {
    return sh(script: "git log --pretty=format:\"%x09%s\" -1", returnStdout: true).trim()
}

/**
 * upload file to GitLab and return the GitLab link
 * @param fileName the local file to be uploaded
 * @return file link on GitLab
 */
private String uploadFileToGitLab(String fileName) {
    String link = ""
    withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
        final String response = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${fileName} https://code.developers.mega.co.nz/api/v4/projects/199/uploads", returnStdout: true).trim()
        link = new groovy.json.JsonSlurperClassic().parseText(response).markdown
        return link
    }
    return link
}

/**
 * Get publish type of SDK.
 * @return return value can be either "dev" or "rel"
 */
private String getSdkPublishType() {
    String type = parseCommandParameter()["lib-type"]
    if (type == "rel") {
        return "rel"
    } else {
        return "dev"
    }
}

/**
 * Parse log file of publishing SDK to Artifactory maven repo
 * and return the new pre-built SDK version. For example:
 * "20221109.084452-rel"
 *
 * The version info is extracted from below line in the log:
 * "[pool-4-thread-1] Deploying artifact: https://artifactory.developers.mega.co.nz/artifactory/mega-gradle/mega-sdk-android/nz/mega/sdk/sdk/20221109.084452-rel/sdk-20221109.084452-rel.aar"
 *
 * @return the version text of the SDK that has just been published to Artifactory
 */
private String getSdkVersionText() {
    println("Entering getSdkVersionText()")

    String content = sh(script: "grep 'Deploying artifact' ${ARTIFACTORY_PUBLISH_LOG}", returnStdout: true).trim()
    String[] lines = content.split("\n")
    for (line in lines) {
        println("parsing line = $line")
        if (line.endsWith("aar")) {
            String version = line.substring(line.lastIndexOf("/sdk-") + 5, line.lastIndexOf("."))
            println("SDK version = $version")
            return version
        }
    }
    return "Invalid Sdk Version"
}

/**
 * Get Sdk AAR download link
 *
 * @return download link
 */
private String getSdkAarArtifactoryPage() {
    String version = getSdkVersionText()
    return "https://artifactory.developers.mega.co.nz/ui/repos/tree/Properties/mega-gradle/mega-sdk-android/nz/mega/sdk/sdk/${version}/sdk-${version}.aar"
}

/**
 * Get the short commit ID of SDK
 * @return short git commit ID
 */
String getSdkGitHash() {
    return sh(script: "cd $WORKSPACE/sdk/src/main/jni/mega/sdk && git rev-parse --short HEAD", returnStdout: true).trim()
}

/**
 * Get the short commit ID of mega chat SDK
 * @return short git commit ID
 */
String getMegaChatSdkGitHash() {
    return sh(script: "cd $WORKSPACE/sdk/src/main/jni/megachat/sdk && git rev-parse --short HEAD", returnStdout: true).trim()
}

/**
 *  Check the feature flag json file and set the feature flag
 *  If the feature_flag.json file already contains the flagName, set the flagValue.
 *  Otherwise add the flagName with specified flagValue.
 *  If featureFlagFile does not exist, a new file will be created.
 *
 * @param featureFlagFile relative path of the feature_flag.json file
 * @param flagName name of the feature flag
 * @param flagValue boolean value of the flag
 */
def setFeatureFlag(String featureFlagFile, String flagName, boolean flagValue) {
    def flagList
    if (fileExists(featureFlagFile)) {
        def fileContents = readFile(featureFlagFile)
        flagList = new JsonSlurperClassic().parseText(fileContents)
    } else {
        println("setFeatureFlag() $featureFlagFile not exist!")
        flagList = new ArrayList()
    }

    def exist = false
    for (feature in flagList) {
        def name = feature["name"]
        if (name == flagName) {
            feature["value"] = flagValue
            exist = true
            break
        }
    }

    if (!exist) {
        def newFeature = new HashMap<String, String>()
        newFeature["value"] = flagValue
        newFeature["name"] = flagName
        flagList.add(newFeature)
    }

    def result = JsonOutput.prettyPrint(JsonOutput.toJson(flagList))
    writeFile file: featureFlagFile, text: result.toString()
}
