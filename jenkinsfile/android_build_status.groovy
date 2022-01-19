
BUILD_STEP = ""
SDK_BRANCH = "develop"
MEGACHAT_BRANCH = "develop"

/**
 * Decide whether we should skip the current build. If MR title starts with "Draft:"
 * or "WIP:", then CI Server won't start a build. Build will resume until these 2 tags
 * have been removed from MR title.
 * @param title of the Merge Request
 * @return true if current stage should be skipped. Otherwise return false.
 */
def shouldSkip(title) {
    if (title != null && !title.isEmpty()) {
        return title.toLowerCase().startsWith("draft:") ||
                title.toLowerCase().startsWith("wip:")
    }
    // if title is null, this build is probably triggered by 'jenkins rebuild' comment
    // in such case, build should not be skipped.
    return false
}

/**
 * Detect if there is SDK_BRANCH specified in MR Description.
 * If yes, parse and assign the value to SDK_BRANCH, so later we
 * can checkout wanted branch in SDK.
 * If no, assign 'develop' to variable SDK_BRANCH.
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
 * Detect if there is SDK_BRANCH specified in MR Description.
 * If yes, parse and assign the value to MEGACHAT_BRANCH, so later we
 * can checkout wanted branch in SDK.
 * If no, assign 'develop' to variable MEGACHAT_BRANCH.
 */
def getMEGAchatBranch() {
    def description = env.GITLAB_OA_DESCRIPTION
    if (description != null) {
        String[] lines = description.split("\n");
        String KEY = "MEGACHAT_BRANCH=";
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(KEY)) {
                print("MEGACHAT_BRANCH line found!!! --> " + line);
                String value = line.substring(KEY.length());
                if (!value.isEmpty()) {
                    MEGACHAT_BRANCH = value;
                    print("Setting MEGACHAT_BRANCH value --> " + value);
                    return;
                }
            }
        }
    }
    MEGACHAT_BRANCH = 'develop'
}

pipeline {
    agent { label 'label-macmini2||label-macmini3||label-macmini4||mac-slave||mac-slave-m1'}
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

        PATH = "/opt/buildtools/android-sdk/cmake/3.10.2.4988404/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:$PATH"

        CONSOLE_LOG_FILE = "console.txt"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
        // webrtc lib link and file name may change with time. Update these 2 values if build fails.
        WEBRTC_LIB_URL = "https://mega.nz/file/RsMEgZqA#s0P754Ua7AqvWwamCeyrvNcyhmPjHTQQIxtqziSU4HI"
        WEBRTC_LIB_FILE = 'WebRTC_NDKr21_p21_branch-heads4405_v2.zip'
        WEBRTC_LIB_UNZIPPED = 'webrtc_unzipped'

        GOOGLE_MAP_API_URL = "https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k"
        GOOGLE_MAP_API_FILE = 'default_google_maps_api.zip'
        GOOGLE_MAP_API_UNZIPPED = 'default_google_map_api_unzipped'


        // only build one architecture for SDK, to save build time. skipping "x86 armeabi-v7a x86_64"
        BUILD_ARCHS = "arm64-v8a"
    }

    post {
        failure {
            script {
                if (env.BRANCH_NAME.startsWith('MR-')) {
                    def mrNumber = env.BRANCH_NAME.replace('MR-', '')

                    withCredentials([usernameColonPassword(credentialsId: 'Jenkins-Login', variable: 'CREDENTIALS')]) {
                        sh 'curl -u $CREDENTIALS ${BUILD_URL}/consoleText -o console.txt'
                    }

                    withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
                        final String response = sh(script: 'curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@console.txt https://code.developers.mega.co.nz/api/v4/projects/199/uploads', returnStdout: true).trim()
                        def json = new groovy.json.JsonSlurperClassic().parseText(response)
                        env.MARKDOWN_LINK = ":x: Build Failed <br />Build Log: ${json.markdown}"
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
                        slackUploadFile filePath:"console.txt", initialComment:"Android Build Log"
                    }
                }
            }
        }
        success {
            script {
                // If CI build is skipped due to Draft status, send to a comment to MR
                if (env.BRANCH_NAME.startsWith('MR-') && shouldSkip(env.GITLAB_OA_TITLE)) {
                    def mrNumber = env.BRANCH_NAME.replace('MR-', '')
                    withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
                        final String response = sh(script: 'curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@console.txt https://code.developers.mega.co.nz/api/v4/projects/199/uploads', returnStdout: true).trim()
                        env.MARKDOWN_LINK = ":raising_hand: Android Pipeline Build Skipped! <BR/> Build will start to be triggerd after you remove <b>Draft:</b> or <b>WIP:</b> at the beginning of MR title."
                        env.MERGE_REQUEST_URL = "https://code.developers.mega.co.nz/api/v4/projects/199/merge_requests/${mrNumber}/notes"
                        sh 'curl --request POST --header PRIVATE-TOKEN:$TOKEN --form body=\"${MARKDOWN_LINK}\" ${MERGE_REQUEST_URL}'
                    }
                }
            }
        }
    }
    stages {
        stage('Preparation') {
            when {
                expression { (!shouldSkip(env.GITLAB_OA_TITLE)) }
            }
            steps {
                script {
                    BUILD_STEP = "Preparation"

                    getSDKBranch()
                    sh("echo SDK_BRANCH = ${SDK_BRANCH}")

                    getMEGAchatBranch()
                    sh("echo MEGACHAT_BRANCH = ${MEGACHAT_BRANCH}")
                }
                gitlabCommitStatus(name: 'Preparation') {
                    sh("rm -fv ${CONSOLE_LOG_FILE}")
                    sh("set")
                }
            }
        }

        stage('Fetch SDK Submodules') {
            when {
                expression { (!shouldSkip(env.GITLAB_OA_TITLE)) }
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
                expression { (!shouldSkip(env.GITLAB_OA_TITLE)) }
            }
            steps {
                script {
                    BUILD_STEP = "Download Dependency Lib for SDK"
                }
                gitlabCommitStatus(name: 'Download Dependency Lib for SDK') {
                    sh """
                        mkdir -p "${BUILD_LIB_DOWNLOAD_FOLDER}"
                        cd "${BUILD_LIB_DOWNLOAD_FOLDER}"
                        pwd
                        ls -lh
                
                        ## check if webrtc exists
                        if test -f "${BUILD_LIB_DOWNLOAD_FOLDER}/${WEBRTC_LIB_FILE}"; then
                            echo "${WEBRTC_LIB_FILE} already downloaded. Skip downloading."
                        else
                            echo "downloading webrtc"
                            mega-get ${WEBRTC_LIB_URL}
                
                            echo "unzipping webrtc"
                            rm -fr ${WEBRTC_LIB_UNZIPPED}
                            unzip ${WEBRTC_LIB_FILE} -d ${WEBRTC_LIB_UNZIPPED}
                        fi
                
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
                        # apply dependency patches
                        rm -fr app/src/main/jni/megachat/webrtc
                
                        ## ATTENTION: sometimes the downloaded webrtc zip has a enclosing folder. like below.
                        ## so we might need to adjust below path when there is a new webrtc zip
                        cp -fr ${BUILD_LIB_DOWNLOAD_FOLDER}/${WEBRTC_LIB_UNZIPPED}/webrtc_branch-heads4405/webrtc app/src/main/jni/megachat/
                        
                        rm -fr app/src/debug
                        rm -fr app/src/release
                        cp -fr ${BUILD_LIB_DOWNLOAD_FOLDER}/${GOOGLE_MAP_API_UNZIPPED}/* app/src/
                
                    """
                }
            }
        }
        stage('Build SDK') {
            when {
                expression { (!shouldSkip(env.GITLAB_OA_TITLE)) }
            }
            steps {
                script {
                    BUILD_STEP = "Build SDK"
                }
                gitlabCommitStatus(name: 'Build SDK') {
                    sh """
                    cd ${WORKSPACE}/app/src/main/jni
                    echo "=== START SDK BUILD===="
                    bash build.sh all
                    """
                }
            }
        }
        stage('Build APK (GMS+HMS)') {
            when {
                expression { (!shouldSkip(env.GITLAB_OA_TITLE)) }
            }
            steps {
                script {
                    BUILD_STEP = 'Build APK (GMS+HMS)'
                }
                gitlabCommitStatus(name: 'Build APK (GMS+HMS)') {
                    // Finish building and packaging the APK
                    sh "./gradlew clean app:assembleGmsRelease app:assembleHmsRelease"

                    // Archive the APKs so that they can be downloaded from Jenkins
                    // archiveArtifacts '**/*.apk'
                }
            }
        }
        stage('Unit Test') {
            when {
                expression { (!shouldSkip(env.GITLAB_OA_TITLE)) }
            }
            steps {
                script {
                    BUILD_STEP = "Unit Test"
                }
                gitlabCommitStatus(name: 'Unit Test') {
                    // Compile and run the unit tests for the app and its dependencies
                    sh "./gradlew testGmsDebugUnitTest"

                    // Analyse the test results and update the build result as appropriate
                    //junit '**/TEST-*.xml'
                }
            }
        }
        // stage('Static analysis') {
        //   when {
        //      expression { (!shouldSkip(env.GITLAB_OA_TITLE)) }
        //   }
        //   steps {
        //     // Run Lint and analyse the results
        //     sh './gradlew lintDebug'
        //     androidLint pattern: '**/lint-results-*.xml'
        //   }
        // }
        stage('Deploy') {
            when {
                allOf {
                    expression { (!shouldSkip(env.GITLAB_OA_TITLE)) }

                    // Only execute this stage when building from the `beta` branch
                    branch 'beta'
                }
            }
            environment {
                // Assuming a file credential has been added to Jenkins, with the ID 'my-app-signing-keystore',
                // this will export an environment variable during the build, pointing to the absolute path of
                // the stored Android keystore file.  When the build ends, the temporarily file will be removed.
                SIGNING_KEYSTORE = credentials('my-app-signing-keystore')

                // Similarly, the value of this variable will be a password stored by the Credentials Plugin
                SIGNING_KEY_PASSWORD = credentials('my-app-signing-password')
            }
            steps {
                script {
                    BUILD_STEP = "Deploy"
                }
                // Build the app in release mode, and sign the APK using the environment variables
                sh './gradlew assembleRelease'

                // Archive the APKs so that they can be downloaded from Jenkins
                archiveArtifacts '**/*.apk'

                // Upload the APK to Google Play
                androidApkUpload googleCredentialsId: 'Google Play', apkFilesPattern: '**/*-release.apk', trackName: 'beta'
            }
            // post {
            //   success {
            //     // Notify if the upload succeeded
            //     mail to: 'beta-testers@example.com', subject: 'New build available!', body: 'Check it out!'
            //   }
            // }
        }
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