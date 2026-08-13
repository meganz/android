/**
 * This script builds and uploads the Android APK to Firebase AppDistribution.
 * It can also run the connected device instrumented tests on a Gradle Managed Device
 * via the "device_test" MR command.
 */

 @Library('jenkins-android-shared-lib') _

BUILD_STEP = ''

/**
 * Folder to contain build outputs, including APK, AAB and symbol files
 */
ARCHIVE_FOLDER = "archive"
NATIVE_SYMBOLS_FILE = "symbols.zip"

/**
 * GitLab commands that can trigger this job.
 */
DELIVER_QA_CMD = "deliver_qa"
UPLOAD_COVERAGE_REPORT_CMD = "upload_coverage"
DEVICE_TEST_CMD = "device_test"

/**
 * Test task of the Gradle Managed Device defined in app/app.gradle.kts (testOptions.managedDevices).
 */
DEVICE_TEST_GRADLE_TASK = ":app:ciPixelGmsDebugAndroidTest"

/**
 * System image used by the managed device. Must match the device definition
 * (apiLevel / systemImageSource / ABI) in app/app.gradle.kts.
 */
DEVICE_TEST_SYSTEM_IMAGE = "system-images;android-36;google_apis;arm64-v8a"

/**
 * Folder holding the JUnit XML results of the managed device run.
 */
DEVICE_TEST_RESULTS_FOLDER = "app/build/outputs/androidTest-results/managedDevice/debug/flavors/gms"

/**
 * Folder holding the HTML report of the managed device run.
 */
DEVICE_TEST_REPORT_FOLDER = "app/build/reports/androidTests/managedDevice/debug/flavors/gms"

/**
 * Zip of the HTML test report, uploaded to Artifactory so it can be downloaded from the MR.
 */
DEVICE_TEST_REPORT_ZIP = "device_test_report.zip"

/**
 * Download link of the uploaded test report zip; empty when no report was produced.
 */
DEVICE_TEST_REPORT_LINK = ""

/**
 * common.groovy file with common methods
 */
def common

pipeline {
    // device_test needs an Apple Silicon agent: on Intel machines (labelled mac-intel) the
    // emulator is too slow to render the app and every UI test times out.
    agent {
        label env.gitlabTriggerPhrase?.startsWith("device_test")
                ? '(mac-jenkins-slave-android || mac-jenkins-slave) && !mac-intel'
                : 'mac-jenkins-slave-android || mac-jenkins-slave'
    }
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

        NDK_ROOT = '/opt/buildtools/android-sdk/ndk/27.1.12297006'
        JAVA_HOME = '/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx'
        ANDROID_HOME = '/opt/buildtools/android-sdk'

        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:/usr/sbin/:$PATH"

        CONSOLE_LOG_FILE = 'console.txt'

        APK_VERSION_CODE_FOR_CD = "${new Date().format('yyDDDHHmm', TimeZone.getTimeZone("GMT"))}"
    }
    post {
        failure {
            script {
                common = load('jenkinsfile/common.groovy')

                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                if (triggerByDeliverQaCmd()) {
                    if (common.hasGitLabMergeRequest()) {
                        String mrNumber = common.getMrNumber()
                        String folder = "android_upload/MR-${mrNumber}"
                        String jenkinsLog = common.uploadFileToArtifactory(folder, CONSOLE_LOG_FILE)

                        String message = firebaseUploadFailureMessage("<br/>", jenkinsLog, true)

                        common.sendToMR(message)
                    }
                } else if (triggerByDeviceTestCmd()) {
                    if (common.hasGitLabMergeRequest()) {
                        String mrNumber = common.getMrNumber()
                        String folder = "android_upload/MR-${mrNumber}"
                        String jenkinsLog = common.uploadFileToArtifactory(folder, CONSOLE_LOG_FILE)

                        common.sendToMR(deviceTestFailureMessage("<br/>", jenkinsLog, false))
                        slackSend channel: '#mobile-pipelines-channel', color: 'danger',
                                message: deviceTestFailureMessage("\n", jenkinsLog, true)
                    }
                } else if (triggerByPushToDevelop()) {
                    String jenkinsLog = common.uploadFileToArtifactory("android_upload", CONSOLE_LOG_FILE)

                    slackSend color: 'danger', message: firebaseUploadFailureMessage("\n", jenkinsLog, false)
                }
            }
        }
        success {
            script {
                common = load('jenkinsfile/common.groovy')

                if (triggerByDeliverQaCmd() || triggerByUploadCoverage()) {
                    slackSend color: "good", message: firebaseUploadSuccessMessage("\n", true)
                    common.sendToMR(firebaseUploadSuccessMessage("<br/>", true))
                } else if (triggerByDeviceTestCmd()) {
                    if (common.hasGitLabMergeRequest()) {
                        common.sendToMR(deviceTestSuccessMessage("<br/>", false))
                    }
                    slackSend channel: '#mobile-pipelines-channel', color: 'good',
                            message: deviceTestSuccessMessage("\n", true)
                } else if (triggerByPushToDevelop()) {
                    slackSend color: "good", message: firebaseUploadSuccessMessage("\n", false)
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

                    // send command acknowledgement to MR
                    common.sendToMR(":runner: Android CD pipeline has started!!!(BuildNumber: ${env.BUILD_NUMBER}))" +
                            "<br/><b>Command</b>: ${env.gitlabTriggerPhrase}"
                    )
                }
            }
        }
        stage('Preparation') {
            when {
                expression {
                    triggerByDeliverQaCmd() ||
                            triggerByUploadCoverage() ||
                            triggerByPushToDevelop() ||
                            triggerByDeviceTestCmd()
                }
            }
            steps {
                script {
                    BUILD_STEP = 'Preparation'

                    sh("rm -frv $ARCHIVE_FOLDER")
                    sh("mkdir -p ${WORKSPACE}/${ARCHIVE_FOLDER}")
                    sh("rm -fv ${CONSOLE_LOG_FILE}")
                    sh('set')
                }
            }
        }

        stage('Apply Google Map API Key') {
            when {
                expression { triggerByDeliverQaCmd() || triggerByUploadCoverage() || triggerByPushToDevelop() }
            }
            steps {
                script {
                    BUILD_STEP = 'Apply Google Map API Key'
                }

                withCredentials([
                        file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_QA', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_QA')
                ]) {
                    script {
                        println("applying production google map api config... ")
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_QA} app/src/release/res/values/google_maps_api.xml"
                    }
                }
            }
        }

        stage('Upload Firebase Crashlytics symbol files') {
            when {
                expression { triggerByDeliverQaCmd() }
            }
            steps {
                script {
                    util.useArtifactory() {
                        BUILD_STEP = 'Upload Firebase Crashlytics symbol files'

                        common.downloadAndExtractNativeSymbols()
                        sh """
                            cd $WORKSPACE
                            ./gradlew --no-daemon app:uploadCrashlyticsSymbolFileGmsRelease
                        """
                    }
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
                    sh './gradlew --no-daemon app:assembleGmsRelease'
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
                                "RELEASE_NOTES_FOR_CD=${readReleaseNotes(triggerByDeliverQaCmd())}",
                                "TESTERS_FOR_CD=${parseCommandParameter()["tester"]}",
                                "TESTER_GROUP_FOR_CD=${parseCommandParameter()["tester-group"]}"
                        ]) {
                            println("Upload GMS APK, TESTERS_FOR_CD = ${env.TESTERS_FOR_CD}")
                            println("Upload GMS APK, RELEASE_NOTES_FOR_CD = ${env.RELEASE_NOTES_FOR_CD}")
                            sh './gradlew --no-daemon appDistributionUploadGmsRelease'
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
                        sh './gradlew --no-daemon app:assembleGmsQa'
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
                                "RELEASE_NOTES_FOR_CD=${readReleaseNotes(triggerByDeliverQaCmd())}",
                                "TESTERS_FOR_CD=${parseCommandParameter()["tester"]}",
                                "TESTER_GROUP_FOR_CD=${parseCommandParameter()["tester-group"]}"
                        ]) {
                            sh './gradlew --no-daemon appDistributionUploadGmsQa'
                        }
                    }
                }
            }
        }

        stage('Run Instrumented Tests on Managed Device') {
            when {
                expression { triggerByDeviceTestCmd() }
            }
            steps {
                script {
                    BUILD_STEP = 'Run Instrumented Tests on Managed Device'

                    // The SDK dir is owned by the provisioning user and is read-only for the
                    // Jenkins user, so the pipeline must not try to install anything itself.
                    // Verify the managed-device prerequisites instead, and fail with an
                    // actionable provisioning message when this agent is missing them.
                    sh """
                        IMAGE_DIR="${ANDROID_HOME}/\$(echo "${DEVICE_TEST_SYSTEM_IMAGE}" | tr ';' '/')"
                        if [ ! -d "\$IMAGE_DIR" ] || [ ! -d "${ANDROID_HOME}/emulator" ]; then
                            echo "ERROR: agent \$(hostname) is missing the managed-device prerequisites."
                            echo "Ask the SDK owner (the user owning ${ANDROID_HOME}) to provision it:"
                            echo "  yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --licenses"
                            echo "  ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager emulator '${DEVICE_TEST_SYSTEM_IMAGE}'"
                            exit 1
                        fi
                    """

                    // --rerun forces test execution even when Gradle considers the task up-to-date
                    sh "./gradlew --no-daemon ${DEVICE_TEST_GRADLE_TASK} --rerun"
                }
            }
            // collect results even when tests fail, so Jenkins still shows the failure details
            post {
                always {
                    junit(testResults: "${DEVICE_TEST_RESULTS_FOLDER}/**/*.xml", allowEmptyResults: true)
                    archiveArtifacts(artifacts: "${DEVICE_TEST_REPORT_FOLDER}/**", allowEmptyArchive: true)
                    script {
                        try {
                            if (fileExists(DEVICE_TEST_REPORT_FOLDER)) {
                                sh "cd ${DEVICE_TEST_REPORT_FOLDER} && zip -qr ${WORKSPACE}/${DEVICE_TEST_REPORT_ZIP} ."
                                DEVICE_TEST_REPORT_LINK = common.uploadFileToArtifactory("android_upload", DEVICE_TEST_REPORT_ZIP)
                            }
                        } catch (Exception e) {
                            println("[WARNING] Failed to upload the device test report: ${e.message}")
                        }
                    }
                }
            }
        }

        stage('Collect and Upload Code Coverage') {
            when {
                expression { triggerByUploadCoverage() || triggerByPushToDevelop() }
            }
            steps {
                script {
                    BUILD_STEP = "Upload Code Coverage"

                    util.useArtifactory() {
                        sh "./gradlew --no-daemon runAllUnitTestsWithCoverage"
                        String artifactoryTargetPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/cicd/coverage/"
                        String coverageSummaryFile = "coverage_summary.csv"
                        sh "./gradlew --no-daemon collectCoverage --modules \"${common.getCoverageModuleList(common.getUnitTestModuleList()).join(",")}\" --csv-output ${coverageSummaryFile}"
                        sh "curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T \"$WORKSPACE/$coverageSummaryFile\" \"${artifactoryTargetPath}/$coverageSummaryFile\""
                    }
                }
            }
        }
    }
}

/**
 * Format comment author by adding @ at the beginning so that Gitlab auto links it
 * @return author of the comment
 */
private String formattedCommentAuthor() {
    return "@${gitlabCommentAuthor}"
}

/**
 * Create the build report of failed Firebase Upload
 *
 * @param lineBreak the line break used between the lines. For GitLab and Slack, different line break
 * can be provided. GitLab accepts HTML "<BR/>", and Slack accepts "\n"
 * @param useCommenterAsAuthor True if author should be the name of user who initiated the build by comment
 * @return failure message
 */
private String firebaseUploadFailureMessage(String lineBreak, String logFile, boolean useCommenterAsAuthor) {
    String author = useCommenterAsAuthor ? formattedCommentAuthor() : gitlabUserName
    String message = ":x: Android Firebase Upload Build Failed!(BuildNumber: ${env.BUILD_NUMBER})" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${author}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
    if (env.gitlabActionType == "PUSH") {
        message += "${lineBreak}Trigger Reason: git PUSH to ${gitlabTargetBranch}"
    } else if (env.gitlabActionType == "NOTE") {
        message += "${lineBreak}Trigger Reason: MR comment (${gitlabTriggerPhrase})"
    }
    message += "Build Log: <${logFile}|${CONSOLE_LOG_FILE}>"
    if (env.gitlabTargetBranch == "develop" && env.gitlabSourceBranch == "develop") {
        message += "${lineBreak}Hi <!subteam^S02B2PB5SG7>,  latest `develop` has build failure, please check."
        //notify all Android devs
    }

    return message
}

/**
 * compose the success message, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @param useCommenterAsAuthor True if author should be the user name of who initiated the build by comment
 * @return The success message to be sent
 */
private String firebaseUploadSuccessMessage(String lineBreak, boolean useCommenterAsAuthor) {
    String author = useCommenterAsAuthor ? formattedCommentAuthor() : gitlabUserName
    return ":rocket: Android APK uploaded successfully to Firebase AppDistribution!(${env.BUILD_NUMBER})" +
            "${lineBreak}Version:\t${readAppVersion()}" +
            "${lineBreak}Last Commit Msg:\t${lastCommitMessage()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${author}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}" +
            "${lineBreak}Trigger Reason: ${getTriggerReason()}"
}

/**
 * Compose the device test failure message to be sent to the GitLab MR.
 *
 * @param lineBreak the line break used between the lines
 * @param logFile the uploaded Jenkins console log URL
 * @return failure message
 */
private String deviceTestFailureMessage(String lineBreak, String logFile, boolean slackFormat) {
    return ":x: Android Device Test Failed!(BuildNumber: ${env.BUILD_NUMBER}, Node: ${env.NODE_NAME}, Duration: ${buildDuration()})" +
            "${lineBreak}Test Summary:\t${readDeviceTestSummary()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${formattedCommentAuthor()}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}" +
            deviceTestReportLine(lineBreak, slackFormat) +
            "${lineBreak}Build Log: ${formatLink(CONSOLE_LOG_FILE, logFile, slackFormat)}"
}

/**
 * Compose the device test success message to be sent to the GitLab MR.
 *
 * @param lineBreak the line break used between the lines
 * @return success message
 */
private String deviceTestSuccessMessage(String lineBreak, boolean slackFormat) {
    return ":white_check_mark: Android Device Test Passed!(BuildNumber: ${env.BUILD_NUMBER}, Node: ${env.NODE_NAME}, Duration: ${buildDuration()})" +
            "${lineBreak}Test Summary:\t${readDeviceTestSummary()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${formattedCommentAuthor()}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}" +
            deviceTestReportLine(lineBreak, slackFormat)
}

/**
 * Format a link for the target destination: Slack uses <url|label>, GitLab uses Markdown.
 */
private String formatLink(String label, String url, boolean slackFormat) {
    return slackFormat ? "<${url}|${label}>" : "[${label}](${url})"
}

/**
 * Elapsed time of the current build, e.g. "12 min 34 sec".
 */
private String buildDuration() {
    return currentBuild.durationString.replace(' and counting', '')
}

/**
 * Download link line for the uploaded test report zip, or empty when no report was uploaded
 * (e.g. the build failed before the tests produced one).
 */
private String deviceTestReportLine(String lineBreak, boolean slackFormat) {
    if (!DEVICE_TEST_REPORT_LINK) {
        return ""
    }
    return "${lineBreak}Test Report: ${formatLink(DEVICE_TEST_REPORT_ZIP, DEVICE_TEST_REPORT_LINK, slackFormat)}"
}

/**
 * Read the executed/failed/skipped test counts from the managed device JUnit XML results.
 * @return summary line, e.g. "3 tests, 0 failures, 0 skipped", or "no test results found"
 */
private String readDeviceTestSummary() {
    return sh(
            script: """
                if ls ${DEVICE_TEST_RESULTS_FOLDER}/*/TEST-*.xml > /dev/null 2>&1; then
                    grep -h '<testsuite ' ${DEVICE_TEST_RESULTS_FOLDER}/*/TEST-*.xml | \\
                        awk -F'"' '{ for (i = 1; i < NF; i++) {
                                if (\$i ~ /tests=\$/) t += \$(i+1);
                                if (\$i ~ / failures=\$/) f += \$(i+1);
                                if (\$i ~ / errors=\$/) f += \$(i+1);
                                if (\$i ~ /skipped=\$/) s += \$(i+1);
                        } } END { printf "%d tests, %d failures, %d skipped", t, f, s }'
                else
                    echo "no test results found"
                fi
            """,
            returnStdout: true
    ).trim()
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
 * Check if this build is triggered by a device_test command
 * @return
 */
private boolean triggerByDeviceTestCmd() {
    return env.gitlabActionType == "NOTE" &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase.startsWith(DEVICE_TEST_CMD)
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
 * Parse the parameter of command that triggers this build task. Both 'deliver_qa' and
 * 'upload_coverage' are supported. Command example:
 * "deliver_qa --tester tester1@gmail.com,tester2@gmail.com --tester-group internal_dev,other_group --notes AND-99999 this build fixes the problem of layout in xxx page"
 *
 * @return a map of the parsed parameters and values. Below parameters should be included.
 * For 'deliver_qa' command
 *     key "tester" - list of tester emails, separated by comma
 *     key "notes" - developer specified release notes.
 *     key "tester-group" - developer specified tester group, separated by comma
 *     If deliver_qa command is issued without parameters, then values of above keys are empty.
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

    String[] paramList = otherParams.split("\\p{Z}+") // Split by tabs/spaces/ideographic space

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

/**
 * @param useCommenterAsAuthor True if author should be the user name of who initiated the build by comment
 */
String readReleaseNotes(boolean useCommenterAsAuthor) {
    String author = useCommenterAsAuthor ? formattedCommentAuthor() : gitlabUserName
    String baseRelNotes = "Triggered by: $author" +
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
