@Library('jenkins-android-shared-lib') _

BUILD_START_TIME = System.currentTimeMillis()


BUILD_STEP = ""

GMS_APK_BUILD_LOG = "gms_build.log"
QA_APK_BUILD_LOG = "qa_build.log"

LINT_REPORT_FOLDER = "lint_reports"
LINT_REPORT_ARCHIVE = "lint_reports.zip"
LINT_REPORT_SUMMARY_MAP = [:]

MERGE_REQUEST_FILE_CHANGES_MESSAGE = ""
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

        NDK_ROOT = "/opt/buildtools/android-sdk/ndk/27.1.12297006"
        JAVA_HOME = "/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx"
        ANDROID_HOME = "/opt/buildtools/android-sdk"

        // PATH for necessary commands
        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        // Jenkins build log will be saved in this file.
        CONSOLE_LOG_FILE = "console.txt"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'

        IS_CI_BUILD = 'true'
    }
    post {
        failure {
            script {
                common = load('jenkinsfile/common.groovy')

                // download Jenkins console log
                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                // upload Jenkins console log
                String mrNumber = common.getMrNumber()
                String folder = "android-build/MR-${mrNumber}"
                String jenkinsLog = common.uploadFileToArtifactory(folder, CONSOLE_LOG_FILE)

                // upload unit test report if unit test fail, wrapped in a collapsible details tag
                String unitTestResult = ""
                if (!UNIT_TEST_RESULT_LINK_MAP.isEmpty()) {
                    unitTestResult += "<details><summary><b>Unit Test Results</b></summary>"

                    boolean first = true
                    for (def module in UNIT_TEST_RESULT_LINK_MAP.keySet()) {
                        String result = UNIT_TEST_RESULT_LINK_MAP[module]
                        if (first) {
                            unitTestResult += "$module Unit Test: [$module test report](${result})"
                            first = false
                        } else {
                            unitTestResult += "<br>$module Unit Test: [$module test report](${result})"
                        }
                    }

                    unitTestResult += "</details>"
                }

                // Calculate build duration
                String duration = getBuildDurationStr()

                def failureMessage = ":x: Build Failed(Build: ${env.BUILD_NUMBER}) (Duration: ${duration})" +
                        "<br/>Failure Stage: ${BUILD_STEP}" +
                        "<br/>Last Commit Message: ${getLastCommitMessage()}" +
                        "Last Commit ID: ${env.GIT_COMMIT}" +
                        "<br/>Build Log: [${env.CONSOLE_LOG_FILE}](${jenkinsLog})" +
                        unitTestResult
                common.sendToMR(failureMessage)
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
                        String coverageMessage = buildLintSummaryTable(JSON_LINT_REPORT_LINK, LINT_REPORT_SUMMARY_MAP) + "\n\n" +
                                COVERAGE_SUMMARY

                        String duration = getBuildDurationStr()
                        String buildMessage = ":white_check_mark: Build Succeeded!(Build: ${env.BUILD_NUMBER}) (Duration: ${duration})\n\n" +
                                "**Last Commit:** (${env.GIT_COMMIT})" + getLastCommitMessage() +
                                "**Build Warnings:**\n" + getBuildWarnings() + "\n\n"

                        // Create the String to be posted as a comment in Gitlab
                        String mergeRequestMessage
                        if (!MERGE_REQUEST_FILE_CHANGES_MESSAGE.isBlank()) {
                            mergeRequestMessage = buildMessage + MERGE_REQUEST_FILE_CHANGES_MESSAGE + "\n\n" + coverageMessage
                        } else {
                            mergeRequestMessage = buildMessage + coverageMessage
                        }

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

                        gitlabCommitStatus(name: 'Build APK (GMS+QA)') {
                            script {
                                common.downloadDependencyLibForSdk()

                                util.useArtifactory() {
                                    sh "./gradlew app:assembleGmsDebug --no-daemon 2>&1  | tee ${GMS_APK_BUILD_LOG}"
                                    sh "./gradlew app:assembleGmsQa --no-daemon 2>&1  | tee ${QA_APK_BUILD_LOG}"
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

                                util.useGitLab() {
                                    String htmlOutput = "mr-file-changes.html"
                                    try {
                                        sh "./gradlew --no-daemon checkMergeRequestFileChanges --html-output $htmlOutput"
                                    } finally {
                                        MERGE_REQUEST_FILE_CHANGES_MESSAGE = getHtmlReport(htmlOutput, "")
                                    }
                                }
                            }
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
                                util.useArtifactory() {
                                    def moduleList = common.getUnitTestModuleList()
                                    try {
                                        sh "./gradlew --no-daemon runAllUnitTestsWithCoverage"
                                    } finally {

                                        for (int i = 0; i < moduleList.size(); i++) {
                                            String module = moduleList[i]
                                            UNIT_TEST_RESULT_LINK_MAP.put(
                                                    module,
                                                    unitTestArchiveLink("${module}/build/unittest/html", "unit_test_result_${module.replace('/', '_')}.zip")
                                            )
                                        }
                                    }

                                    String htmlOutput = "coverage.html"
                                    sh "./gradlew --no-daemon collectCoverage --modules \"${moduleList.join(",")}\" --html-output ${htmlOutput}"
                                    COVERAGE_SUMMARY = getHtmlReport(htmlOutput, "No coverage report found")
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
                        gitlabCommitStatus(name: 'Lint Check') {
                            script {
                                common.downloadDependencyLibForSdk()

                                util.useArtifactory() {
                                    sh "mv custom_lint.xml lint.xml"
                                    sh "./gradlew --no-daemon lint"
                                }

                                def lintModuleList = common.getModuleList()

                                lintModuleList.each { module ->
                                    def lintJsonContent = generateLintSummary(module)
                                    checkFatalErrors(lintJsonContent)
                                    LINT_REPORT_SUMMARY_MAP.put(module, lintJsonContent)
                                }
                                archiveLintReports(lintModuleList)

                                this.JSON_LINT_REPORT_LINK = common.uploadFileToArtifactory(LINT_REPORT_ARCHIVE)
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
 * @param lintReportSummaryMap a Map of all Modules with their corresponding Lint Results
 *
 * @return a Markdown table-formatted String
 */
String buildLintSummaryTable(String jsonLintReportLink, Map lintReportSummaryMap) {
    println("Entering buildLintSummaryTable()")

    // Declare the initial value for the Table String
    String tableStr = "| Module | Fatal | Error | Warning | Information | Error Message |\n" +
            "| :---: | :---: | :---: | :---: | :---: | :---: |\n"

    int fatalCount = 0
    int errorCount = 0
    int warningCount = 0
    // Iterate through all the values in LINT_REPORT_SUMMARY_MAP and add a row per module
    // The standard method of iterating a map returns an error when used with a Jenkins pipeline,
    // which is why the map iteration is set up in this manner
    for (def key in lintReportSummaryMap.keySet()) {
        def lintJsonContent = lintReportSummaryMap[key]

        // Add a new row to the table
        tableStr += "| **$key** " +
                "| $lintJsonContent.fatalCount " +
                "| $lintJsonContent.errorCount " +
                "| $lintJsonContent.warningCount " +
                "| $lintJsonContent.informationCount " +
                "| $lintJsonContent.errorMessage |\n"
        fatalCount += lintJsonContent.fatalCount as int
        errorCount += lintJsonContent.errorCount as int
        warningCount += lintJsonContent.warningCount as int
    }

    // Create Summary to be returned after iterating through all modules
    String lintSummary = "<details><summary><b>Lint Summary:</b> Fatal(${fatalCount}) Error(${errorCount}) Warning(${warningCount})</summary>" + "\n [lint_report](${jsonLintReportLink}) \n\n" + tableStr + "</details>"

    // Return the final result
    lintSummary
}

String getHtmlReport(String reportPath, String messageOnMissingFile) {
    String htmlReport
    if (fileExists(reportPath)) {
        htmlReport = readFile(reportPath)
    } else {
        htmlReport = messageOnMissingFile
    }
    return htmlReport
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

static String wrapBuildWarnings(String rawWarning) {
    if (rawWarning == null || rawWarning.isEmpty()) {
        return ""
    } else {
        return rawWarning.split('\n').join("<br/>")
    }
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
    def reportsDir = "$WORKSPACE/${module}/build/reports"
    
    // Find lint XML result files, which matches "lint-*.xml"
    def lintResultsFiles = sh(
        script: "ls ${reportsDir}/lint-*.xml 2>/dev/null || true",
        returnStdout: true
    ).trim().split("\\r?\\n").findAll { it }

    if (!lintResultsFiles) {
        print("No lint-*.xml file found in ${reportsDir}")
        return [
                "fatalCount": 0,
                "errorCount": 0,
                "warningCount": 0,
                "informationCount": 0,
                "errorMessage": "No lint results found"
        ]
    }

    // Process first lint results file
    def lintResultsFile = lintResultsFiles[0]
    def targetFile = "${module}_processed-lint-results.json"

    // Generate JSON report from XML
    sh "./gradlew --no-daemon generateLintReport --lint-results ${lintResultsFile} --target-file ${targetFile}"
    
    // Parse JSON report
    def lintJsonFile = readFile(targetFile)
    def lintJsonContent = new HashMap(new groovy.json.JsonSlurper().parseText(lintJsonFile))
    
    print("lintSummary($module) = ${lintJsonContent}")
    return lintJsonContent
}

/**
 * Archive all HTML lint reports into a zip file.
 */
def archiveLintReports(List<String> moduleList) {
    sh """
        cd ${WORKSPACE}
        rm -frv ${LINT_REPORT_FOLDER}
        mkdir -pv ${LINT_REPORT_FOLDER}
        rm -fv ${LINT_REPORT_ARCHIVE}
    """

    moduleList.each { module ->
        sh("cp -fv ${module}/build/reports/lint*.html ${WORKSPACE}/${LINT_REPORT_FOLDER}/${module.replace('/', '_')}_lint_report.html 2>/dev/null || true")
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
        unitTestFileLink = common.uploadFileToArtifactory(archiveTargetName)
        result = "${unitTestFileLink}"
    } else {
        result = "Unit Test report not available, perhaps test code has compilation error. Please check full build log."
    }
    return result
}

/**
 * Checks if the specific module has any Fatal Errors or not. The Pipeline automatically fails if
 * at least one Fatal Error was found
 *
 * @param lintJsonContent A List containing the module's Lint Summary.
 * Here's a Sample Result:
 *
 * [errorCount:20, errorMessage:None, fatalCount:10, informationCount:40, warningCount:30]
 */
def checkFatalErrors(def lintJsonContent) {
    println("Check if there are Fatal Lint errors. ${lintJsonContent}")
    def fatalCount = lintJsonContent.fatalCount as int
    if (fatalCount > 0) {
        util.failPipeline("!!!!!!!! There are ${fatalCount} fatal lint errors. Build is failing.")
    }
}

/**
 * Get the build duration in minutes and seconds.
 *
 * @return A String that contains the build duration in minutes and seconds.
 */
String getBuildDurationStr() {
    long BUILD_END_TIME = System.currentTimeMillis()
    long durationMillis = BUILD_END_TIME - BUILD_START_TIME
    int minutes = (int)(durationMillis / 1000 / 60)
    int seconds = (int)((int)(durationMillis / 1000) % 60)
    return String.format("%dm %02ds", minutes, seconds)
}

