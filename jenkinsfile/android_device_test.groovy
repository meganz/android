/**
 * This script runs the connected device instrumented tests on a Gradle Managed Device.
 *
 * Two Jenkins jobs share it: the MR job, triggered by the "device_test" MR command, builds the MR
 * source branch; the nightly job builds develop on a schedule. The schedule lives in the nightly
 * job configuration, not here: a `triggers` directive would be registered on both jobs, and the MR
 * job checks out `origin/${gitlabSourceBranch}`, which a timer build cannot resolve.
 *
 * The result is posted to the MR when there is one, and to Slack: a passed run goes to the default
 * channel, a failed run to the default channel and the Android dev team channel.
 */

BUILD_STEP = ''

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
 * Zip of the HTML test report and the per-test logcat, uploaded to Artifactory so it can be
 * downloaded from the MR or the Slack message.
 */
DEVICE_TEST_REPORT_ZIP = "device_test_report.zip"

/**
 * Download link of the uploaded test report zip; empty when no report was produced.
 */
DEVICE_TEST_REPORT_LINK = ""

/**
 * Artifactory folder of the uploaded console log and test report. MR runs get a MR-<n> sub folder,
 * the other runs a sub folder per job.
 */
ARTIFACTORY_FOLDER = "android_device_test"

/**
 * common.groovy file with common methods
 */
def common

pipeline {
    // Apple Silicon only: on Intel machines (labelled mac-intel) the emulator is too slow to
    // render the app and every UI test times out.
    agent { label '(mac-jenkins-slave-android || mac-jenkins-slave) && !mac-intel' }
    options {
        // Stop the build early in case of compile or test failures
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '1'))
        timeout(time: 2, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }
    environment {

        ARTIFACTORY_USER = credentials('ARTIFACTORY_USER')
        ARTIFACTORY_ACCESS_TOKEN = credentials('ARTIFACTORY_ACCESS_TOKEN')
        SLACK_CHANNEL_ANDROID_DEV_TEAM = credentials('SLACK_CHANNEL_ANDROID_DEV_TEAM')
        LC_ALL = 'en_US.UTF-8'
        LANG = 'en_US.UTF-8'

        NDK_ROOT = '/opt/buildtools/android-sdk/ndk/27.1.12297006'
        JAVA_HOME = '/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx'
        ANDROID_HOME = '/opt/buildtools/android-sdk'

        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:/usr/sbin/:$PATH"

        CONSOLE_LOG_FILE = 'console.txt'
    }
    post {
        always {
            script {
                if (common != null) {
                    common.finishStages(currentBuild.currentResult)
                }
            }
        }
        failure {
            script {
                common = load('jenkinsfile/common.groovy')

                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                String jenkinsLog = common.uploadFileToArtifactory(artifactoryFolder(common), CONSOLE_LOG_FILE)

                if (common.hasGitLabMergeRequest()) {
                    common.sendToMR(deviceTestFailureMessage("<br/>", jenkinsLog, false))
                }
                String slackMessage = deviceTestFailureMessage("\n", jenkinsLog, true)
                slackSend color: 'danger', message: slackMessage
                slackSend channel: env.SLACK_CHANNEL_ANDROID_DEV_TEAM, color: 'danger', message: slackMessage
            }
        }
        success {
            script {
                common = load('jenkinsfile/common.groovy')

                if (common.hasGitLabMergeRequest()) {
                    common.sendToMR(deviceTestSuccessMessage("<br/>", false))
                }
                slackSend color: 'good', message: deviceTestSuccessMessage("\n", true)
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

                    // send command acknowledgement to MR (no-op when there is no MR)
                    common.sendToMR(":runner: Android device test pipeline has started!!!(BuildNumber: ${env.BUILD_NUMBER}))" +
                            "<br/><b>Command</b>: ${env.gitlabTriggerPhrase}"
                    )
                }
            }
        }

        stage('Run Instrumented Tests on Managed Device') {
            steps {
                script {
                    BUILD_STEP = 'Run Instrumented Tests on Managed Device'
                    // GitLab commit statuses are named after the MR command; a timer or manual
                    // run has none, and would show up on the develop commit as a "push" status.
                    if (env.gitlabActionType) {
                        common.beginStage(BUILD_STEP)
                    }

                    sh("rm -fv ${CONSOLE_LOG_FILE}")

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
                    archiveArtifacts(artifacts: "${DEVICE_TEST_RESULTS_FOLDER}/**/logcat-*.txt", allowEmptyArchive: true)
                    script {
                        try {
                            if (fileExists(DEVICE_TEST_REPORT_FOLDER)) {
                                sh "cd ${DEVICE_TEST_REPORT_FOLDER} && zip -qr ${WORKSPACE}/${DEVICE_TEST_REPORT_ZIP} ."
                                stageDeviceTestLogcat()
                                DEVICE_TEST_REPORT_LINK = common.uploadFileToArtifactory(artifactoryFolder(common), DEVICE_TEST_REPORT_ZIP)
                            }
                        } catch (Exception e) {
                            println("[WARNING] Failed to upload the device test report: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Artifactory folder for this build's uploads: a MR-<n> sub folder for MR-triggered builds, a
 * per-job sub folder for scheduled / manual runs, whose build numbers would otherwise collide
 * between the jobs sharing this script.
 *
 * @param common the loaded common.groovy script
 */
private String artifactoryFolder(common) {
    return common.hasGitLabMergeRequest()
            ? "${ARTIFACTORY_FOLDER}/MR-${common.getMrNumber()}"
            : "${ARTIFACTORY_FOLDER}/${env.JOB_BASE_NAME}"
}

/**
 * Who started the build: the MR commenter (prefixed with @ so that GitLab auto links it), or the
 * Jenkins build cause (e.g. "Started by timer") when there is no GitLab event.
 */
private String triggerAuthor() {
    if (env.gitlabCommentAuthor) {
        return "@${env.gitlabCommentAuthor}"
    }
    def causes = currentBuild.getBuildCauses()
    return causes ? causes[0].shortDescription : "Jenkins"
}

/**
 * Branch lines of the report: target and source branch for a MR build, the checked out branch
 * for a timer / manual run.
 *
 * @param lineBreak the line break used between the lines
 */
private String branchLines(String lineBreak) {
    if (env.gitlabSourceBranch) {
        return "${lineBreak}Target Branch:\t${env.gitlabTargetBranch}" +
                "${lineBreak}Source Branch:\t${env.gitlabSourceBranch}"
    }
    return "${lineBreak}Branch:\t${env.GIT_BRANCH}"
}

/**
 * Compose the device test failure message to be sent to the GitLab MR or Slack.
 *
 * @param lineBreak the line break used between the lines. GitLab accepts HTML "<br/>", and Slack accepts "\n"
 * @param logFile the uploaded Jenkins console log URL
 * @param slackFormat true to format links for Slack, false for GitLab Markdown
 * @return failure message
 */
private String deviceTestFailureMessage(String lineBreak, String logFile, boolean slackFormat) {
    return ":x: Android Device Test Failed!(BuildNumber: ${env.BUILD_NUMBER}, Node: ${env.NODE_NAME})" +
            "${lineBreak}Test Summary:\t${readDeviceTestSummary()}" +
            "${lineBreak}Total Time:\t${buildDuration()}" +
            branchLines(lineBreak) +
            "${lineBreak}Author:\t${triggerAuthor()}" +
            "${lineBreak}Commit:\t${env.GIT_COMMIT}" +
            deviceTestReportLine(lineBreak, slackFormat) +
            "${lineBreak}Build Log: ${formatLink(CONSOLE_LOG_FILE, logFile, slackFormat)}"
}

/**
 * Compose the device test success message to be sent to the GitLab MR or Slack.
 *
 * @param lineBreak the line break used between the lines. GitLab accepts HTML "<br/>", and Slack accepts "\n"
 * @param slackFormat true to format links for Slack, false for GitLab Markdown
 * @return success message
 */
private String deviceTestSuccessMessage(String lineBreak, boolean slackFormat) {
    return ":white_check_mark: Android Device Test Passed!(BuildNumber: ${env.BUILD_NUMBER}, Node: ${env.NODE_NAME})" +
            "${lineBreak}Test Summary:\t${readDeviceTestSummary()}" +
            "${lineBreak}Total Time:\t${buildDuration()}" +
            branchLines(lineBreak) +
            "${lineBreak}Author:\t${triggerAuthor()}" +
            "${lineBreak}Commit:\t${env.GIT_COMMIT}" +
            deviceTestReportLine(lineBreak, slackFormat)
}

/**
 * Format a link for the target destination: Slack uses <url|label>, GitLab uses Markdown.
 */
private String formatLink(String label, String url, boolean slackFormat) {
    return slackFormat ? "<${url}|${label}>" : "[${label}](${url})"
}

/**
 * Total time of the pipeline so far, e.g. "12 min 34 sec". Called from the post section, so it
 * covers every stage from checkout to the test run; the build is still running there, hence the
 * "and counting" suffix to strip.
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
 * Add the managed device's per-test logcat files to the uploaded test report zip.
 *
 * A crash that kills the instrumentation process explains itself only in logcat; the HTML report
 * records that the run died but not why. The files are added under the device folder they were
 * produced in, so each one sits beside the HTML for the same run.
 */
private void stageDeviceTestLogcat() {
    sh """
        if ls ${DEVICE_TEST_RESULTS_FOLDER}/*/logcat-*.txt > /dev/null 2>&1; then
            cd ${DEVICE_TEST_RESULTS_FOLDER} && zip -qr ${WORKSPACE}/${DEVICE_TEST_REPORT_ZIP} . -i '*/logcat-*.txt'
        else
            echo "No device test logcat files to add to ${DEVICE_TEST_REPORT_ZIP}"
        fi
    """
}
