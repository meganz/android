/**
 * The purpose of this script is to collect lint reports (And later build warnings) and send them to slack
 */

import groovy.json.JsonSlurperClassic

BUILD_STEP = ""

GMS_APK_BUILD_LOG = "gms_build.log"
BUILD_WARNING_FILE = "buildWarnings.json"
WARNING_SOURCE_FILE = "warningResultLog.txt"

MODULE_LIST = ['app', 'domain', 'data', 'core-ui']

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
        JAVA_HOME = "/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx"
        ANDROID_HOME = "/opt/buildtools/android-sdk"

        // PATH for necessary commands
        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        // Jenkins build log will be saved in this file.
        CONSOLE_LOG_FILE = "console.txt"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'

        // Google map api
        GOOGLE_MAP_API_URL = "https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k"
        GOOGLE_MAP_API_FILE = 'default_google_maps_api.zip'
        GOOGLE_MAP_API_UNZIPPED = 'default_google_map_api_unzipped'

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
                        buildLintSummaryTable()
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
                }
                gitlabCommitStatus(name: 'Download Dependency Lib for SDK') {

                    sh """
                        # we still have to download webrtc file for lint check. :( 
                        cd "${WORKSPACE}/jenkinsfile/"
                        bash download_webrtc.sh

                        mkdir -p "${BUILD_LIB_DOWNLOAD_FOLDER}"
                        cd "${BUILD_LIB_DOWNLOAD_FOLDER}"

                        pwd 
                        ls -lh

                        ## check default Google API
                        if test -f "${BUILD_LIB_DOWNLOAD_FOLDER}/${GOOGLE_MAP_API_FILE}"; then
                            echo "${GOOGLE_MAP_API_FILE} already downloaded. Skip downloading."
                        else
                            echo "downloading google map api"
                            mega-get ${GOOGLE_MAP_API_URL}
                
                            echo "unzipping google map api"
                            rm -fr ${GOOGLE_MAP_API_UNZIPPED}
                            unzip ${GOOGLE_MAP_API_FILE} -d ${GOOGLE_MAP_API_UNZIPPED}
                        fi
                
                        ls -lh
                
                        cd ${WORKSPACE}
                        pwd

                        echo "Applying Google Map API patches"
                        rm -fr app/src/debug/res/values/google_maps_api.xml
                        rm -fr app/src/release/res/values/google_maps_api.xml
                        cp -fr ${BUILD_LIB_DOWNLOAD_FOLDER}/${GOOGLE_MAP_API_UNZIPPED}/* app/src/
                
                    """
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
                            LINT_REPORT_SUMMARY_MAP.put(module, lintSummary(module))
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
 * @param jsonLintReportLink A String that contains a link to all Lint Results
 *
 * @return a Markdown table-formatted String
 */
String buildLintSummaryTable() {

    // Declare a JsonSlurperClassic object
    def jsonSlurperClassic = new JsonSlurperClassic()

    def table = [
            ["Module name", "Fatal", "Error", "Warning", "Information", "Error Message"]
    ]

    // Iterate through all the values in LINT_REPORT_SUMMARY_MAP and add a row per module
    // The standard method of iterating a map returns an error when used with a Jenkins pipeline,
    // which is why the map iteration is set up in this manner
    for (def key in LINT_REPORT_SUMMARY_MAP.keySet()) {
        // Parse the JSON String received from lint_report.py into a Map
        def jsonObject = jsonSlurperClassic.parseText(LINT_REPORT_SUMMARY_MAP[key])

        table.add(
                [
                        "$key",
                        "$jsonObject.fatalCount",
                        "$jsonObject.errorCount",
                        "$jsonObject.warningCount",
                        "$jsonObject.informationCount",
                        "$jsonObject.errorMessage"
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
 * Executes lint_report.py in order to parse the Lint Results and create a Lint Summary
 *
 * @param module The name of the module (e.g. app, domain, sdk)
 * @return The Lint Summary Report of the given module from lint_report.py. Here's a sample return value:
 *
 * {"fatalCount": 10, "errorCount": 20, "warningCount": 30, "informationCount": 40, "errorMessage": ""}
 */
String lintSummary(String module) {
    summary = sh(
            script: "python3 ${WORKSPACE}/jenkinsfile/lint_report.py $WORKSPACE/${module}/build/reports/lint-results.xml",
            returnStdout: true).trim()
    // If summary is empty, return a String with 0 counts and a "No lint results found" error message
    if (!summary) summary = '{"fatalCount": "0", "errorCount": "0", "warningCount": "0", "informationCount": "0", "errorMessage": "No lint results found"}'
    print("lintSummary($module) = $summary")
    return summary
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
 * Generate a build warning json file at the target location
 * @param targetFileLocation
 * @return
 */
String generateWarningReport(String targetFileLocation) {
    sh "./gradlew -w clean compileGmsReleaseUnitTestSources > ${WARNING_SOURCE_FILE} 2>&1"
    sh "python3 ${WORKSPACE}/jenkinsfile/warning_report.py ${WARNING_SOURCE_FILE} ${targetFileLocation}"
}
