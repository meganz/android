
pipeline {
  agent { label 'mac-slave' }
  environment {

    LC_ALL = "en_US.UTF-8"
    LANG = "en_US.UTF-8"

    NDK_ROOT = "/opt/buildtools/android-sdk/ndk/21.3.6528147"
    JAVA_HOME = "/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx_x64"
    ANDROID_HOME = "/opt/buildtools/android-sdk"

    PATH = "/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx_x64/bin:/opt/homebrew/bin:/opt/homebrew/opt/gnu-sed/libexec/gnubin:/opt/homebrew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:$PATH"

    CONSOLE_LOG_FILE = "androidLog.txt"

    BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
    // webrtc lib link and file name may change with time. Update these 2 values if build failed.
    WEBRTC_LIB_URL = "https://mega.nz/file/RsMEgZqA#s0P754Ua7AqvWwamCeyrvNcyhmPjHTQQIxtqziSU4HI"
    WEBRTC_LIB_FILE = 'WebRTC_NDKr21_p21_branch-heads4405_v2.zip'
    WEBRTC_LIB_UNZIPPED = 'webrtc_unzipped'

    GOOGLE_MAP_API_URL = "https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k"
    GOOGLE_MAP_API_FILE = 'default_google_maps_api.zip'
    GOOGLE_MAP_API_UNZIPPED = 'default_google_map_api_unzipped'
  }
  options {
    // Stop the build early in case of compile or test failures
    skipStagesAfterUnstable()
    timeout(time: 2, unit: 'HOURS')
    gitLabConnection('GitLabConnection')
  }
  post {
    failure {
      script {
        def comment = "Android Build Failed: ${env.GIT_BRANCH}"
        if (env.CHANGE_URL) {
          comment = "Android Build Failed: ${env.GIT_BRANCH} ${env.CHANGE_URL}"
        }
        slackUploadFile filePath: env.CONSOLE_LOG_FILE, initialComment: comment
      }
    }
  }
  stages {

    stage('Notify GitLab Test') {
      steps {
        echo 'Notify GitLab'
        updateGitlabCommitStatus name: 'build', state: 'pending'
        updateGitlabCommitStatus name: 'build', state: 'failed'
        updateGitlabCommitStatus name: 'test', state: 'pending'
        updateGitlabCommitStatus name: 'test', state: 'failed'
        addGitLabMRComment comment: 'Another build has been triggered in CI'
        slackSend color: "warning", message: "Message from Jenkins Pipeline - Android"
      }
    }

    stage('Fetch SDK Submodules') {
      steps {
        withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
          sh "git config --file=.gitmodules submodule.\"app/src/main/jni/mega/sdk\".url https://code.developers.mega.co.nz/sdk/sdk.git > ${CONSOLE_LOG_FILE}"
          sh "git config --file=.gitmodules submodule.\"app/src/main/jni/mega/sdk\".branch develop >> ${CONSOLE_LOG_FILE}"
          sh "git config --file=.gitmodules submodule.\"app/src/main/jni/megachat/sdk\".url https://code.developers.mega.co.nz/megachat/MEGAchat.git >> ${CONSOLE_LOG_FILE}"
          sh "git config --file=.gitmodules submodule.\"app/src/main/jni/megachat/sdk\".branch develop >> ${CONSOLE_LOG_FILE}"
          sh "git submodule sync >> ${CONSOLE_LOG_FILE}"
          sh "git submodule update --init --recursive --remote >> ${CONSOLE_LOG_FILE}"
        }
      }
    }

    stage('Download Dependency Lib for SDK') {
      steps {
        sh """

        mkdir -p "${BUILD_LIB_DOWNLOAD_FOLDER}"
        cd "${BUILD_LIB_DOWNLOAD_FOLDER}"
        pwd
        ls -lh

        ## check webrtc file
        if test -f "${BUILD_LIB_DOWNLOAD_FOLDER}/${WEBRTC_LIB_FILE}"; then
          echo "${WEBRTC_LIB_FILE} already downloaded. Skip downloading."
        else
          echo "downloading webrtc"
          mega-get ${WEBRTC_LIB_URL} >> ${CONSOLE_LOG_FILE}

          echo "unzipping webrtc"
          rm -fr ${WEBRTC_LIB_UNZIPPED}
          unzip ${WEBRTC_LIB_FILE} -d ${WEBRTC_LIB_UNZIPPED}
        fi

        ## check default google api
        if test -f "${BUILD_LIB_DOWNLOAD_FOLDER}/${GOOGLE_MAP_API_FILE}"; then
          echo "${GOOGLE_MAP_API_FILE} already downloaded. Skip downloading."
        else
          echo "downloading google map api"
          mega-get ${GOOGLE_MAP_API_URL} >> ${CONSOLE_LOG_FILE}

          echo "unzipping google map api"
          rm -fr ${GOOGLE_MAP_API_UNZIPPED}
          unzip ${GOOGLE_MAP_API_FILE} -d ${GOOGLE_MAP_API_UNZIPPED}
        fi

        ls -lh >> ${CONSOLE_LOG_FILE}

        cd ${WORKSPACE}
        pwd
        # apply dependency patch
        rm -fr app/src/main/jni/megachat/webrtc

        ## ATTENTION: sometimes the downloaded webrtc zip has a enclosing folder. like below.
        ## so we might need to adjust below path when there is a new webrtc zip
        cp -fr ${BUILD_LIB_DOWNLOAD_FOLDER}/${WEBRTC_LIB_UNZIPPED}/webrtc_branch-heads4405/webrtc app/src/main/jni/megachat/
        # mkdir -p app/src
        rm -fr app/src/debug
        rm -fr app/src/release
        cp -fr ${BUILD_LIB_DOWNLOAD_FOLDER}/${GOOGLE_MAP_API_UNZIPPED}/* app/src/

        """
      }
    }
    stage('Build SDK') {
      steps {
        sh """
        cd ${WORKSPACE}/app/src/main/jni
        /opt/homebrew/bin/bash build.sh all >> ${env.CONSOLE_LOG_FILE}
        """
      }
    }
    stage('Compile') {
      steps {
        // Compile the app and its dependencies
        sh './gradlew compileDebugSources >> ${env.CONSOLE_LOG_FILE}'
      }
    }
    stage('Unit test') {
      steps {
        // Compile and run the unit tests for the app and its dependencies
        sh './gradlew testDebugUnitTest >> ${env.CONSOLE_LOG_FILE}'

        // Analyse the test results and update the build result as appropriate
        //junit '**/TEST-*.xml'
      }
    }
    stage('Build APK') {
      steps {
        // Finish building and packaging the APK
        sh './gradlew assemble >> ${env.CONSOLE_LOG_FILE}'

        // Archive the APKs so that they can be downloaded from Jenkins
        // archiveArtifacts '**/*.apk'
      }
    }
    // stage('Static analysis') {
    //   steps {
    //     // Run Lint and analyse the results
    //     sh './gradlew lintDebug'
    //     androidLint pattern: '**/lint-results-*.xml'
    //   }
    // }
    stage('Deploy') {
      when {
        // Only execute this stage when building from the `beta` branch
        branch 'beta'
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
  }
  // post {
  //   failure {
  //     // Notify developer team of the failure
  //     mail to: 'android-devs@example.com', subject: 'Oops!', body: "Build ${env.BUILD_NUMBER} failed; ${env.BUILD_URL}"
  //   }
  // }
}