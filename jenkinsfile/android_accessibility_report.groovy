/**
 * Scheduled pipeline that runs the :a11y-test accessibility checks on a Gradle Managed Device
 * and posts a designer-facing HTML report of the findings to Slack. When the build is triggered
 * by a GitLab merge request, the same findings digest is also posted as a comment on that MR.
 *
 * Developer-owned "gate" findings fail the instrumented tests and mark this build UNSTABLE.
 * Design-owned findings (text/image contrast, text size) never fail the tests; they are
 * serialized to JSON reports which jenkinsfile/a11y_report.py folds into the HTML report.
 */
BUILD_STEP = ""

/**
 * Test task of the Gradle Managed Device defined in a11y-test/a11y-test.gradle.kts
 * (testOptions.managedDevices).
 */
A11Y_TEST_GRADLE_TASK = ":a11y-test:ciPixelDebugAndroidTest"

/**
 * System image used by the managed device. Must match the device definition
 * (apiLevel / systemImageSource / ABI) in a11y-test/a11y-test.gradle.kts.
 */
A11Y_TEST_SYSTEM_IMAGE = "system-images;android-36;google_apis;arm64-v8a"

/**
 * Folder holding the JUnit XML results of the managed device run.
 */
A11Y_TEST_RESULTS_FOLDER = "a11y-test/build/outputs/androidTest-results/managedDevice/debug/ciPixel"

/**
 * Folder that Gradle Managed Devices pulls the additional test outputs into: the a11y JSON
 * reports written by AccessibilityCheckReporter (under a11y/) plus any screenshots.
 */
A11Y_OUTPUT_FOLDER = "a11y-test/build/outputs/managed_device_android_test_additional_output/debug/ciPixel"

/**
 * The designer-facing HTML report rendered from the JSON outputs.
 */
A11Y_REPORT_FILE = "accessibility_report.html"

/**
 * Plain-text digest of the findings, rendered alongside the HTML report and embedded in the
 * Slack message.
 */
A11Y_SUMMARY_FILE = "accessibility_summary.txt"

/**
 * Findings digest read from A11Y_SUMMARY_FILE, for the Slack message and the MR comment.
 */
A11Y_SLACK_SUMMARY = ""

/**
 * Artifactory link of the uploaded HTML report, shared in the Slack message and the MR comment.
 */
A11Y_REPORT_LINK = ""

/**
 * common.groovy file with common methods. Kept in the script binding (no `def`) like the other
 * globals above, so the helper methods outside the pipeline block can use it too.
 */
common = null

pipeline {
    // The managed device needs an Apple Silicon agent: on Intel machines (labelled mac-intel)
    // the emulator is too slow to render the app and the checks time out.
    agent { label '(mac-jenkins-slave-android || mac-jenkins-slave) && !mac-intel' }
    triggers { cron('0 6 * * 7') }   // weekly
    options {
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '1'))
        timeout(time: 1, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }
    environment {

        ARTIFACTORY_USER = credentials('ARTIFACTORY_USER')
        ARTIFACTORY_ACCESS_TOKEN = credentials('ARTIFACTORY_ACCESS_TOKEN')

        LC_ALL = "en_US.UTF-8"
        LANG = "en_US.UTF-8"

        NDK_ROOT = "/opt/buildtools/android-sdk/ndk/27.1.12297006"
        JAVA_HOME = "/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx"
        ANDROID_HOME = "/opt/buildtools/android-sdk"

        // PATH for necessary commands
        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        // Jenkins build log will be saved in this file.
        CONSOLE_LOG_FILE = "console.txt"
    }
    post {
        failure {
            script {
                withCredentials([usernameColonPassword(credentialsId: 'Jenkins-Login', variable: 'CREDENTIALS')]) {
                    def comment = ":x: Android accessibility report build failed (step: ${BUILD_STEP})"
                    sh 'curl -u $CREDENTIALS ${BUILD_URL}/consoleText -o console.txt'
                    String jenkinsLog = common.uploadFileToArtifactory("accessibility_report", CONSOLE_LOG_FILE)
                    String buildLog = "Build Log: <${jenkinsLog}|${CONSOLE_LOG_FILE}>"
                    slackSend color: "danger", message: buildLog + "\n" + comment
                    if (common.hasGitLabMergeRequest()) {
                        common.sendToMR("${comment}<br/>Build Log: [${CONSOLE_LOG_FILE}](${jenkinsLog})")
                    }
                }
            }
        }
        unstable {
            script {
                def message = ":warning: Android accessibility report — some checks failed the developer gate\n" +
                        "```${A11Y_SLACK_SUMMARY}```\n" +
                        "Gate failures: ${BUILD_URL}testReport"
                sendReportToSlack(message, "warning")
                sendReportToMR(":warning: **Android accessibility report** — some checks failed the developer gate")
            }
        }
        success {
            script {
                def message = "Android accessibility report:\n```${A11Y_SLACK_SUMMARY}```"
                sendReportToSlack(message, "good")
                sendReportToMR(":white_check_mark: **Android accessibility report**")
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
                    BUILD_STEP = 'Preparation'
                    common = load('jenkinsfile/common.groovy')
                }
            }
        }

        stage('Run accessibility checks') {
            steps {
                script {
                    BUILD_STEP = 'Run accessibility checks'

                    // The SDK dir is owned by the provisioning user and is read-only for the
                    // Jenkins user, so the pipeline must not try to install anything itself.
                    // Verify the managed-device prerequisites instead, and fail with an
                    // actionable provisioning message when this agent is missing them.
                    sh """
                        IMAGE_DIR="${ANDROID_HOME}/\$(echo "${A11Y_TEST_SYSTEM_IMAGE}" | tr ';' '/')"
                        if [ ! -d "\$IMAGE_DIR" ] || [ ! -d "${ANDROID_HOME}/emulator" ]; then
                            echo "ERROR: agent \$(hostname) is missing the managed-device prerequisites."
                            echo "Ask the SDK owner (the user owning ${ANDROID_HOME}) to provision it:"
                            echo "  yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --licenses"
                            echo "  ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager emulator '${A11Y_TEST_SYSTEM_IMAGE}'"
                            exit 1
                        fi
                    """

                    // Gate failures mark the build UNSTABLE instead of aborting it, so the
                    // design-owned findings are still rendered and posted to Slack.
                    // --rerun forces test execution even when Gradle considers the task up-to-date
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        sh "./gradlew --no-daemon ${A11Y_TEST_GRADLE_TASK} --rerun"
                    }
                }
            }
            // collect results even when the checks fail, so Jenkins still shows the details
            post {
                always {
                    junit(testResults: "${A11Y_TEST_RESULTS_FOLDER}/**/*.xml", allowEmptyResults: true)
                    archiveArtifacts(artifacts: "${A11Y_OUTPUT_FOLDER}/**", allowEmptyArchive: true)
                }
            }
        }

        stage('Render designer report') {
            steps {
                script {
                    BUILD_STEP = 'Render designer report'

                    sh "python3 jenkinsfile/a11y_report.py ${A11Y_OUTPUT_FOLDER} ${A11Y_REPORT_FILE} --summary ${A11Y_SUMMARY_FILE}"
                    A11Y_SLACK_SUMMARY = readFile(A11Y_SUMMARY_FILE).trim()
                    A11Y_REPORT_LINK = common.uploadFileToArtifactory("accessibility_report", A11Y_REPORT_FILE)
                }
                archiveArtifacts(artifacts: "${A11Y_REPORT_FILE}", allowEmptyArchive: false)
            }
        }
    }
}

/**
 * Posts the findings summary to the default Slack channel, with a link to the HTML report.
 *
 * @param message the summary message
 * @param color the slackSend color, e.g. "good" or "warning"
 */
private void sendReportToSlack(String message, String color) {
    String reportLine = A11Y_REPORT_LINK ? "\nReport for designers: <${A11Y_REPORT_LINK}|${A11Y_REPORT_FILE}>" : ""
    slackSend color: color, message: message + reportLine
}

/**
 * Comments the findings digest and the report links on the merge request that triggered this
 * build. Does nothing for scheduled or manual builds.
 *
 * @param headline first line of the comment, e.g. the build outcome
 */
private void sendReportToMR(String headline) {
    if (!common.hasGitLabMergeRequest()) {
        return
    }
    List<String> lines = [headline]
    if (A11Y_SLACK_SUMMARY) {
        lines.addAll(A11Y_SLACK_SUMMARY.readLines())
    }
    if (A11Y_REPORT_LINK) {
        lines.add("Report for designers: [${A11Y_REPORT_FILE}](${A11Y_REPORT_LINK})".toString())
    }
    lines.add("Gate results: [Jenkins test report](${env.BUILD_URL}testReport)".toString())
    common.sendToMR(lines.join("<br/>"))
}
