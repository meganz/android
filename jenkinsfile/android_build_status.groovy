import groovy.json.JsonSlurperClassic

BUILD_STEP = ""

GMS_APK_BUILD_LOG = "gms_build.log"
QA_APK_BUILD_LOG = "qa_build.log"

MODULE_LIST = ['app', 'domain', 'shared/original-core-ui', 'data', 'feature/sync','feature/devicecenter','legacy-core-ui']

LINT_REPORT_FOLDER = "lint_reports"
LINT_REPORT_ARCHIVE = "lint_reports.zip"
LINT_REPORT_SUMMARY_MAP = [:]

COVERAGE_SUMMARY = ""

// key is module name, value is the link of the unit test html report uploaded to GitLab
UNIT_TEST_RESULT_LINK_MAP = [:]

JSON_LINT_REPORT_LINK = ""

NODE_LABELS = 'mac-jenkins-slave-android || mac-jenkins-slave'


/**
 * Folder to contain build outputs, including APK, AAG and symbol files
 */
ARCHIVE_FOLDER = "archive"

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
    println("entering getLastCommitMessage()")
    def lastCommitMessage = env.GITLAB_OA_LAST_COMMIT_MESSAGE
    if (lastCommitMessage == null) {
        return '**N/A**'
    } else {
        // use markdown backticks to format commit message into a code block
        return "\n```\n$lastCommitMessage\n```\n".stripIndent().stripMargin()
    }
}

pipeline {
    agent { label NODE_LABELS }
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

        IS_CI_BUILD = 'true'
    }
    post {
        failure {
            script {
                common = load('jenkinsfile/common.groovy')

                if (common.hasGitLabMergeRequest()) {

                    // download Jenkins console log
                    common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                    // upload Jenkins console log
                    String jsonJenkinsLog = common.uploadFileToGitLab(CONSOLE_LOG_FILE)

                    // upload unit test report if unit test fail

                    String unitTestResult = ""
                    for (def module in UNIT_TEST_RESULT_LINK_MAP.keySet()) {
                        String result = UNIT_TEST_RESULT_LINK_MAP[module]
                        unitTestResult += "<br>$module Unit Test: ${result}"
                    }

                    def failureMessage = ":x: Build Failed(Build: ${env.BUILD_NUMBER})" +
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
                        // Create the String to be posted as a comment in Gitlab
                        String mergeRequestMessage = ":white_check_mark: Build Succeeded!(Build: ${env.BUILD_NUMBER})\n\n" +
                                "**Last Commit:** (${env.GIT_COMMIT})" + getLastCommitMessage() +
                                "**Build Warnings:**\n" + getBuildWarnings() + "\n\n" +
                                buildLintSummaryTable(JSON_LINT_REPORT_LINK) + "\n\n" +
                                COVERAGE_SUMMARY

                        common.sendToMR(mergeRequestMessage)
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

                        sh("rm -frv $ARCHIVE_FOLDER")
                        sh("mkdir -p ${WORKSPACE}/${ARCHIVE_FOLDER}")
                    }
                }
            }
        }
        stage("Build, Test and Lint") {
            when {
                expression { (!shouldSkipBuild()) }
            }
            parallel {
                stage('Build APK (GMS+QA)') {
                    agent { label NODE_LABELS }
                    when {
                        expression { (!shouldSkipBuild()) }
                    }
                    steps {
                        script {
                            common.downloadDependencyLibForSdk()
                        }
                        gitlabCommitStatus(name: 'Build APK (GMS+QA)') {

                            withCredentials([
                                    string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                                    string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN'),
                            ]) {
                                withEnv([
                                        "ARTIFACTORY_USER=${ARTIFACTORY_USER}",
                                        "ARTIFACTORY_ACCESS_TOKEN=${ARTIFACTORY_ACCESS_TOKEN}"
                                ]) {
                                    sh "./gradlew app:assembleGmsDebug 2>&1  | tee ${GMS_APK_BUILD_LOG}"
                                    sh "./gradlew app:assembleGmsQa 2>&1  | tee ${QA_APK_BUILD_LOG}"
                                }
                            }

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
                    post {
                        failure {
                            script {
                                BUILD_STEP = "Build APK (GMS+QA)"
                            }
                        }
                        cleanup {
                            cleanWs(cleanWhenFailure: true)
                        }
                    }
                } //stage('Build APK (GMS+QA)')

                stage('Unit Test and Code Coverage') {
                    agent { label NODE_LABELS }
                    when {
                        expression { (!shouldSkipBuild()) }
                    }
                    steps {
                        script {
                            common.downloadDependencyLibForSdk()
                        }
                        gitlabCommitStatus(name: 'Unit Test and Code Coverage') {
                            script {
                                withCredentials([
                                        string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                                        string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN'),
                                ]) {
                                    withEnv([
                                            "ARTIFACTORY_USER=${ARTIFACTORY_USER}",
                                            "ARTIFACTORY_ACCESS_TOKEN=${ARTIFACTORY_ACCESS_TOKEN}"
                                    ]) {
                                        String buildReportPath = "build/unittest/html"
                                        try {
                                            sh "./gradlew domain:jacocoTestReport"
                                        } finally {
                                            // if gradle command fails, we collect the test report. And the build will discontinue.
                                            UNIT_TEST_RESULT_LINK_MAP.put("domain", unitTestArchiveLink("domain/$buildReportPath", "unit_test_result_domain.zip"))
                                        }

                                        try {
                                            sh "./gradlew data:testDebugUnitTestCoverage"
                                        } finally {
                                            UNIT_TEST_RESULT_LINK_MAP.put("data", unitTestArchiveLink("data/$buildReportPath", "unit_test_result_data.zip"))
                                        }

                                        try {
                                            sh "./gradlew app:createUnitTestCoverageReport"
                                        } finally {
                                            UNIT_TEST_RESULT_LINK_MAP.put("app", unitTestArchiveLink("app/$buildReportPath", "unit_test_result_app.zip"))
                                        }

                                        try {
                                            sh "./gradlew feature:devicecenter:testDebugUnitTestCoverage"
                                        } finally {
                                            UNIT_TEST_RESULT_LINK_MAP.put("feature/devicecenter", unitTestArchiveLink("feature/devicecenter/$buildReportPath", "unit_test_result_feature_devicecenter.zip"))
                                        }

                                        try {
                                            sh "./gradlew feature:sync:testDebugUnitTestCoverage"
                                        } finally {
                                            UNIT_TEST_RESULT_LINK_MAP.put("feature/sync", unitTestArchiveLink("feature/sync/$buildReportPath", "unit_test_result_feature_sync.zip"))
                                        }

                                        try {
                                            sh "./gradlew shared:original-core-ui:testDebugUnitTestCoverage"
                                        } finally {
                                            UNIT_TEST_RESULT_LINK_MAP.put("shared/original-core-ui", unitTestArchiveLink("shared/original-core-ui/$buildReportPath", "unit_test_result_shared_original_core_ui.zip"))
                                        }

                                        try {
                                            sh "./gradlew legacy-core-ui:testDebugUnitTestCoverage"
                                        } finally {
                                            UNIT_TEST_RESULT_LINK_MAP.put("legacy-core-ui", unitTestArchiveLink("legacy-core-ui/$buildReportPath", "unit_test_result_legacy_core_ui.zip"))
                                        }

                                        String htmlOutput = "coverage.html"
                                        sh "./gradlew collectCoverage --modules \"${MODULE_LIST.join(",")}\" --html-output ${htmlOutput}"
                                        COVERAGE_SUMMARY = getCoverageHtmlReport(htmlOutput)
                                    }
                                }
                            }
                        }
                    }
                    post {
                        failure {
                            script {
                                BUILD_STEP = "Unit Test and Code Coverage"
                            }
                        }
                        cleanup {
                            cleanWs(cleanWhenFailure: true)
                        }
                    }
                } //stage('Unit Test and Code Coverage')

                stage('Lint Check') {
                    agent { label NODE_LABELS }
                    when {
                        expression { (!shouldSkipBuild()) }
                    }
                    steps {
                        // Run Lint and analyse the results
                        script {
                            common.downloadDependencyLibForSdk()
                        }

                        gitlabCommitStatus(name: 'Lint Check') {

                            withCredentials([
                                    string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                                    string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN'),
                            ]) {
                                withEnv([
                                        "ARTIFACTORY_USER=${ARTIFACTORY_USER}",
                                        "ARTIFACTORY_ACCESS_TOKEN=${ARTIFACTORY_ACCESS_TOKEN}"
                                ]) {
                                    sh "mv custom_lint.xml lint.xml"
                                    sh "./gradlew lint"
                                }
                            }

                            script {
                                MODULE_LIST.each { module ->
                                    LINT_REPORT_SUMMARY_MAP.put(module, lintSummary(module))
                                }
                                archiveLintReports()

                                JSON_LINT_REPORT_LINK = common.uploadFileToGitLab(LINT_REPORT_ARCHIVE)
                            }
                        }
                    }
                    post {
                        failure {
                            script {
                                BUILD_STEP = "Lint Check"
                            }
                        }
                        cleanup {
                            cleanWs(cleanWhenFailure: true)
                        }
                    }
                }  //stage('Lint Check')
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
    println("Entering buildLintSummaryTable()")
    // Declare a JsonSlurperClassic object
    def jsonSlurperClassic = new JsonSlurperClassic()

    // Declare the initial value for the Table String
    String tableStr = "| Module | Fatal | Error | Warning | Information | Error Message |\n" +
            "| :---: | :---: | :---: | :---: | :---: | :---: |\n"

    int fatalCount = 0
    int errorCount = 0
    int warningCount = 0
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
        fatalCount += jsonObject.fatalCount as int
        errorCount += jsonObject.errorCount as int
        warningCount += jsonObject.warningCount as int
    }

    // Create Summary to be returned after iterating through all modules
    String lintSummary = "<details><summary><b>Lint Summary:</b> Fatal(${fatalCount}) Error(${errorCount}) Warning(${warningCount})</summary>" + "\n ${jsonLintReportLink} \n\n" + tableStr + "</details>"

    // Return the final result
    lintSummary
}

String getCoverageHtmlReport(String reportPath) {
    String coverageReport
    if (fileExists(reportPath)) {
        coverageReport = readFile(reportPath)
    } else {
        coverageReport = "No coverage report found"
    }
    return coverageReport
}

/**
 * Combines the GMS and QA Build Warnings into one String
 *
 * @return A String that contains some or all Build Warnings combined together.
 * If there are no Build Warnings, return "None".
 */
String getBuildWarnings() {
    println("Entering getBuildWarnings()")
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
        sh("cp -fv ${module}/build/reports/lint*.html ${WORKSPACE}/${LINT_REPORT_FOLDER}/${module.replace('/', '_')}_lint_report.html")
    }

    sh """
        cd ${WORKSPACE}
        zip -r ${LINT_REPORT_ARCHIVE} ${LINT_REPORT_FOLDER}/*.html
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
 * Get the link of the HTML test report.
 *
 * @param reportPath relative path to the HTML format test report
 * @param archiveTargetName file name of the test report zip file
 */
def unitTestArchiveLink(String reportPath, String archiveTargetName) {
    String result
    if (archiveUnitTestReport(reportPath, archiveTargetName)) {
        common = load('jenkinsfile/common.groovy')
        unitTestFileLink = common.uploadFileToGitLab(archiveTargetName)
        result = "${unitTestFileLink}"
    } else {
        result = "Unit Test report not available, perhaps test code has compilation error. Please check full build log."
    }
    return result
}
