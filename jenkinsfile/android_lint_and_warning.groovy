/**
 * The purpose of this script is to collect lint reports (And later build warnings) and send them to slack
 */
BUILD_STEP = ""

GMS_APK_BUILD_LOG = "gms_build.log"
BUILD_WARNING_FILE = "buildWarnings.json"
WARNING_SOURCE_FILE = "warningResultLog.txt"

MODULE_LIST = ['app', 'domain', 'data', 'shared/original-core-ui']

LINT_REPORT_SUMMARY_MAP = [:]

/**
 * common.groovy file with common methods
 */
def common

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }
    triggers { cron('0 7 * * 7') }   // weekly
    options {
        // Stop the build early in case of compile or test failures
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '1'))
        timeout(time: 1, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }
    environment {

        LC_ALL = "en_US.UTF-8"
        LANG = "en_US.UTF-8"

        NDK_ROOT = "/opt/buildtools/android-sdk/ndk/21.3.6528147"
        JAVA_HOME = "/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx"
        ANDROID_HOME = "/opt/buildtools/android-sdk"

        // PATH for necessary commands
        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        // Jenkins build log will be saved in this file.
        CONSOLE_LOG_FILE = "console.txt"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'

        COMBINE_LINT_REPORTS = "true"
        DO_NOT_SUPPRESS_WARNINGS = "true"
    }
    post {
        failure {
            script {
                withCredentials([usernameColonPassword(credentialsId: 'Jenkins-Login', variable: 'CREDENTIALS')]) {
                    def comment = ":x: Android Lint Build failed"
                    slackSend color: "danger", message: comment
                    sh 'curl -u $CREDENTIALS ${BUILD_URL}/consoleText -o console.txt'
                    slackUploadFile filePath: "console.txt", initialComment: "Android Build Log"
                }
            }
        }
        success {
            script {
                def successSlackMessage = "Android Lint report: \n" +
                        buildLintSummaryTable(LINT_REPORT_SUMMARY_MAP)
                slackSend channel: "#android_lint_and_build_warnings", color: "good", message: successSlackMessage
                slackSend channel: "#mobile-dev-team", color: "good", message: successSlackMessage

                def lintReportFile = "app/build/reports/combined.html"

                withCredentials([string(credentialsId: 'Slack-integration-login', variable: 'CREDENTIALS')]) {
                    sh "curl -F file=@${lintReportFile} -F \"initial_comment=Lint report\" -F channels=android_lint_and_build_warnings -H \"Authorization: Bearer ${CREDENTIALS}\" https://slack.com/api/files.upload"
                    sh "curl -F file=@${BUILD_WARNING_FILE} -F \"initial_comment=Build warnings\" -F channels=android_lint_and_build_warnings -H \"Authorization: Bearer ${CREDENTIALS}\" https://slack.com/api/files.upload"
                    sh "curl -F file=@${WARNING_SOURCE_FILE} -F \"initial_comment=Source file\" -F channels=android_lint_and_build_warnings -H \"Authorization: Bearer ${CREDENTIALS}\" https://slack.com/api/files.upload"
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
                    BUILD_STEP = 'Preparation'

                    common = load('jenkinsfile/common.groovy')
                }
            }
        }

        stage('Download Dependency Lib for SDK') {
            steps {
                script {
                    BUILD_STEP = "Download Dependency Lib for SDK"
                    common.downloadDependencyLibForSdk()
                }
            }
        }
        stage('Build APK (GMS)') {
            steps {
                script {
                    BUILD_STEP = 'Build APK (GMS)'
                }
                gitlabCommitStatus(name: 'Build APK (GMS+QA)') {
                    // Finish building and packaging the APK
                    sh "./gradlew clean"
                    sh "./gradlew app:assembleGmsRelease 2>&1  | tee ${GMS_APK_BUILD_LOG}"

                    sh """
                        if grep -q -m 1 \"^FAILURE: \" ${GMS_APK_BUILD_LOG}; then
                            echo GMS APK build failed. Exitting....
                            exit 1
                        fi
                    """
                }
            }
        }
        stage('Generate warning report') {
            steps {
                script {
                    BUILD_STEP = "Generate warning report"

                    generateWarningReport(BUILD_WARNING_FILE)
                }
            }
        }
        stage('Lint Check') {
            steps {
                // Run Lint and analyse the results
                script {
                    BUILD_STEP = "Lint Check"
                }

                gitlabCommitStatus(name: 'Lint Check') {
                    sh "mv custom_lint.xml lint.xml"
                    sh "./gradlew lint"

                    script {
                        MODULE_LIST.each { module ->
                            LINT_REPORT_SUMMARY_MAP.put(module, generateLintSummary(module))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a Markdown table-formatted String that holds all the Lint Results for available modules
 *
 * @param lintReportSummaryMap a Map of all Modules with their corresponding Lint Results
 * @return a Markdown table-formatted String
 */
String buildLintSummaryTable(Map lintReportSummaryMap) {
    def table = [
            ["Module name", "Fatal", "Error", "Warning", "Information", "Error Message"]
    ]

    // Iterate through all the values in LINT_REPORT_SUMMARY_MAP and add a row per module
    // The standard method of iterating a map returns an error when used with a Jenkins pipeline,
    // which is why the map iteration is set up in this manner
    for (def key in lintReportSummaryMap.keySet()) {
        def lintJsonContent = lintReportSummaryMap[key]

        table.add(
                [
                        "$key",
                        "$lintJsonContent.fatalCount",
                        "$lintJsonContent.errorCount",
                        "$lintJsonContent.warningCount",
                        "$lintJsonContent.informationCount",
                        "$lintJsonContent.errorMessage"
                ]
        )

    }

    def title = "*Lint Report Summary*"
    def out = new StringBuffer()
    out << "```"
    out << title.center(65, "-")
    out << "\n"
    table.each {
        out << it[0].padRight(14)
        out << it[1].center(7)
        out << it[2].center(7)
        out << it[3].center(9)
        out << it[4].center(13)
        out << it[5].center(15)
        out << '\n'
    }
    out << "```"
    out.toString()
}

/**
 * Executes a specific Gradle Task to parse the raw Lint Results and returns a Lint Summary
 *
 * @param module The name of the module (e.g. app, domain, sdk)
 * @return A List containing the module's Lint Summary.
 * Here's a Sample Result:
 *
 * [errorCount:20, errorMessage:None, fatalCount:10, informationCount:40, warningCount:30]
 */
def generateLintSummary(String module) {
    def targetFile = "${module}_processed-lint-results.json"
    sh "./gradlew generateLintReport --lint-results $WORKSPACE/${module}/build/reports/lint-results.xml --target-file ${targetFile}"
    def lintJsonFile = readFile(targetFile)
    def lintJsonContent = new HashMap(new groovy.json.JsonSlurper().parseText(lintJsonFile))
    print("lintSummary($module) = ${lintJsonContent}")

    return lintJsonContent
}

/**
 * Generate a build warning json file at the target location
 * @param targetFileLocation
 * @return
 */
String generateWarningReport(String targetFileLocation) {
    sh "./gradlew -w clean compileGmsReleaseUnitTestSources 2>&1 | tee ${WARNING_SOURCE_FILE}"
    sh "./gradlew generateBuildWarningReport --build-log $WARNING_SOURCE_FILE --target-file $targetFileLocation"
}
