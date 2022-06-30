BUILD_STEP = ""
SDK_BRANCH = "develop"
MEGACHAT_BRANCH = "develop"

GMS_APK_BUILD_LOG = "gms_build.log"
HMS_APK_BUILD_LOG = "hms_build.log"
QA_APK_BUILD_LOG = "qa_build.log"

UNIT_TEST_SUMMARY = ""
UNIT_TEST_REPORT_ARCHIVE = "unit_test_result_${BUILD_NUMBER}.zip"

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
 * Detect if there is SDK_BRANCH specified in MR Description.
 * If yes, parse and assign the value to SDK_BRANCH, so later we
 * can checkout wanted branch for SDK.
 * If no, set SDK_BRANCH to "develop".
 */
def getSDKBranch() {
    def description = env.GITLAB_OA_DESCRIPTION
    if (description != null) {
        String[] lines = description.split("\n");
        String KEY = "SDK_BRANCH=";
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(KEY)) {
                print("SDK_BRANCH line found!!! --> " + line);
                String value = line.substring(KEY.length());
                if (!value.isEmpty()) {
                    print("Setting SDK_BRANCH value --> " + value);
                    SDK_BRANCH = value;
                    return;
                }
            }
        }
    }
    SDK_BRANCH = 'develop'
}

/**
 * Detect if there is MEGACHAT_BRANCH specified in MR Description.
 * If yes, parse and assign the value to MEGACHAT_BRANCH, so later we
 * can checkout wanted branch for MEGAChat SDK.
 * If no, set MEGACHAT_BRANCH to "develop".
 */
def getMEGAChatBranch() {
    def description = env.GITLAB_OA_DESCRIPTION
    if (description != null) {
        String[] lines = description.split("\n");
        String KEY = "MEGACHAT_BRANCH=";
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(KEY)) {
                print("MEGACHAT_BRANCH line found!!! --> " + line)
                String value = line.substring(KEY.length());
                if (!value.isEmpty()) {
                    MEGACHAT_BRANCH = value;
                    print("Setting MEGACHAT_BRANCH value --> " + value)
                    return;
                }
            }
        }
    }
    MEGACHAT_BRANCH = 'develop'
}

/**
 * Fetch message of last commit from environment variable.
 * @return the commit message text if GitLab plugin has sent a valid commit message,
 *         otherwise return "N/A" normally when CI build is triggered by MR comment "jenkins rebuild".
 */
def getLastCommitMessage() {
    def lastCommitMessage = env.GITLAB_OA_LAST_COMMIT_MESSAGE
    if (lastCommitMessage == null) {
        lastCommitMessage = "N/A"
    }
    return lastCommitMessage
}

pipeline {
    agent { label 'mac-jenkins-slave' }
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
        PATH = "/opt/buildtools/android-sdk/cmake/3.10.2.4988404/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:$PATH"

        // Jenkins build log will be saved in this file.
        CONSOLE_LOG_FILE = "console.txt"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'

        // Google map api
        GOOGLE_MAP_API_URL = "https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k"
        GOOGLE_MAP_API_FILE = 'default_google_maps_api.zip'
        GOOGLE_MAP_API_UNZIPPED = 'default_google_map_api_unzipped'

        // only build one architecture for SDK, to save build time. skipping "x86 armeabi-v7a x86_64"
        BUILD_ARCHS = "arm64-v8a"

        // SDK build log. ${LOG_FILE} will be used by build.sh to export SDK build log.
        SDK_LOG_FILE_NAME = "sdk_build_log.txt"
        LOG_FILE = "${WORKSPACE}/${SDK_LOG_FILE_NAME}"
    }
    post {
        failure {
            script {
                if (env.BRANCH_NAME.startsWith('MR-')) {
                    def mrNumber = env.BRANCH_NAME.replace('MR-', '')

                    // download Jenkins console log
                    withCredentials([usernameColonPassword(credentialsId: 'Jenkins-Login', variable: 'CREDENTIALS')]) {
                        sh 'curl -u $CREDENTIALS ${BUILD_URL}/consoleText -o console.txt'
                    }

                    withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
                        // upload Jenkins console log
                        final String respJenkinsLog = sh(script: 'curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@console.txt https://code.developers.mega.co.nz/api/v4/projects/199/uploads', returnStdout: true).trim()
                        def jsonJenkinsLog = new groovy.json.JsonSlurperClassic().parseText(respJenkinsLog)

                        // upload unit test report if unit test fails
                        String unitTestResult = ""
                        if (BUILD_STEP == "Unit Test") {
                            if (archiveUnitTestReport()) {
                                final String unitTestUploadResponse = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${UNIT_TEST_REPORT_ARCHIVE} https://code.developers.mega.co.nz/api/v4/projects/199/uploads", returnStdout: true).trim()
                                def unitTestFileLink = new groovy.json.JsonSlurperClassic().parseText(unitTestUploadResponse).markdown

                                String unitTestSummary = unitTestSummary("${WORKSPACE}/app/build/test-results/testGmsDebugUnitTest")
                                unitTestResult = "<br/>${unitTestSummary} <br/>Unit Test Report:${unitTestFileLink}"
                            } else {
                                unitTestResult = "<br>Unit Test report not available, perhaps test code has compilation error. Please check full build log."
                            }
                        }

                        // upload SDK build log if SDK build fails
                        String sdkBuildMessage = ""
                        if (BUILD_STEP == "Build SDK") {
                            final String respSdkLog = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${SDK_LOG_FILE_NAME} https://code.developers.mega.co.nz/api/v4/projects/199/uploads", returnStdout: true).trim()
                            def jsonSdkLog = new groovy.json.JsonSlurperClassic().parseText(respSdkLog)
                            sdkBuildMessage = "<br/>SDK Build failed. Log:${jsonSdkLog.markdown}"
                        }

                        env.MARKDOWN_LINK = ":x: Build Failed" +
                                "<br/>Failure Stage: ${BUILD_STEP}" +
                                "<br/>Last Commit Message: <b>${getLastCommitMessage()}</b>" +
                                "<br/>Last Commit ID: ${env.GIT_COMMIT}" +
                                "<br/>Build Log: ${jsonJenkinsLog.markdown}" +
                                sdkBuildMessage +
                                unitTestResult

                        env.MERGE_REQUEST_URL = "https://code.developers.mega.co.nz/api/v4/projects/199/merge_requests/${mrNumber}/notes"
                        sh 'curl --request POST --header PRIVATE-TOKEN:$TOKEN --form body=\"${MARKDOWN_LINK}\" ${MERGE_REQUEST_URL}'
                    }
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
                if (env.BRANCH_NAME.startsWith('MR-')) {
                    def mrNumber = env.BRANCH_NAME.replace('MR-', '')

                    // If CI build is skipped due to Draft status, send a comment to MR
                    if (shouldSkipBuild()) {
                        withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
                            env.MARKDOWN_LINK = ":raising_hand: Android CI Pipeline Build Skipped! <BR/> Newly triggered builds will resume after you have removed <b>Draft:</b> or <b>WIP:</b> from the beginning of MR title."
                            env.MERGE_REQUEST_URL = "https://code.developers.mega.co.nz/api/v4/projects/199/merge_requests/${mrNumber}/notes"
                            sh 'curl --request POST --header PRIVATE-TOKEN:$TOKEN --form body=\"${MARKDOWN_LINK}\" ${MERGE_REQUEST_URL}'
                        }
                    } else {
                        // always report build success to MR comment
                        withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
                            env.MARKDOWN_LINK = ":white_check_mark: Build Succeeded!" +
                                    "<br/>Last Commit: <b>${getLastCommitMessage()}</b> (${env.GIT_COMMIT})" +
                                    "<br/>Build Warnings: ${readBuildWarnings()}" +
                                    "<br/>${UNIT_TEST_SUMMARY}"

                            env.MERGE_REQUEST_URL = "https://code.developers.mega.co.nz/api/v4/projects/199/merge_requests/${mrNumber}/notes"
                            sh 'curl --request POST --header PRIVATE-TOKEN:$TOKEN --form body=\"${MARKDOWN_LINK}\" ${MERGE_REQUEST_URL}'
                        }
                    }
                }
            }
        }
    }
    stages {
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
                        getSDKBranch()
                        sh("echo SDK_BRANCH = ${SDK_BRANCH}")

                        getMEGAChatBranch()
                        sh("echo MEGACHAT_BRANCH = ${MEGACHAT_BRANCH}")

                        sh("rm -fv ${CONSOLE_LOG_FILE}")
                        sh("set")
                        sh("rm -fv unit_test_result*.zip")
                    }
                }
            }
        }

        stage('Fetch SDK Submodules') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                script {
                    BUILD_STEP = "Fetch SDK Submodules"
                }

                gitlabCommitStatus(name: 'Fetch SDK Submodules') {
                    withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
                        sh 'git config --file=.gitmodules submodule."app/src/main/jni/mega/sdk".url https://code.developers.mega.co.nz/sdk/sdk.git'
                        sh "git config --file=.gitmodules submodule.\"app/src/main/jni/mega/sdk\".branch ${SDK_BRANCH}"
                        sh 'git config --file=.gitmodules submodule."app/src/main/jni/megachat/sdk".url https://code.developers.mega.co.nz/megachat/MEGAchat.git'
                        sh "git config --file=.gitmodules submodule.\"app/src/main/jni/megachat/sdk\".branch ${MEGACHAT_BRANCH}"
                        sh "git submodule sync"
                        sh "git submodule update --init --recursive --remote"
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
        stage('Build SDK') {
            when {
                expression { (!shouldSkipBuild()) }
            }
            steps {
                script {
                    BUILD_STEP = "Build SDK"
                }
                gitlabCommitStatus(name: 'Build SDK') {
                    sh """
                    rm -f ${LOG_FILE}
                    cd ${WORKSPACE}/app/src/main/jni
                    echo "=== START SDK BUILD===="
                    bash build.sh all
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
                    // Compile and run the unit tests for the app and its dependencies
                    sh "./gradlew testGmsDebugUnitTest"

                    script {
                        // below code is only run when UnitTest is OK, before test reports are cleaned up.
                        // If UnitTest is failed, summary is collected at post.failure{} phase
                        UNIT_TEST_SUMMARY = unitTestSummary("${WORKSPACE}/app/build/test-results/testGmsDebugUnitTest")
                    }

                }
            }
        }
        // stage('Static analysis') {
        //   when {
        //      expression { (!shouldSkip()) }
        //   }
        //   steps {
        //     // Run Lint and analyse the results
        //     sh './gradlew lintDebug'
        //     androidLint pattern: '**/lint-results-*.xml'
        //   }
        // }

        stage('Clean up') {
            steps {
                script {
                    BUILD_STEP = "Clean Up"
                }
                gitlabCommitStatus(name: 'Clean Up') {
                    sh """
                    cd ${WORKSPACE}
                    echo "workspace size before clean: "
                    du -sh
                    cd ${WORKSPACE}/app/src/main/jni
                    bash build.sh clean
                    cd ${WORKSPACE}
                    ./gradlew clean
                    echo "workspace size after clean: "
                    du -sh
                    """
                }
            }
        }
    }
    // post {
    //   failure {
    //     // Notify developer team of the failure
    //     mail to: 'android-devs@example.com', subject: 'Oops!', body: "Build ${env.BUILD_NUMBER} failed; ${env.BUILD_URL}"
    //   }
    // }
}

String readBuildWarnings() {
    String result = ""
    if (fileExists(GMS_APK_BUILD_LOG)) {
        String gmsBuildWarnings = sh(script: "cat ${GMS_APK_BUILD_LOG} | grep -a '^w:' || true", returnStdout: true).trim()
        println("gmsBuildWarnings = $gmsBuildWarnings")
        if (!gmsBuildWarnings.isEmpty()) {
            result = "<br/><b>:warning: GMS Build Warnings :warning:</b><br/>" + wrapBuildWarnings(gmsBuildWarnings)
        }
    }

    if (fileExists(HMS_APK_BUILD_LOG)) {
        String hmsBuildWarnings = sh(script: "cat ${HMS_APK_BUILD_LOG} | grep -a '^w:' || true", returnStdout: true).trim()
        println("hmsBuildWarnings = $hmsBuildWarnings")
        if (!hmsBuildWarnings.isEmpty()) {
            result += "<br/><b>:warning: HMS Build Warnings :warning:</b><br/>" + wrapBuildWarnings(hmsBuildWarnings)
        }
    }

    if (fileExists(QA_APK_BUILD_LOG)) {
        String qaBuildWarnings = sh(script: "cat ${QA_APK_BUILD_LOG} | grep -a '^w:' || true", returnStdout: true).trim()
        println("qaGmsBuildWarnings = $qaBuildWarnings")
        if (!qaBuildWarnings.isEmpty()) {
            result += "<br/><b>:warning: QA GMS Build Warnings :warning:</b><br/>" + wrapBuildWarnings(qaBuildWarnings)
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
 * @return summary string of unit test
 */
String unitTestSummary(String testReportRoot) {
    return sh(script: "python3 ${WORKSPACE}/jenkinsfile/junit_report.py ${testReportRoot}", returnStdout: true).trim()
}

def archiveUnitTestReport() {
    sh("rm -f ${WORKSPACE}/${UNIT_TEST_REPORT_ARCHIVE}")
    if (fileExists(WORKSPACE + "/app/build/reports")) {
        sh """
            cd app/build
            zip -r ${UNIT_TEST_REPORT_ARCHIVE} reports/* 
            mv ${UNIT_TEST_REPORT_ARCHIVE} ${WORKSPACE}
        """
        return true
    } else {
        return false
    }
}
