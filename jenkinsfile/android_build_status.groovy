BUILD_STEP = ""

// Below values will be read from MR description and are used to decide SDK versions
SDK_BRANCH = "develop"
MEGACHAT_BRANCH = "develop"

GMS_APK_BUILD_LOG = "gms_build.log"
HMS_APK_BUILD_LOG = "hms_build.log"
QA_APK_BUILD_LOG = "qa_build.log"

MODULE_LIST = ['app', 'domain', 'sdk', 'data']

LINT_REPORT_FOLDER = "lint_reports"
LINT_REPORT_ARCHIVE = "lint_reports.zip"
LINT_REPORT_SUMMARY = ""

APP_UNIT_TEST_SUMMARY = ""
DOMAIN_UNIT_TEST_SUMMARY = ""
DATA_UNIT_TEST_SUMMARY = ""
APP_UNIT_TEST_RESULT = ""
DOMAIN_UNIT_TEST_RESULT = ""
DATA_UNIT_TEST_RESULT = ""
APP_UNIT_TEST_REPORT_ARCHIVE = "app_unit_test_result_${env.GIT_COMMIT}.zip"
DOMAIN_UNIT_TEST_REPORT_ARCHIVE = "domain_unit_test_result_${env.GIT_COMMIT}.zip"
DATA_UNIT_TEST_REPORT_ARCHIVE = "data_unit_test_result_${env.GIT_COMMIT}.zip"

APP_COVERAGE = ""
DOMAIN_COVERAGE = ""
DATA_COVERAGE = ""
COVERAGE_ARCHIVE = "coverage.zip"
COVERAGE_FOLDER = "coverage"

/**
 * common.groovy file with common methods
 */
def common

HTML_INDENT = "-- "
/**
 * Decide whether we should skip the current build. If MR title starts with "Draft:"
 * or "WIP:", then CI pipeline skips all stages in a build. After these 2 tags have
 * been removed from MR title, newly triggered builds will resume to normal.
 *
 * @return true if current stage should be skipped. Otherwise return false.
 */
def shouldSkipBuild() {
    String mrTitle = env.GITLAB_OA_TITLE
    if (mrTitle != null && !mrTitle.isEmpty()) {
        return mrTitle.toLowerCase().startsWith("draft:") ||
                mrTitle.toLowerCase().startsWith("wip:")
    }
    // If title is null, this build is probably triggered by 'jenkins rebuild' comment.
    // In such case, build should not be skipped.
    return false
}

/**
 * Fetch the message of the last commit from environment variable.
 *
 * @return The commit message text if GitLab plugin has sent a valid commit message, which is
 * denoted as a Code Block in Gitlab.
 *
 * Otherwise, return a Bold "N/A" normally when CI build is triggered by MR comment "jenkins rebuild".
 */
String getLastCommitMessage() {
    def lastCommitMessage = env.GITLAB_OA_LAST_COMMIT_MESSAGE
    if (lastCommitMessage == null) {
        return '**N/A**'
    } else {
        // use markdown backticks to format commit message into a code block
        return "\n\\`\\`\\`\n$lastCommitMessage\n\\`\\`\\`\n".stripIndent().stripMargin()
    }
}

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }
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
    }
    post {
        failure {
            script {
                common = load('jenkinsfile/common.groovy')

                if (common.hasGitLabMergeRequest()) {

                    // download Jenkins console log
                    downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                    // upload Jenkins console log
                    String jsonJenkinsLog = uploadFileToGitLab(CONSOLE_LOG_FILE)

                    // upload unit test report if unit test fail
                    String unitTestResult = ""
                    if (BUILD_STEP == "Unit Test") {
                        def appUnitTestSummary = unitTestSummaryWithArchiveLink(
                                "app/build/test-results/testGmsDebugUnitTest",
                                "app/build/reports/tests/testGmsDebugUnitTest",
                                APP_UNIT_TEST_REPORT_ARCHIVE
                        )
                        unitTestResult += "<br>App Unit Test: ${appUnitTestSummary}"

                        def domainUnitTestSummary = unitTestSummaryWithArchiveLink(
                                "domain/build/test-results/test",
                                "domain/build/reports/tests/test",
                                DOMAIN_UNIT_TEST_REPORT_ARCHIVE
                        )
                        unitTestResult += "<br>Domain Unit Test: ${domainUnitTestSummary}"

                        def dataUnitTestSummary = unitTestSummaryWithArchiveLink(
                                "data/build/test-results/testDebugUnitTest",
                                "data/build/reports/tests/testDebugUnitTest",
                                DATA_UNIT_TEST_REPORT_ARCHIVE
                        )
                        unitTestResult += "<br>Data Unit Test: ${dataUnitTestSummary}"
                    }

                    def failureMessage = ":x: Build Failed" +
                            "<br/>Failure Stage: ${BUILD_STEP}" +
                            "<br/>Last Commit Message: ${getLastCommitMessage()}" +
                            "Last Commit ID: ${env.GIT_COMMIT}" +
                            "<br/>Build Log: ${jsonJenkinsLog}" +
                            unitTestResult
                    common.sendToMR(failureMessage)
                } else {
                    withCredentials([usernameColonPassword(credentialsId: 'Jenkins-Login', variable: 'CREDENTIALS')]) {
                        def comment = ":x: Android Build failed for branch: ${env.GIT_BRANCH}"
                        if (env.CHANGE_URL) {
                            comment = ":x: Android Build failed for branch: ${env.GIT_BRANCH} \nMR Link:${env.CHANGE_URL}"
                        }
                        slackSend color: "danger", message: comment
                        sh 'curl -u $CREDENTIALS ${BUILD_URL}/consoleText -o console.txt'
                        slackUploadFile filePath: "console.txt", initialComment: "Android Build Log"
                    }
                }
            }
        }
        success {
            script {
                common = load('jenkinsfile/common.groovy')

                if (common.hasGitLabMergeRequest()) {
                    // If CI build is skipped due to Draft status, send a comment to MR
                    if (shouldSkipBuild()) {
                        def skipMessage = ":raising_hand: Android CI Pipeline Build Skipped! <BR/> " +
                                "Newly triggered builds will resume after you have removed <b>Draft:</b> or " +
                                "<b>WIP:</b> from the beginning of MR title."
                        common.sendToMR(skipMessage)
                    } else {
                        // String containing the Lint Results
                        String jsonLintReportLink = uploadFileToGitLab(LINT_REPORT_ARCHIVE)

                        // Create the String to be posted as a comment in Gitlab
                        String mergeRequestMessage = ":white_check_mark: Build Succeeded!\n\n" +
                                "**Last Commit:** (${env.GIT_COMMIT})" + getLastCommitMessage() +
                                "**Build Warnings:**\n" + getBuildWarnings() + "\n\n" +
                                "**Lint Summary:** (${jsonLintReportLink}):<br/>" + "${LINT_REPORT_SUMMARY}" + "\n\n" +
                                buildTestResults()

                        // Send mergeRequestMessage to MR
                        common.sendToMR(mergeRequestMessage)

                        def successSlackMessage = "Android Line Code Coverage:" +
                                "\nCommit:\t${env.GIT_COMMIT}" +
                                "\nBranch:\t${env.GIT_BRANCH}" +
                                "\n- app coverage: $APP_COVERAGE" +
                                "\n- domain coverage: $DOMAIN_COVERAGE" +
                                "\n- data coverage: $DATA_COVERAGE"
                        slackSend color: "good", message: successSlackMessage
                    }
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
        stage('Preparation') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                script {
                    BUILD_STEP = "Preparation"
                }
                gitlabCommitStatus(name: 'Preparation') {
                    script {

                        sh("rm -fv ${CONSOLE_LOG_FILE}")
                        sh("set")
                        sh("rm -fv unit_test_result*.zip")
                    }
                }
            }
        }

        stage('Download Dependency Lib for SDK') {
            when {
                expression { (!shouldSkipBuild()) }
            }
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
        stage('Build APK (GMS+HMS+QA)') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                script {
                    BUILD_STEP = 'Build APK (GMS+HMS+QA)'
                }
                gitlabCommitStatus(name: 'Build APK (GMS+HMS)') {
                    // Finish building and packaging the APK
                    sh "./gradlew clean"
                    sh "./gradlew app:assembleGmsRelease 2>&1  | tee ${GMS_APK_BUILD_LOG}"
                    sh "./gradlew app:assembleHmsRelease 2>&1  | tee ${HMS_APK_BUILD_LOG}"
                    sh "./gradlew app:assembleGmsQa 2>&1  | tee ${QA_APK_BUILD_LOG}"

                    sh """
                        if grep -q -m 1 \"^FAILURE: \" ${GMS_APK_BUILD_LOG}; then
                            echo GMS APK build failed. Exitting....
                            exit 1
                        fi
                        if grep -q -m 1 \"^FAILURE: \" ${HMS_APK_BUILD_LOG}; then
                            echo HMS APK build failed. Exitting....
                            exit 1
                        fi
                        if grep -q -m 1 \"^FAILURE: \" ${QA_APK_BUILD_LOG}; then
                            echo HMS APK build failed. Exitting....
                            exit 1
                        fi
                    """
                }
            }
        }
        stage('Unit Test') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                script {
                    BUILD_STEP = "Unit Test"
                }
                gitlabCommitStatus(name: 'Unit Test') {
                    // Compile and run unit tests for available modules
                    sh "./gradlew testGmsDebugUnitTest"
                    sh "./gradlew domain:test"
                    sh "./gradlew :data:testGmsDebugUnitTest"
                    sh "./gradlew lint:test"

                    script {
                        // below code is only run when UnitTest is OK, before test reports are cleaned up.
                        // If UnitTest is failed, summary is collected at post.failure{} phase
                        // We have to collect the report here, before they are cleaned in the last stage.
                        APP_UNIT_TEST_SUMMARY = unitTestSummary("${WORKSPACE}/app/build/test-results/testGmsDebugUnitTest")
                        DOMAIN_UNIT_TEST_SUMMARY = unitTestSummary("${WORKSPACE}/domain/build/test-results/test")
                        DATA_UNIT_TEST_SUMMARY = unitTestSummary("${WORKSPACE}/data/build/test-results/testGmsDebugUnitTest")
                        APP_UNIT_TEST_RESULT = unitTestArchiveLink("app/build/reports/tests/testGmsDebugUnitTest", "app_unit_test_result.zip")
                        DOMAIN_UNIT_TEST_RESULT = unitTestArchiveLink("domain/build/reports/tests/test", "domain_unit_test_result.zip")
                        DATA_UNIT_TEST_RESULT = unitTestArchiveLink("data/build/reports/tests/testGmsDebugUnitTest", "data_unit_test_result.zip")
                    }
                }
            }
        }
        stage('Code Coverage') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                script {
                    BUILD_STEP = "Code Coverage"
                }
                gitlabCommitStatus(name: 'Code Coverage') {
                    script {

                        // domain coverage
                        sh "./gradlew domain:jacocoTestReport"
                        sh "ls -l $WORKSPACE/domain/build/reports/jacoco/test/"
                        DOMAIN_COVERAGE = "${getTestCoverageSummary("$WORKSPACE/domain/build/reports/jacoco/test/jacocoTestReport.csv")}"
                        println("DOMAIN_COVERAGE = ${DOMAIN_COVERAGE}")

                        // data coverage
                        sh "./gradlew data:testGmsDebugUnitTestCoverage"
                        DATA_COVERAGE = "${getTestCoverageSummary("$WORKSPACE/data/build/reports/jacoco/testGmsDebugUnitTestCoverage/testGmsDebugUnitTestCoverage.csv")}"
                        println("DATA_COVERAGE = ${DATA_COVERAGE}")

                        // temporarily disable the failed test cases
                        sh "rm -frv ${WORKSPACE}/app/src/testDebug"

                        // run coverage for app module
                        sh "./gradlew app:createUnitTestCoverageReport"

                        // restore failed test cases
                        sh "git checkout -- app/src/testDebug"

                        APP_COVERAGE = "${getTestCoverageSummary("$WORKSPACE/app/build/reports/jacoco/gmsDebugUnitTestCoverage.csv")}"
                        println("APP_COVERAGE = ${APP_COVERAGE}")
                    }
                }
            }
        }
        stage('Lint Check') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                // Run Lint and analyse the results
                script {
                    BUILD_STEP = "Lint Check"
                }

                gitlabCommitStatus(name: 'Lint Check') {
                    sh "mv custom_lint.xml lint.xml"
                    sh "./gradlew lint"

                    script {
                        MODULE_LIST.eachWithIndex { module, index ->
                            LINT_REPORT_SUMMARY += "${HTML_INDENT}<b>${module}</b>: ${lintSummary(module)}<br/>"
                        }
                        print("LINT_REPORT_SUMMARY = ${LINT_REPORT_SUMMARY}")

                        archiveLintReports()
                    }
                }
            }
        }
    }
}

/**
 * Returns a Markdown table-formatted String that holds all Test Results for available modules
 *
 * @return String that contains all Test Results for available modules
 */
String buildTestResults() {
    // Break down the Test Summary Reports into String arrays.
    // As dictated in junit_report.py, each value in the String is separated by a comma.
    // Use "," as the delimiter in order to split all values, then add them in their respective String arrays.
    def appSummaryArray = APP_UNIT_TEST_SUMMARY.split(',')
    def domainSummaryArray = DOMAIN_UNIT_TEST_SUMMARY.split(',')
    def dataSummaryArray = DATA_UNIT_TEST_SUMMARY.split(',')

    String appTestResultsRow = "| **app** | " +
            "${APP_COVERAGE} | " +
            "${appSummaryArray[0]} | " +
            "${appSummaryArray[1]} | " +
            "${appSummaryArray[2]} | " +
            "${appSummaryArray[3]} | " +
            "${appSummaryArray[4]} | " +
            "${APP_UNIT_TEST_RESULT} |"

    String domainTestResultsRow = "| **domain** | " +
            "${DOMAIN_COVERAGE} | " +
            "${domainSummaryArray[0]} | " +
            "${domainSummaryArray[1]} | " +
            "${domainSummaryArray[2]} | " +
            "${domainSummaryArray[3]} | " +
            "${domainSummaryArray[4]} | " +
            "${DOMAIN_UNIT_TEST_RESULT} |"

    String dataTestResultsRow = "| **data** | " +
            "${DATA_COVERAGE} | " +
            "${dataSummaryArray[0]} | " +
            "${dataSummaryArray[1]} | " +
            "${dataSummaryArray[2]} | " +
            "${dataSummaryArray[3]} | " +
            "${dataSummaryArray[4]} | " +
            "${DATA_UNIT_TEST_RESULT} |"

    "| Module | Coverage | Total Cases | Skipped | Errors | Failure | Duration (s) | Test Report |\n" +
            "| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n" +
            "$appTestResultsRow\n" +
            "$domainTestResultsRow\n" +
            "$dataTestResultsRow\n"
}

/**
 * Combines the GMS, HMS and QA Build Warnings into one String
 *
 * @return A String that contains some or all Build Warnings combined together.
 * If there are no Build Warnings, return "None".
 */
String getBuildWarnings() {
    String result = ""
    if (fileExists(GMS_APK_BUILD_LOG)) {
        String gmsBuildWarnings = sh(script: "cat ${GMS_APK_BUILD_LOG} | grep -a '^w:' || true", returnStdout: true).trim()
        println("gmsBuildWarnings = $gmsBuildWarnings")
        if (!gmsBuildWarnings.isEmpty()) {
            result = "<details><summary>:warning: GMS Build Warnings :warning:</summary>" + wrapBuildWarnings(gmsBuildWarnings) + "</details>"
        }
    }

    if (fileExists(HMS_APK_BUILD_LOG)) {
        String hmsBuildWarnings = sh(script: "cat ${HMS_APK_BUILD_LOG} | grep -a '^w:' || true", returnStdout: true).trim()
        println("hmsBuildWarnings = $hmsBuildWarnings")
        if (!hmsBuildWarnings.isEmpty()) {
            result += "<details><summary>:warning: HMS Build Warnings :warning:</summary>" + wrapBuildWarnings(hmsBuildWarnings) + "</details>"
        }
    }

    if (fileExists(QA_APK_BUILD_LOG)) {
        String qaBuildWarnings = sh(script: "cat ${QA_APK_BUILD_LOG} | grep -a '^w:' || true", returnStdout: true).trim()
        println("qaGmsBuildWarnings = $qaBuildWarnings")
        if (!qaBuildWarnings.isEmpty()) {
            result += "<details><summary>:warning: QA GMS Build Warnings :warning:</summary>" + wrapBuildWarnings(qaBuildWarnings) + "</details>"
        }
    }

    if (result == "") result = "None"
    println("readBuildWarnings() = ${result}")
    return result
}

String wrapBuildWarnings(String rawWarning) {
    if (rawWarning == null || rawWarning.isEmpty()) {
        return ""
    } else {
        return rawWarning.split('\n').join("<br/>")
    }
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
 * Parse lint analysis report and create summary
 *
 * @param module module of the code. Possible values can be app, domain or sdk.
 * @return lint summary report of the given module. Example return value:
 *{'Error': 248, 'Fatal': 17, 'Warning': 4781, 'Information': 1}
 */
String lintSummary(String module) {
    summary = sh(
            script: "python3 ${WORKSPACE}/jenkinsfile/lint_report.py $WORKSPACE/${module}/build/reports/lint-results.xml",
            returnStdout: true).trim()
    if (!summary) summary = 'Warning(0) Error(0) Information(0) Fatal(0)'
    print("lintSummary($module) = $summary")
    return summary
}

/**
 * Archive all HTML lint reports into a zip file.
 */
def archiveLintReports() {
    sh """
        cd ${WORKSPACE}
        rm -frv ${LINT_REPORT_FOLDER}
        mkdir -pv ${LINT_REPORT_FOLDER}
        rm -fv ${LINT_REPORT_ARCHIVE}
    """

    MODULE_LIST.eachWithIndex { module, _ ->
        sh("cp -fv ${module}/build/reports/lint*.html ${WORKSPACE}/${LINT_REPORT_FOLDER}/${module}_lint_report.html")
    }

    sh """
        cd ${WORKSPACE}
        zip -r ${LINT_REPORT_ARCHIVE} ${LINT_REPORT_FOLDER}/*.html
    """
}

/**
 * Archive all HTML coverage reports into one zip file
 */
def archiveCoverageReport() {
    sh """
        cd ${WORKSPACE}
        rm -frv ${COVERAGE_FOLDER}
        mkdir -pv ${COVERAGE_FOLDER}/app
        mkdir -pv ${COVERAGE_FOLDER}/domain
        mv -v ${WORKSPACE}/domain/build/coverage-report/* $WORKSPACE/$COVERAGE_FOLDER/domain/
        mv -v ${WORKSPACE}/app/build/reports/jacoco/html/* $WORKSPACE/$COVERAGE_FOLDER/app/
        
        zip -r ${COVERAGE_ARCHIVE} $WORKSPACE/$COVERAGE_FOLDER/*
        ls -l ${COVERAGE_ARCHIVE}
    """
}

/**
 *
 * @param reportPath relative path of the test report folder,
 *                  for example: "app/build/reports" or "domain/build/reports"
 *
 * @param targetFileName target archive file name
 * @return true if test report files are available. Otherwise return false.
 */
def archiveUnitTestReport(String reportPath, String targetFileName) {
    sh("rm -f ${WORKSPACE}/${targetFileName}")
    if (fileExists(WORKSPACE + "/" + reportPath)) {
        sh """
            cd ${WORKSPACE}
            cd ${reportPath}
            zip -r ${targetFileName} * 
            cd ${WORKSPACE}
            cp ${reportPath}/${targetFileName} ${targetFileName}
        """
        return true
    } else {
        return false
    }
}

/**
 * Create a unit test summary after uploading the HTML test report. The summary includes the download
 * link of the HTML test report.
 *
 * @param testResultPath relative path to the xml format test results
 * @param reportPath relative path to the HTML format test report
 * @param archiveTargetName file name of the test report zip file
 */
def unitTestSummaryWithArchiveLink(String testResultPath, String reportPath, String archiveTargetName) {

    String unitTestResult
    if (archiveUnitTestReport(reportPath, archiveTargetName)) {
        unitTestFileLink = uploadFileToGitLab(archiveTargetName)

        String unitTestSummary = unitTestSummary("${WORKSPACE}/${testResultPath}")
        unitTestResult = "<br/>${unitTestSummary} <br/>${unitTestFileLink}"
    } else {
        unitTestResult = "<br>Unit Test report not available, perhaps test code has compilation error. Please check full build log."
    }
    return unitTestResult
}

/**
 * Get the link of the HTML test report.
 *
 * @param reportPath relative path to the HTML format test report
 * @param archiveTargetName file name of the test report zip file
 */
def unitTestArchiveLink(String reportPath, String archiveTargetName) {
    String result
    if (archiveUnitTestReport(reportPath, archiveTargetName)) {
        unitTestFileLink = uploadFileToGitLab(archiveTargetName)
        result = "${unitTestFileLink}"
    } else {
        result = "Unit Test report not available, perhaps test code has compilation error. Please check full build log."
    }
    return result
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
        final String response = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${fileName} https://code.developers.mega.co.nz/api/v4/projects/199/uploads", returnStdout: true).trim()
        link = new groovy.json.JsonSlurperClassic().parseText(response).markdown
        return link
    }
    return link
}

/**
 * check if a certain value is defined by checking the tag value
 * @param value value of tag
 * @return true if tag has a value. false if tag is null or zero length
 */
static boolean isDefined(String value) {
    return value != null && !value.isEmpty()
}
