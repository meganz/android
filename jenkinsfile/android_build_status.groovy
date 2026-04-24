@Library('jenkins-android-shared-lib') _

BUILD_START_TIME = System.currentTimeMillis()


BUILD_STEP = ""

GMS_APK_BUILD_LOG = "gms_build.log"
QA_APK_BUILD_LOG = "qa_build.log"

LINT_REPORT_FOLDER = "lint_reports"
LINT_REPORT_ARCHIVE = "lint_reports.zip"
LINT_REPORT_SUMMARY_MAP = [:]

// Per-stage timing for build-stats collection. Keys: 'build_apk_ms', 'unit_test_ms', 'lint_ms'.
STAGE_START_MS = [:]
STAGE_DURATIONS_MS = [:]

// Per-stage agent name. Keys: 'build_apk', 'unit_test', 'lint'.
STAGE_NODE_NAMES = [:]

// Captured at the top of the first agent-running stage so queue-wait (scheduled → agent acquired)
// can be separated from execution time. Null until the agent is acquired.
AGENT_ACQUIRED_TIME = null

MERGE_REQUEST_FILE_CHANGES_MESSAGE = ""
COVERAGE_SUMMARY = ""

// key is module name, value is the link of the unit test html report uploaded to GitLab
UNIT_TEST_RESULT_LINK_MAP = [:]

JSON_LINT_REPORT_LINK = ""

NODE_LABELS = 'mac-jenkins-slave-android'

/**
 * Folder to contain build outputs, including APK, AAG and symbol files
 */
ARCHIVE_FOLDER = "archive"

CODE_REVIEW_CMD = "code_review"
CODE_REVIEW_OUTPUT_FILE = "code_review_report.md"
CODE_REVIEW_SUMMARY_FILE = "code_review_summary.md"
CODE_REVIEW_ERROR_REPORT_FILE = "code_review_error_report.txt"

/**
 * common.groovy file with common methods
 */
def common

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

                // upload unit test reports for failed modules only, wrapped in a collapsible details tag
                String unitTestResult = ""
                if (!UNIT_TEST_RESULT_LINK_MAP.isEmpty()) {
                    unitTestResult += "<details><summary><b>Failed Unit Test Results</b></summary>"

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

                String lintSummaryMessage = ""
                if (BUILD_STEP == "Lint Check" && !LINT_REPORT_SUMMARY_MAP.isEmpty()) {
                    lintSummaryMessage = "\n\n" + buildLintSummaryTable(JSON_LINT_REPORT_LINK, LINT_REPORT_SUMMARY_MAP)
                }

                def failureMessage = ":x: Build Failed(Build: ${env.BUILD_NUMBER}) (Duration: ${duration})" +
                        "<br/>Failure Stage: ${BUILD_STEP}" +
                        "<br/>Last Commit Message: ${getLastCommitMessage()}" +
                        "Last Commit ID: ${env.GIT_COMMIT}" +
                        "<br/>Build Log: [${env.CONSOLE_LOG_FILE}](${jenkinsLog})" +
                        unitTestResult +
                        lintSummaryMessage
                common.sendToMR(failureMessage)

                common.recordBuildStats(
                        common.collectBuildStats(
                                AGENT_ACQUIRED_TIME ?: BUILD_START_TIME,
                                STAGE_DURATIONS_MS,
                                STAGE_NODE_NAMES,
                                currentBuild.currentResult,
                                shouldSkipBuild(),
                                isCodeReviewOnly()
                        )
                )
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
                                "\n\n**Build Warnings:**\n" + getBuildWarnings() + "\n\n"

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

                common.recordBuildStats(
                        common.collectBuildStats(
                                AGENT_ACQUIRED_TIME ?: BUILD_START_TIME,
                                STAGE_DURATIONS_MS,
                                STAGE_NODE_NAMES,
                                currentBuild.currentResult,
                                shouldSkipBuild(),
                                isCodeReviewOnly()
                        )
                )
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
                    AGENT_ACQUIRED_TIME = System.currentTimeMillis()
                    BUILD_STEP = 'Preparation'

                    common = load('jenkinsfile/common.groovy')
                }
            }
        }
        stage('Preparation') {
            when {
                expression { !shouldSkipBuild() }
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
                expression { !shouldSkipBuild() }
            }
            parallel {
                stage('Build APK (GMS+QA)') {
                    when {
                        expression { !shouldSkipBuild() && !isCodeReviewOnly() }
                    }
                    steps {

                        gitlabCommitStatus(name: 'Build APK (GMS+QA)') {
                            script {
                                STAGE_START_MS['build_apk_ms'] = System.currentTimeMillis()
                                STAGE_NODE_NAMES['build_apk'] = env.NODE_NAME
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
                                        sh "./gradlew --no-daemon checkMergeRequestFileChanges --html-output $htmlOutput --current-branch=\"${env.GIT_BRANCH}\" --target-branch=\"${env.GITLAB_OA_TARGET_BRANCH}\""
                                    } finally {
                                        MERGE_REQUEST_FILE_CHANGES_MESSAGE = getHtmlReport(htmlOutput, "")
                                    }
                                }
                            }
                        }
                    }
                    post {
                        always {
                            script {
                                def s = STAGE_START_MS['build_apk_ms']
                                if (s != null) {
                                    STAGE_DURATIONS_MS['build_apk_ms'] = System.currentTimeMillis() - s
                                }
                            }
                        }
                        failure {
                            script {
                                BUILD_STEP = "Build APK (GMS+QA)"
                            }
                        }
                    }
                } //stage('Build APK (GMS+QA)')

                stage('Unit Test and Code Coverage') {
                    agent { label NODE_LABELS }
                    when {
                        expression { !shouldSkipBuild() && !isCodeReviewOnly() }
                    }
                    steps {
                        script {
                            STAGE_START_MS['unit_test_ms'] = System.currentTimeMillis()
                            STAGE_NODE_NAMES['unit_test'] = env.NODE_NAME
                        }
                        gitlabCommitStatus(name: 'Unit Test and Code Coverage') {
                            script {
                                util.useArtifactory() {
                                    def moduleList = common.getUnitTestModuleList()
                                    def failedModules = []

                                    try {
                                        sh "./gradlew --no-daemon runAllUnitTestsWithCoverage"
                                    } finally {

                                        failedModules = detectFailedTestModules(moduleList)

                                        if (!failedModules.isEmpty()) {
                                            for (int i = 0; i < failedModules.size(); i++) {
                                                String module = failedModules[i]
                                                UNIT_TEST_RESULT_LINK_MAP.put(
                                                        module,
                                                        unitTestArchiveLink("${module}/build/unittest/html", "unit_test_result_${module.replace('/', '_')}.zip")
                                                )
                                            }
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
                        always {
                            script {
                                def s = STAGE_START_MS['unit_test_ms']
                                if (s != null) {
                                    STAGE_DURATIONS_MS['unit_test_ms'] = System.currentTimeMillis() - s
                                }
                            }
                        }
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
                        expression { !shouldSkipBuild() && !isCodeReviewOnly() }
                    }
                    steps {
                        gitlabCommitStatus(name: 'Lint Check') {
                            script {
                                STAGE_START_MS['lint_ms'] = System.currentTimeMillis()
                                STAGE_NODE_NAMES['lint'] = env.NODE_NAME
                                util.useArtifactory() {
                                    sh "mv custom_lint.xml lint.xml"
                                    sh "./gradlew --no-daemon lint"
                                }

                                def lintModuleList = common.getModuleList()

                                def totalFatalLintErrors = generateLintSummary(lintModuleList)
                                archiveLintReports(lintModuleList)

                                this.JSON_LINT_REPORT_LINK = common.uploadFileToArtifactory(LINT_REPORT_ARCHIVE)

                                if (totalFatalLintErrors > 0) {
                                    util.failPipeline("!!!!!!!! There are ${totalFatalLintErrors} fatal lint errors. Build is failing.")
                                }
                            }
                        }
                    }
                    post {
                        always {
                            script {
                                def s = STAGE_START_MS['lint_ms']
                                if (s != null) {
                                    STAGE_DURATIONS_MS['lint_ms'] = System.currentTimeMillis() - s
                                }
                            }
                        }
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

        stage('Code Review') {
            agent { label NODE_LABELS }
            when {
                expression { shouldRunCodeReview() }
            }
            steps {
                gitlabCommitStatus(name: 'Code Review') {
                    script {
                        def skillFile = "${WORKSPACE}/.claude/skills/android-code-review/SKILL.md"
                        def targetBranch = env.GITLAB_OA_TARGET_BRANCH ?: 'develop'

                        util.useGitLab() {
                            withCredentials([string(credentialsId: 'ANTHROPIC_API_KEY', variable: 'ANTHROPIC_API_KEY')]) {
                                try {
                                    sh "./gradlew --no-daemon codeReview --skill '${skillFile}' --output '${CODE_REVIEW_OUTPUT_FILE}' --target-branch '${targetBranch}' --model 'claude-opus-4-6' --summary '${CODE_REVIEW_SUMMARY_FILE}' --error-report '${CODE_REVIEW_ERROR_REPORT_FILE}'"
                                    def summary = "Code Review Report"
                                    if (fileExists(CODE_REVIEW_SUMMARY_FILE)) {
                                        summary = readFile(CODE_REVIEW_SUMMARY_FILE).trim()
                                    }
                                    common.sendFileToMRComment(CODE_REVIEW_OUTPUT_FILE, summary)
                                } catch (Exception e) {
                                    String errorMessage = e.message ?: 'Unknown error'
                                    try {
                                        if (fileExists(CODE_REVIEW_ERROR_REPORT_FILE)) {
                                            errorMessage = readFile(CODE_REVIEW_ERROR_REPORT_FILE).trim()
                                        }
                                    } catch (ignored) {
                                    }

                                    common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                                    String mrNumber = common.getMrNumber()
                                    String folder = "android-build/MR-${mrNumber}"
                                    String jenkinsLog = common.uploadFileToArtifactory(folder, CONSOLE_LOG_FILE)

                                    String failMsg = ":x: **Code Review Failed** (Build: ${env.BUILD_NUMBER})<br/>" +
                                            "Error: ${errorMessage}<br/>" +
                                            "Please check the [build log](${jenkinsLog}) for details."

                                    common.sendToMR(failMsg)
                                }
                            }
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "${CODE_REVIEW_OUTPUT_FILE},${CODE_REVIEW_SUMMARY_FILE},${CODE_REVIEW_ERROR_REPORT_FILE}", allowEmptyArchive: true
                }
                cleanup {
                    cleanWs(cleanWhenFailure: true)
                }
            }
        } //stage('Code Review')
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
 * Executes the `generateLintReport` Gradle task once for all modules and populates
 * LINT_REPORT_SUMMARY_MAP with per-module severity counts parsed from the aggregated JSON.
 *
 * @param moduleList List of module paths (e.g. ["app", "domain", "core:ui"]).
 * @return The total number of fatal lint errors across all modules.
 */
def generateLintSummary(List<String> moduleList) {
    String aggregatedJson = "lint_summary.json"
    sh "./gradlew --no-daemon generateLintReport " +
            "--modules \"${moduleList.join(",")}\" " +
            "--target-file ${aggregatedJson}"

    String aggregatedJsonText = readFile(aggregatedJson)
    def rawModules = new groovy.json.JsonSlurperClassic().parseText(aggregatedJsonText).modules
    int totalFatal = 0
    for (int i = 0; i < moduleList.size(); i++) {
        String module = moduleList[i]
        def raw = rawModules[module]
        def perModule = [
                "fatalCount"      : (raw?.fatalCount ?: 0) as int,
                "errorCount"      : (raw?.errorCount ?: 0) as int,
                "warningCount"    : (raw?.warningCount ?: 0) as int,
                "informationCount": (raw?.informationCount ?: 0) as int,
                "errorMessage"    : (raw == null ? "No lint results found" : (raw.errorMessage ?: "None")).toString()
        ]
        totalFatal += perModule.fatalCount
        print("lintSummary($module) = ${perModule}")
        LINT_REPORT_SUMMARY_MAP.put(module, perModule)
    }

    if (totalFatal > 0) {
        println("Detected ${totalFatal} fatal lint error(s) across all modules.")
    }
    return totalFatal
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

    for (int i = 0; i < moduleList.size(); i++) {
        String module = moduleList[i]
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
 * Detects which modules have failed unit tests by checking their test result files.
 *
 * @param moduleList List of all modules that were tested
 * @return List of module names that have failed tests
 */
def detectFailedTestModules(List<String> moduleList) {
    def failedModules = []

    for (int i = 0; i < moduleList.size(); i++) {
        String module = moduleList[i]
        // Check if failure can be found in XML test reports under "build/unittest/junit"
        if (fileExists("${module}/build/unittest/junit")) {
            def testResultFiles = sh(
                    script: "grep -irE \"failures=\\\"[1-9][0-9]*\\\"\" ${module}/build/unittest/junit/* 2>/dev/null || true",
                    returnStdout: true
            ).trim().split("\\r?\\n").findAll { it }

            if (testResultFiles) {
                failedModules.add(module)
                println("Detected failed tests in module: ${module}")
            }
        } else {
            println("No test result files found for module: ${module}")
        }
    }

    println("Failed modules detected: ${failedModules}")
    return new ArrayList<>(failedModules)
}

/**
 * Get the build duration in minutes and seconds.
 *
 * @return A String that contains the build duration in minutes and seconds.
 */
String getBuildDurationStr() {
    long BUILD_END_TIME = System.currentTimeMillis()
    long durationMillis = BUILD_END_TIME - BUILD_START_TIME
    int minutes = (int) (durationMillis / 1000 / 60)
    int seconds = (int) ((int) (durationMillis / 1000) % 60)
    return String.format("%dm %02ds", minutes, seconds)
}

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

/**
 * Decide whether the Code Review stage should run.
 * - On MR OPEN (initial creation) if not a Draft/WIP.
 * - On-demand via "code_review" comment (NOTE action).
 * Note: does NOT run automatically on subsequent pushes (PUSH action) to keep CI cost low.
 *
 * @return true if code review should run, false otherwise.
 */
def shouldRunCodeReview() {
    // 1. Determine if this is a "Newly Created MR"
    // Condition: It is an MR context (CHANGE_ID exists) and it's the very first build for this MR job.
    // This is the most reliable method because the first build of a Multibranch Pipeline MR job
    // always corresponds to the MR creation event.
    boolean isNewMr = (env.CHANGE_ID != null) && (env.BUILD_NUMBER == "1")

    // 2. Determine if triggered by a "Specific MR Comment"
    // Note: When triggered by a comment, GITLAB_OBJECT_KIND usually changes to "note".
    boolean isCommentTrigger = isCodeReviewOnly()

    // 3. Fallback logic: Use GITLAB_OA_ACTION for auxiliary validation if it exists
    boolean isExplicitOpen = (env.GITLAB_OA_ACTION == "open")

    return isNewMr || isCommentTrigger || isExplicitOpen
}


/**
 * Returns true when the build was triggered solely by the "code_review" comment,
 * meaning all other stages should be skipped.
 */
def isCodeReviewOnly() {
    return env.GITLAB_OBJECT_KIND == "note" &&
            env.GITLAB_COMMENT_TRIGGER != null &&
            env.GITLAB_COMMENT_TRIGGER.trim() == CODE_REVIEW_CMD
}
