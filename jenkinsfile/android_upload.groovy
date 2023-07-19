/**
 * This script serves 2 purposes:
 * 1. Build and upload Android APK to Firebase AppDistribution
 * 2. Build SDK and publish to Artifactory
 */

BUILD_STEP = ''

// Below values will be read from MR description and are used to decide SDK versions
SDK_BRANCH = 'develop'
MEGACHAT_BRANCH = 'develop'
SDK_COMMIT = ""
MEGACHAT_COMMIT = ""

APP_UNIT_TEST_SUMMARY = ""
DOMAIN_UNIT_TEST_SUMMARY = ""
DATA_UNIT_TEST_SUMMARY = ""

APP_COVERAGE = ""
DOMAIN_COVERAGE = ""
DATA_COVERAGE = ""

/**
 * Folder to contain build outputs, including APK, AAG and symbol files
 */
ARCHIVE_FOLDER = "archive"
NATIVE_SYMBOLS_FILE = "symbols.zip"

/**
 * GitLab commands that can trigger this job.
 */
DELIVER_QA_CMD = "deliver_qa"
PUBLISH_SDK_CMD = "publish_sdk"
UPLOAD_COVERAGE_REPORT_CMD = "upload_coverage"

// The log file of publishing pre-built SDK to Artifactory
ARTIFACTORY_PUBLISH_LOG = "artifactory_publish.log"

/**
 * common.groovy file with common methods
 */
def common

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
        JAVA_HOME = '/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx'
        ANDROID_HOME = '/opt/buildtools/android-sdk'

        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        CONSOLE_LOG_FILE = 'console.txt'

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'

        APK_VERSION_CODE_FOR_CD = "${new Date().format('yyDDDHHmm', TimeZone.getTimeZone("GMT"))}"

        // SDK build log. ${LOG_FILE} will be used by build.sh to export SDK build log.
        SDK_LOG_FILE_NAME = "sdk_build_log.txt"
        LOG_FILE = "${WORKSPACE}/${SDK_LOG_FILE_NAME}"
    }
    post {
        failure {
            script {
                common = load('jenkinsfile/common.groovy')

                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                if (triggerByDeliverQaCmd()) {
                    if (common.hasGitLabMergeRequest()) {
                        String jsonJenkinsLog = common.uploadFileToGitLab(CONSOLE_LOG_FILE)

                        String message = firebaseUploadFailureMessage("<br/>") +
                                "<br/>Build Log:\t${jsonJenkinsLog}"

                        common.sendToMR(message)
                    } else {
                        slackSend color: 'danger', message: firebaseUploadFailureMessage("\n")
                        slackUploadFile filePath: 'console.txt', initialComment: 'Jenkins Log'
                    }
                } else if (triggerByPushToDevelop()) {
                    slackSend color: 'danger', message: firebaseUploadFailureMessage("\n")
                    slackUploadFile filePath: 'console.txt', initialComment: 'Jenkins Log'
                } else if (triggerByPublishSdkCmd()) {
                    String jsonJenkinsLog = common.uploadFileToGitLab(CONSOLE_LOG_FILE)

                    // upload SDK build log if SDK build fails
                    String sdkBuildMessage = ""
                    if (BUILD_STEP == "Build SDK") {
                        if (fileExists(SDK_LOG_FILE_NAME)) {
                            def jsonSdkLog = common.uploadFileToGitLab(SDK_LOG_FILE_NAME)
                            sdkBuildMessage = "<br/>SDK BuildLog:\t${jsonSdkLog}"
                        } else {
                            sdkBuildMessage = "<br/>SDK Build log not available."
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

                if (triggerByDeliverQaCmd() || triggerByUploadCoverage()) {
                    slackSend color: "good", message: firebaseUploadSuccessMessage("\n")
                    common.sendToMR(firebaseUploadSuccessMessage("<br/>"))
                } else if (triggerByPushToDevelop()) {
                    slackSend color: "good", message: firebaseUploadSuccessMessage("\n")
                } else if (triggerByPublishSdkCmd()) {
                    slackSend color: "good", message: publishSdkSuccessMessage("\n")
                    common.sendToMR(publishSdkSuccessMessage("<br/>"))
                }
            }
        }
        cleanup {
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
                expression {
                    triggerByDeliverQaCmd() ||
                            triggerByPublishSdkCmd() ||
                            triggerByUploadCoverage() ||
                            triggerByPushToDevelop()
                }
            }
            steps {
                script {
                    BUILD_STEP = 'Preparation'
                    checkSDKVersion()

                    sh("rm -frv $ARCHIVE_FOLDER")
                    sh("mkdir -p ${WORKSPACE}/${ARCHIVE_FOLDER}")
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
                withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
                    script {
                        String sdkCommit = parseCommandParameter()["sdk-commit"]
                        if (sdkCommit != null && sdkCommit.length() > 0) {
                            common.checkoutSdkByCommit(sdkCommit)
                            SDK_BRANCH = "N/A"
                        }

                        String chatCommit = parseCommandParameter()["chat-commit"]
                        if (chatCommit != null && chatCommit.length() > 0) {
                            common.checkoutMegaChatSdkByCommit(chatCommit)
                            MEGACHAT_BRANCH = "N/A"
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

        stage('Download Google Map API Key') {
            when {
                expression { triggerByDeliverQaCmd() || triggerByUploadCoverage() || triggerByPushToDevelop() }
            }
            steps {
                script {
                    BUILD_STEP = 'Download Google Map API Key'
                }

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

        stage('Collect native symbol files') {
            when {
                expression { triggerByPublishSdkCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Collect native symbol files'

                    common.deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/arm64-v8a",
                            "libmega.so")
                    common.deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/armeabi-v7a/",
                            "libmega.so")
                    common.deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/x86",
                            "libmega.so")
                    common.deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/x86_64",
                            "libmega.so")

                    sh """
                        cd ${WORKSPACE}/sdk/src/main/obj/local
                        rm -fv */.DS_Store
                        rm -fv .DS_Store
                        zip -r ${NATIVE_SYMBOLS_FILE} .
                        mv -v ${NATIVE_SYMBOLS_FILE} ${WORKSPACE}/${ARCHIVE_FOLDER}
                    """
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
                        String targetPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/cicd/native-symbol/"
                        withEnv([
                                "ARTIFACTORY_USER=${ARTIFACTORY_USER}",
                                "ARTIFACTORY_ACCESS_TOKEN=${ARTIFACTORY_ACCESS_TOKEN}",
                                "SDK_PUBLISH_TYPE=${getSdkPublishType()}",
                                "SDK_COMMIT=${getSdkGitHash()}",
                                "CHAT_COMMIT=${getMegaChatSdkGitHash()}",
                                "SDK_BRANCH=${SDK_BRANCH}",
                                "MEGACHAT_BRANCH=${MEGACHAT_BRANCH}"
                        ]) {
                            sh """
                                cd ${WORKSPACE}
                                ./gradlew sdk:artifactoryPublish 2>&1  | tee ${ARTIFACTORY_PUBLISH_LOG}
                            """
                        }

                        sh "curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T \"${WORKSPACE}/${ARCHIVE_FOLDER}/${NATIVE_SYMBOLS_FILE}\" \"${targetPath}/${getSdkVersionText()}.zip\""
                    }
                }
            }
        }

        stage('Enable Permanent Logging') {
            when {
                expression { triggerByDeliverQaCmd() || triggerByPushToDevelop() }
            }
            steps {
                script {
                    BUILD_STEP = 'Enable Permanent Logging'

                    def featureFlagFile = "app/src/main/assets/featuretoggle/feature_flags.json"
                    common.setFeatureFlag(featureFlagFile, "PermanentLogging", true)
                    sh("cat $featureFlagFile")
                }
            }
        }
        stage('Build APK(GMS)') {
            when {
                expression { triggerByDeliverQaCmd() || triggerByPushToDevelop() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build APK (GMS)'
                    sh './gradlew app:assembleGmsRelease'
                }
            }
        }
        stage('Sign APK(GMS)') {
            when {
                expression { triggerByDeliverQaCmd() || triggerByPushToDevelop() }
            }
            steps {
                script {
                    BUILD_STEP = 'Sign APK(GMS)'
                }
                withCredentials([
                        file(credentialsId: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE', variable: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE'),
                        file(credentialsId: 'ANDROID_PRD_GMS_APK_KEYSTORE', variable: 'ANDROID_PRD_GMS_APK_KEYSTORE')
                ]) {
                    script {
                        sh """
                                cd app/build/outputs/apk/gms/release
                                zipalign -v -p 4 app-*-unsigned.apk app-gms-release-unsigned-aligned.apk
                                apksigner sign --ks "${ANDROID_PRD_GMS_APK_KEYSTORE}" --ks-pass file:"${ANDROID_PRD_GMS_APK_PASSWORD_FILE}" --out app-gms-release-signed.apk app-gms-release-unsigned-aligned.apk
                                ls -lh
                                rm -fv *unsigned*.apk
                                pwd
                                ls -lh
                                cd -
                            """
                    }
                }
            }
        }
        stage('Upload APK(GMS) to Firebase') {
            when {
                expression { triggerByDeliverQaCmd() || triggerByPushToDevelop() }
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
                expression { triggerByDeliverQaCmd() || triggerByPushToDevelop() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build QA APK(GMS)'
                    withEnv([
                            "APK_VERSION_NAME_TAG_FOR_CD=_QA"
                    ]) {
                        sh './gradlew app:assembleGmsQa'
                    }
                }
            }
        }

        stage('Upload QA APK(GMS) to Firebase') {
            when {
                expression { triggerByDeliverQaCmd() || triggerByPushToDevelop() }
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

        stage('Upload Code Coverage') {
            when {
                expression { triggerByUploadCoverage() }
            }
            steps {
                script {
                    BUILD_STEP = "Upload Code Coverage"

                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN')
                    ]) {
                        String targetPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/cicd/coverage/"
                        // domain coverage
                        sh "./gradlew domain:jacocoTestReport"

                        // data coverage
                        sh "./gradlew data:testGmsDebugUnitTestCoverage"

                        // run coverage for app module
                        sh "./gradlew app:createUnitTestCoverageReport"

                        APP_UNIT_TEST_SUMMARY = unitTestSummary("${WORKSPACE}/app/build/test-results/testGmsDebugUnitTest")
                        DOMAIN_UNIT_TEST_SUMMARY = unitTestSummary("${WORKSPACE}/domain/build/test-results/test")
                        DATA_UNIT_TEST_SUMMARY = unitTestSummary("${WORKSPACE}/data/build/test-results/testGmsDebugUnitTest")
                        DOMAIN_COVERAGE = "${getTestCoverageSummary("$WORKSPACE/domain/build/reports/jacoco/test/jacocoTestReport.csv")}"
                        DATA_COVERAGE = "${getTestCoverageSummary("$WORKSPACE/data/build/reports/jacoco/testGmsDebugUnitTestCoverage/testGmsDebugUnitTestCoverage.csv")}"
                        APP_COVERAGE = "${getTestCoverageSummary("$WORKSPACE/app/build/reports/jacoco/gmsDebugUnitTestCoverage.csv")}"
                        def appSummaryCoverageArray = APP_COVERAGE.split('=')[1].split('/')
                        def domainSummaryCoverageArray = DOMAIN_COVERAGE.split('=')[1].split('/')
                        def dataSummaryCoverageArray = DATA_COVERAGE.split('=')[1].split('/')
                        def appSummaryArray = APP_UNIT_TEST_SUMMARY.split(',')
                        def domainSummaryArray = DOMAIN_UNIT_TEST_SUMMARY.split(',')
                        def dataSummaryArray = DATA_UNIT_TEST_SUMMARY.split(',')

                        String appTestResultsRow = "| **app** | " +
                                "${appSummaryCoverageArray[0]} | " +
                                "${appSummaryCoverageArray[1]} | " +
                                "${appSummaryArray[0]} | " +
                                "${appSummaryArray[1]} | " +
                                "${appSummaryArray[2]} | " +
                                "${appSummaryArray[3]} | " +
                                "${appSummaryArray[4]} | "

                        String domainTestResultsRow = "| **domain** | " +
                                "${domainSummaryCoverageArray[0]} | " +
                                "${domainSummaryCoverageArray[1]} | " +
                                "${domainSummaryArray[0]} | " +
                                "${domainSummaryArray[1]} | " +
                                "${domainSummaryArray[2]} | " +
                                "${domainSummaryArray[3]} | " +
                                "${domainSummaryArray[4]} | "

                        String dataTestResultsRow = "| **data** | " +
                                "${dataSummaryCoverageArray[0]} | " +
                                "${dataSummaryCoverageArray[1]} | " +
                                "${dataSummaryArray[0]} | " +
                                "${dataSummaryArray[1]} | " +
                                "${dataSummaryArray[2]} | " +
                                "${dataSummaryArray[3]} | " +
                                "${dataSummaryArray[4]} | "


                        writeFile file: 'coverage_summary.txt', text: "$appTestResultsRow\n$domainTestResultsRow\n$dataTestResultsRow"

                        sh "curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T \"$WORKSPACE/coverage_summary.txt\" \"${targetPath}/coverage_summary.txt\""
                    }
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
        message += "${lineBreak}Trigger Reason: git PUSH to ${gitlabTargetBranch}"
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
private boolean isDefined(String value) {
    return value != null && !value.isEmpty()
}

/**
 * compose the success message, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
private String firebaseUploadSuccessMessage(String lineBreak) {
    return ":rocket: Android APK uploaded successfully to Firebase AppDistribution!(${env.BUILD_NUMBER})" +
            "${lineBreak}Version:\t${readAppVersion()}" +
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
 * Check if this build is triggered by a deliver_qa command
 * @return
 */
private boolean triggerByDeliverQaCmd() {
    return env.gitlabActionType == "NOTE" &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase.startsWith(DELIVER_QA_CMD)
}

/**
 * Check if this build is triggered by a upload_coverage command
 * @return
 */
private boolean triggerByUploadCoverage() {
    return env.gitlabActionType == "NOTE" &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase.startsWith(UPLOAD_COVERAGE_REPORT_CMD)
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
 * Check if build is triggered by a git push to "develop" branch.
 */
private boolean triggerByPushToDevelop() {
    return env.gitlabActionType == "PUSH" &&
            gitlabTargetBranch == "develop"
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
    } else if (triggerByUploadCoverage()) {
        command = UPLOAD_COVERAGE_REPORT_CMD
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
            "\nLast 10 git commits:\n${sh(script: "git log --pretty=format:\"(%h,%an)%x09%s\" -10", returnStdout: true).trim()}"

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

    SDK_BRANCH = parseCommandParameter()["sdk-branch"]
    if (!isDefined(SDK_BRANCH)) {
        SDK_BRANCH = "develop"
    }

    MEGACHAT_BRANCH = parseCommandParameter()["chat-branch"]
    if (!isDefined(MEGACHAT_BRANCH)) {
        MEGACHAT_BRANCH = "develop"
    }
}

/**
 * read version name and version code from build.gradle.kts
 * @return version name plus version code. Example: "6.6(433)"
 */
private String readAppVersion() {
    String versionName = sh(script: "grep appVersion build.gradle.kts | awk -F'\"' '{print \$4}'", returnStdout: true).trim().replaceAll("\"", "")
    String versionCode = env.APK_VERSION_CODE_FOR_CD
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
 * "[pool-4-thread-1] Deploying artifact: ARTIFACTORY_BASE_URL/artifactory/mega-gradle/mega-sdk-android/nz/mega/sdk/sdk/20221109.084452-rel/sdk-20221109.084452-rel.aar"
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
    return "${env.ARTIFACTORY_BASE_URL}/ui/repos/tree/Properties/mega-gradle/mega-sdk-android/nz/mega/sdk/sdk/${version}/sdk-${version}.aar"
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
 * Analyse unit test report and get the summary string
 * @param testReportPath path of the unit test report in xml format
 * @return summary string of unit test
 */
String unitTestSummary(String testReportPath) {
    return sh(
            script: "python3 ${WORKSPACE}/jenkinsfile/junit_report.py ${testReportPath}",
            returnStdout: true).trim()
}

/**
 * Reads and calculates the Test Coverage by a given csv format report
 * @param csvReportPath path to the csv coverage file, generated by JaCoCo
 * @return a String containing the Test Coverage report
 */
String getTestCoverageSummary(String csvReportPath) {
    summary = sh(
            script: "python3 ${WORKSPACE}/jenkinsfile/coverage_report.py ${csvReportPath}",
            returnStdout: true).trim()
    print("coverage path(${csvReportPath}): ${summary}")
    return summary
}