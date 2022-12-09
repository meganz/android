import groovy.json.JsonSlurperClassic

import java.math.RoundingMode
import java.text.DecimalFormat

BUILD_STEP = ""

GMS_APK_BUILD_LOG = "gms_build.log"
QA_APK_BUILD_LOG = "qa_build.log"

MODULE_LIST = ['app', 'domain', 'sdk', 'data']

LINT_REPORT_FOLDER = "lint_reports"
LINT_REPORT_ARCHIVE = "lint_reports.zip"
LINT_REPORT_SUMMARY_MAP = [:]

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
ARTIFACTORY_DEVELOP_CODE_COVERAGE = ""

/**
 * common.groovy file with common methods
 */
def common

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
        return "\n```\n$lastCommitMessage\n```\n".stripIndent().stripMargin()
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
                                buildLintSummaryTable(jsonLintReportLink) + "\n\n" +
                                buildCodeComparisonResults()

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
        stage('Build APK (GMS+QA)') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                script {
                    BUILD_STEP = 'Build APK (GMS+QA)'
                }
                gitlabCommitStatus(name: 'Build APK (GMS+QA)') {
                    // Finish building and packaging the APK
                    sh "./gradlew clean"
                    sh "./gradlew app:assembleGmsRelease 2>&1  | tee ${GMS_APK_BUILD_LOG}"
                    sh "./gradlew app:assembleGmsQa 2>&1  | tee ${QA_APK_BUILD_LOG}"

                    sh """
                        if grep -q -m 1 \"^FAILURE: \" ${GMS_APK_BUILD_LOG}; then
                            echo GMS APK build failed. Exitting....
                            exit 1
                        fi
                        if grep -q -m 1 \"^FAILURE: \" ${QA_APK_BUILD_LOG}; then
                            echo QA APK build failed. Exitting....
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
        stage('Compare Coverage') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                script {
                    BUILD_STEP = "Compare Coverage"
                }
                gitlabCommitStatus(name: 'Compare Coverage') {
                    script {
                        withCredentials([
                                string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                                string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN')
                        ]) {
                            String developCoverageLocation = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/cicd/coverage/coverage_summary.txt"

                            // Navigate to the "/cicd/coverage/" path
                            // Download the code coverage from Artifactory.
                            // Afterwards, rename the downloaded code coverage text file to "develop_coverage_summary.txt"
                            sh """
                                cd ${WORKSPACE}
                                rm -frv cicd
                                mkdir -pv ${WORKSPACE}/cicd/coverage
                                cd ${WORKSPACE}/cicd/coverage
                                curl -u ${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -o develop_coverage_summary.txt ${developCoverageLocation}
                                ls
                            """

                            // Once the file has been downloaded, call the script to parse the Code Coverage results
                            ARTIFACTORY_DEVELOP_CODE_COVERAGE = "${getArtifactoryDevelopCodeCoverage("$WORKSPACE/cicd/coverage/develop_coverage_summary.txt")}"
                            println("ARTIFACTORY_DEVELOP_CODE_COVERAGE from Groovy: ${ARTIFACTORY_DEVELOP_CODE_COVERAGE}")
                        }
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
                        MODULE_LIST.each { module ->
                            LINT_REPORT_SUMMARY_MAP.put(module, lintSummary(module))
                        }
                        archiveLintReports()
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
String buildLintSummaryTable(String jsonLintReportLink) {

    // Declare a JsonSlurperClassic object
    def jsonSlurperClassic = new JsonSlurperClassic()

    // Declare the initial value for the String
    String tableStr = "**Lint Summary:** (${jsonLintReportLink}):" + "\n\n" +
            "| Module | Fatal | Error | Warning | Information | Error Message |\n" +
            "| :---: | :---: | :---: | :---: | :---: | :---: |\n"

    // Iterate through all the values in LINT_REPORT_SUMMARY_MAP and add a row per module
    // The standard method of iterating a map returns an error when used with a Jenkins pipeline,
    // which is why the map iteration is set up in this manner
    for (def key in LINT_REPORT_SUMMARY_MAP.keySet()) {
        // Parse the JSON String received from lint_report.py into a Map
        def jsonObject = jsonSlurperClassic.parseText(LINT_REPORT_SUMMARY_MAP[key])

        // Add a new row to the table
        tableStr += "| **$key** " +
                "| $jsonObject.fatalCount " +
                "| $jsonObject.errorCount " +
                "| $jsonObject.warningCount " +
                "| $jsonObject.informationCount " +
                "| $jsonObject.errorMessage |\n"
    }

    // Return the final result after iterating through all modules
    tableStr
}

/**
 * Compares the Code Coverage results of all available modules between the source branch and
 * the latest develop branch from Artifactory
 *
 * @return A Markdown-formatted table String that contains the Code Coverage results of all
 * available modules between the source branch and the latest develop branch from Artifactory
 */
String buildCodeComparisonResults() {
    def latestDevelopResults = new JsonSlurperClassic().parseText(ARTIFACTORY_DEVELOP_CODE_COVERAGE)

    String tableString = "**Code Coverage and Comparison:**".concat("\n\n")
            .concat("| Module | Test Cases | Coverage | Coverage Change |").concat("\n")
            .concat("|:---|:---|:---|:---|").concat("\n")

    def currentModuleTestCases = []
    def currentModuleCoverage = ""
    def currentModuleTestResultsLink = ""

    for (def latestDevelopModuleResults in latestDevelopResults) {
        // Compare the name from moduleResult with a static module name
        switch (latestDevelopModuleResults.name) {
            case "**app**":
                currentModuleTestCases = APP_UNIT_TEST_SUMMARY.split(',')
                currentModuleCoverage = APP_COVERAGE
                currentModuleTestResultsLink = APP_UNIT_TEST_RESULT
                break
            case "**domain**":
                currentModuleTestCases = DOMAIN_UNIT_TEST_SUMMARY.split(',')
                currentModuleCoverage = DOMAIN_COVERAGE
                currentModuleTestResultsLink = DOMAIN_UNIT_TEST_RESULT
                break
            case "**data**":
                currentModuleTestCases = DATA_UNIT_TEST_SUMMARY.split(',')
                currentModuleCoverage = DATA_COVERAGE
                currentModuleTestResultsLink = DATA_UNIT_TEST_RESULT
                break
        }
        println("Current Coverage of ${latestDevelopModuleResults.name}: $currentModuleCoverage")

        // Build the Columns
        String testCasesColumn = buildTestCasesColumn(currentModuleTestCases, currentModuleTestResultsLink, latestDevelopModuleResults)
        String coverageColumn = buildCoverageColumn(currentModuleCoverage, latestDevelopModuleResults)
        String coverageChangeColumn = buildCoverageChangeColumn(currentModuleCoverage, latestDevelopModuleResults)

        // Add a Column for every item in the list
        tableString = tableString.concat("| ${latestDevelopModuleResults.name} | $testCasesColumn | $coverageColumn | $coverageChangeColumn |").concat("\n")
    }

    return tableString
}

/**
 * Builds a Markdown-formatted String of the test cases of a specific module from the current
 * branch and the latest develop branch in Artifactory
 *
 * @param currentModuleTestCases The Module results of the current branch
 * @param currentModuleTestResultsLink The link to the Module test results of the current branch
 * @param latestDevelopModuleTestCases The Module results of the latest develop branch in Artifactory
 *
 * @return A String that serves as an entry for the "Test Cases" column
 */
String buildTestCasesColumn(def currentModuleTestCases,
                            def currentModuleTestResultsLink,
                            def latestDevelopModuleTestCases
) {
    // Build the "Current Branch" Column first
    String currentBranchColumn = "**Current Branch:**".concat("<br><br>")

    if (currentModuleTestCases[0].toInteger() > 0) {
        currentBranchColumn = currentBranchColumn.concat("_Total Cases:_ ")
                .concat("**${currentModuleTestCases[0]}**").concat("<br>")
    }
    if (currentModuleTestCases[1].toInteger() > 0) {
        currentBranchColumn = currentBranchColumn.concat("_Skipped Cases:_ ")
                .concat("**${currentModuleTestCases[1]}**").concat("<br>")
    }
    if (currentModuleTestCases[2].toInteger() > 0) {
        currentBranchColumn = currentBranchColumn.concat("_Error Cases:_ ")
                .concat("**${currentModuleTestCases[2]}**").concat("<br>")
    }
    if (currentModuleTestCases[3].toInteger() > 0) {
        currentBranchColumn = currentBranchColumn.concat("_Failed Cases:_ ")
                .concat("**${currentModuleTestCases[3]}**").concat("<br>")
    }
    currentBranchColumn = currentBranchColumn.concat("_Duration (s):_ ")
            .concat("**${currentModuleTestCases[4]}**").concat("<br>")
    currentBranchColumn = currentBranchColumn.concat("_Test Report Link:_")
            .concat("<br>").concat(currentModuleTestResultsLink).concat("<br><br>")


    // Afterwards, build the "Latest develop Branch" Column
    String latestDevelopColumn = "**Latest develop Branch:**".concat("<br><br>")

    if (latestDevelopModuleTestCases.totalTestCases.toInteger() > 0) {
        latestDevelopColumn = latestDevelopColumn.concat("_Total Cases:_ ")
                .concat("**${latestDevelopModuleTestCases.totalTestCases}**").concat("<br>")
    }
    if (latestDevelopModuleTestCases.skippedTestCases.toInteger() > 0) {
        latestDevelopColumn = latestDevelopColumn.concat("_Skipped Cases:_ ")
                .concat("**${latestDevelopModuleTestCases.skippedTestCases}**").concat("<br>")
    }
    if (latestDevelopModuleTestCases.errorTestCases.toInteger() > 0) {
        latestDevelopColumn = latestDevelopColumn.concat("_Error Cases:_ ")
                .concat("**${latestDevelopModuleTestCases.errorTestCases}**").concat("<br>")
    }
    if (latestDevelopModuleTestCases.failedTestCases.toInteger() > 0) {
        latestDevelopColumn = latestDevelopColumn.concat("_Failed Cases:_ ")
                .concat("**${latestDevelopModuleTestCases.failedTestCases}**").concat("<br>")
    }
    latestDevelopColumn = latestDevelopColumn.concat("_Duration (s):_ ")
            .concat("**${latestDevelopModuleTestCases.duration}**")

    currentBranchColumn.concat(latestDevelopColumn)
}

/**
 * Builds a Markdown-formatted String of the coverage of a specific module from the current
 * branch and the latest develop branch in Artifactory
 *
 * @param currentModuleCoverage The Module results of the current branch
 * @param latestDevelopModuleCoverage The Module results of the latest develop branch in Artifactory
 *
 * @return A String that serves as an entry for the "Coverage" column
 */
String buildCoverageColumn(def currentModuleCoverage, def latestDevelopModuleCoverage) {
    def df = new DecimalFormat("0.00")

    // Build the "Current Branch" Column first
    String currentBranchColumn = "**Current Branch:**".concat("<br><br>")

    def currentInitialArray = currentModuleCoverage.split('=')
    def currentLineArray = currentInitialArray[1].split('/')

    currentBranchColumn = currentBranchColumn.concat("_Total Lines:_ ")
            .concat("**${currentLineArray[1]}**").concat("<br>")
    currentBranchColumn = currentBranchColumn.concat("_Covered Lines:_ ")
            .concat("**${currentLineArray[0]}**").concat("<br>")
    currentBranchColumn = currentBranchColumn.concat("_Percentage Covered_: ")
            .concat("**${currentInitialArray[0]}**").concat("<br><br>")

    // Afterwards, build the "Latest develop Branch" Column
    String latestDevelopColumn = "**Latest develop Branch:**".concat("<br><br>")

    def latestDevelopTotalLines = Float.parseFloat(latestDevelopModuleCoverage.totalLines)
    def latestDevelopCoveredLines = Float.parseFloat(latestDevelopModuleCoverage.coveredLines)
    def latestDevelopLinePercentage = df.format((latestDevelopCoveredLines / latestDevelopTotalLines) * 100)

    latestDevelopColumn = latestDevelopColumn.concat("_Total Lines:_ ")
            .concat("**${latestDevelopModuleCoverage.totalLines}**").concat("<br>")
    latestDevelopColumn = latestDevelopColumn.concat("_Covered Lines:_ ")
            .concat("**${latestDevelopModuleCoverage.coveredLines}**").concat("<br>")
    latestDevelopColumn = latestDevelopColumn.concat("_Percentage Covered_: ")
            .concat("**$latestDevelopLinePercentage%**")

    return currentBranchColumn.concat(latestDevelopColumn)
}

/**
 * Builds a Markdown-formatted String of the coverage change of a specific module from the current
 * branch and the latest develop branch in Artifactory
 *
 * @param currentModuleCoverage The Module results of the current branch
 * @param latestDevelopModuleCoverage The Module results of the latest develop branch in Artifactory
 *
 * @return A String that serves as an entry for the "Coverage Change" column
 */
String buildCoverageChangeColumn(def currentModuleCoverage, def latestDevelopModuleCoverage) {
    def currentModuleLinePercentage = currentModuleCoverage.split("%")[0]
    def currentModuleBigDecimal = new BigDecimal(currentModuleLinePercentage).setScale(2, RoundingMode.HALF_UP)

    def latestDevelopTotalLines = Float.parseFloat(latestDevelopModuleCoverage.totalLines)
    def latestDevelopCoveredLines = Float.parseFloat(latestDevelopModuleCoverage.coveredLines)
    def latestDevelopLinePercentage = (latestDevelopCoveredLines / latestDevelopTotalLines) * 100
    def latestDevelopBigDecimal = new BigDecimal(latestDevelopLinePercentage).setScale(2, RoundingMode.HALF_UP)

    def result = currentModuleBigDecimal - latestDevelopBigDecimal

    if (result > 0) {
        return "**+$result%**"
    } else if (result < 0) {
        return "**$result%**"
    } else {
        return "**No Change**"
    }
}

/**
 * Combines the GMS and QA Build Warnings into one String
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
 * Parses the file that contains the Code Coverage results of the latest develop branch from Artifactory
 * into a formatted String, so that the results can be easily transformed and displayed in the Gitlab MR
 * @param coveragePath The full path to the file containing the Code Coverage from the latest develop
 * @return A formatted String of the Code Coverage
 */
String getArtifactoryDevelopCodeCoverage(String coveragePath) {
    summary = sh(
            script: "python3 ${WORKSPACE}/jenkinsfile/artifactory_develop_code_coverage.py ${coveragePath}",
            returnStdout: true).trim()
    print("artifactory develop coverage path(${coveragePath}): ${summary}")
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
        final String response = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${fileName} ${env.GITLAB_BASE_URL}/api/v4/projects/199/uploads", returnStdout: true).trim()
        link = new JsonSlurperClassic().parseText(response).markdown
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
