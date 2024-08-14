/**
 * This script is to build and upload Android AAB to Google Play Store
 */

@Library('jenkins-android-shared-lib') _

import groovy.json.JsonSlurperClassic
import mega.privacy.android.pipeline.DefaultParserWrapper
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

/**
 * This script is to build and upload Android AAB to Google Play Store
 */

BUILD_STEP = ''

/**
 * Folder to contain build outputs, including APK, AAB and symbol files
 */
ARCHIVE_FOLDER = "archive"
NATIVE_SYMBOLS_FILE = "symbols.zip"
ARTIFACTORY_BUILD_INFO = "buildinfo.txt"

/**
 * Default release notes content files
 */
RELEASE_NOTES = "default_release_notes.json"
MAJOR_RELEASE_NOTES = "major_release_notes.json"
RELEASE_NOTES_CONTENT = ""

/**
 * common.groovy file with common methods
 */
def common

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }
    options {
        // Stop the build early in case of compile or test failures
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
        timeout(time: 2, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }
    environment {
        LC_ALL = 'en_US.UTF-8'
        LANG = 'en_US.UTF-8'

        NDK_ROOT = '/opt/buildtools/android-sdk/ndk/21.3.6528147'
        JAVA_HOME = '/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx'
        ANDROID_HOME = '/opt/buildtools/android-sdk'

        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        CONSOLE_LOG_FILE = 'console.txt'

        // CD pipeline uses this environment variable to assign version code
        APK_VERSION_CODE_FOR_CD = "${new Date().format('yyDDDHHmm', TimeZone.getTimeZone("GMT"))}"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
    }
    post {
        failure {
            script {
                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)

                if (hasGitLabMergeRequest()) {
                    String jenkinsLog = common.uploadFileToArtifactory(CONSOLE_LOG_FILE)

                    def message = ""
                    if (triggeredByDeliverAppStore()) {
                        message = common.releaseFailureMessage("<br/>") +
                                "<br/>Build Log:\t[$CONSOLE_LOG_FILE](${jenkinsLog})"
                    } else if (triggeredByUploadSymbol()) {
                        message = common.uploadSymbolFailureMessage("<br/>") +
                                "<br/>Build Log:\t[$CONSOLE_LOG_FILE](${jenkinsLog})"
                    } else if (triggeredByCreateJiraVersion()) {
                        message = createJiraVersionFailureMessage() +
                                "<br/>Build Log:\t[$CONSOLE_LOG_FILE](${jenkinsLog})"
                    } else if (triggeredBySendCodeFreezeReminder()) {
                        message = sendCodeFreezeReminderFailureMessage() +
                                "<br/>Build Log:\t[$CONSOLE_LOG_FILE](${jenkinsLog})"
                    } else if (triggeredByPostRelease()) {
                        message = sendPostReleaseFailureMessage() +
                                "<br/>Build Log:\t[$CONSOLE_LOG_FILE](${jenkinsLog})"
                    }
                    common.sendToMR(message)
                } else {
                    slackSend color: 'danger', message: common.releaseFailureMessage("\n")
                    slackUploadFile filePath: CONSOLE_LOG_FILE, initialComment: 'Jenkins Log'
                }
            }
        }
        success {
            script {
                if (triggeredByCreateJiraVersion()) {
                    def message = createJiraVersionSuccessMessage()
                    common.sendToMR(message)
                    slackSend color: "good", message: message
                } else if (!isOnReleaseBranch()) {
                    common.sendToMR(skipMessage("<br/>"))
                } else if (hasGitLabMergeRequest()) {
                    common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                    String link = common.uploadFileToArtifactory(CONSOLE_LOG_FILE)

                    if (triggeredByDeliverAppStore()) {
                        def message = releaseSuccessMessage("<br/>", common) +
                                "<br/>Build Log:\t[$CONSOLE_LOG_FILE](${link})"
                        common.sendToMR(message)

                        // send to android slack channel
                        // the version name can be a <major>.<minor>.<point> (for hotfix) or <major>.<minor> format it to <major>.<minor>
                        // we are using the same channel if the version has hotfix
                        def formattedVersionName = common.readAppVersion()[0].split("\\.")[0..1].join(".")
                        def slackVersionInfo = getSlackBuildVersionInfo(common)
                        def slackInfoFileName = "slack_info.txt"

                        // fetch slack channel id from Artifactory if exists
                        String slackInfoPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/release/v${formattedVersionName}/${slackInfoFileName}"
                        try {
                            common.downloadFromArtifactory(slackInfoPath, slackInfoFileName)
                        } catch (Exception ignored) {
                            println("slack_info.txt not found in Artifactory.")
                        }

                        def slackChannelId = ""
                        def qaSlackChannelId = ""
                        if (fileExists(WORKSPACE + "/" + slackInfoFileName)) {
                            def content = readFile(WORKSPACE + "/" + slackInfoFileName).trim()
                            def slackInfo = content.split(",")
                            if (slackInfo.size() > 0) {
                                slackChannelId = slackInfo[0]
                            }
                            if (slackInfo.size() > 1) {
                                qaSlackChannelId = slackInfo[1]
                            }
                        }

                        if (slackChannelId == "") {
                            def slackResponse = slackSend(channel: "android", message: slackVersionInfo)
                            def qaSlackResponse = slackSend(channel: "qa", message: slackVersionInfo)
                            // write slackResponse.threadId to local file and upload to slackInfoPath
                            slackChannelId = slackResponse.threadId
                            qaSlackChannelId = qaSlackResponse.threadId
                            sh """
                               cd ${WORKSPACE}
                               echo ${slackChannelId},${qaSlackChannelId} > ${slackInfoFileName}
                            """
                            common.uploadToArtifactory(slackInfoFileName, slackInfoPath)
                        } else {
                            slackSend channel: slackChannelId, message: slackVersionInfo, replyBroadcast: true
                            if (qaSlackChannelId != "") {
                                slackSend channel: qaSlackChannelId, message: slackVersionInfo, replyBroadcast: true
                            } else {
                                slackSend channel: "qa", message: slackVersionInfo
                            }
                        }

                        // send to MR
                        common.sendToMR(getBuildVersionInfo(common))

                        slackSend color: "good", message: releaseSuccessMessage("\n", common)
                    } else if (triggeredByUploadSymbol()) {
                        def message = common.uploadSymbolSuccessMessage("<br/>") +
                                "<br/>Build Log:\t[$CONSOLE_LOG_FILE](${link})"
                        common.sendToMR(message)

                        slackSend color: "good", message: common.uploadSymbolSuccessMessage("\n")
                    } else if (triggeredBySendCodeFreezeReminder()) {
                        def message = sendCodeFreezeReminderSuccessMessage()
                        common.sendToMR(message)
                        slackSend color: "good", message: message
                    } else if (triggeredByPostRelease()) {
                        def message = sendPostReleaseSuccessMessage()
                        common.sendToMR(message)
                        slackSend color: "good", message: message
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
                    BUILD_STEP = 'Load Common Script'

                    // load the common library script
                    common = load('jenkinsfile/common.groovy')
                    util.printEnv()
                }
            }
        }
        stage('Clone transifex') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByDeleteOldString() || triggeredByPostRelease() }
            }
            steps {
                script {
                    BUILD_STEP = 'Clone transifex'
                    withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
                        sh """
                            git clone https://code.developers.mega.co.nz/mobile/android/transifex.git
                        """
                    }
                }
            }
        }
        stage('Preparation') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByUploadSymbol() }
            }
            steps {
                script {
                    BUILD_STEP = 'Preparation'

                    // send command acknowledgement to MR
                    common.sendToMR(":runner: Android CD Release pipeline has started!!!" +
                            "<br/><b>Command</b>: ${env.gitlabTriggerPhrase}"
                    )

                    sh("rm -frv $ARCHIVE_FOLDER")
                    sh("mkdir -p ${WORKSPACE}/${ARCHIVE_FOLDER}")
                    sh("rm -fv ${CONSOLE_LOG_FILE}")
                    sh('set')
                }
            }
        }
        stage('Fetch native symbols') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByUploadSymbol() }
            }
            steps {
                script {
                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN')
                    ]) {
                        BUILD_STEP = 'Fetch native symbols'

                        common.downloadAndExtractNativeSymbols()
                    }
                }
            }
        }
        stage('Apply Google Map API Key') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Apply Google Map API Key'
                }

                withCredentials([
                        file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_DEBUG', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_DEBUG'),
                        file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_RELEASE', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_RELEASE')
                ]) {
                    script {
                        println("applying production google map api config... ")
                        sh 'mkdir -p app/src/debug/res/values'
                        sh 'mkdir -p app/src/release/res/values'
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_DEBUG} app/src/debug/res/values/google_maps_api.xml"
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_RELEASE} app/src/release/res/values/google_maps_api.xml"
                    }
                }
            }
        }
        stage('Build GMS APK') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build GMS APK'
                    sh './gradlew clean app:assembleGmsRelease'
                }
            }
        }
        stage('Sign GMS APK') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Sign GMS APK'
                }
                withCredentials([
                        file(credentialsId: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE', variable: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE'),
                        file(credentialsId: 'ANDROID_PRD_GMS_APK_KEYSTORE', variable: 'ANDROID_PRD_GMS_APK_KEYSTORE')
                ]) {
                    script {
                        println("signing GMS APK")
                        String tempAlignedGmsApk = "unsigned_gms_apk_aligned.apk"
                        String gmsApkInput = "${WORKSPACE}/app/build/outputs/apk/gms/release/app-gms-release-unsigned.apk"
                        String gmsApkOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${common.readAppVersion2()}-gms-release.apk"
                        println("input = $gmsApkInput \noutput = $gmsApkOutput")
                        sh """
                            rm -fv ${tempAlignedGmsApk}
                            zipalign -p 4 ${gmsApkInput} ${tempAlignedGmsApk}
                            apksigner sign --ks "${ANDROID_PRD_GMS_APK_KEYSTORE}" --ks-pass file:"${ANDROID_PRD_GMS_APK_PASSWORD_FILE}" --out ${gmsApkOutput} ${tempAlignedGmsApk}
                            ls -lh ${WORKSPACE}/${ARCHIVE_FOLDER}
                            rm -fv ${tempAlignedGmsApk}
                        """
                        println("Finish signing APK. ($gmsApkOutput) generated!")
                    }
                }
            }
        }
        stage('Upload Firebase Crashlytics symbol files') {
            when {
                expression { triggeredByUploadSymbol() }
            }
            steps {
                script {
                    BUILD_STEP = 'Upload Firebase Crashlytics symbol files'
                    sh """
                    cd $WORKSPACE
                    ./gradlew app:uploadCrashlyticsSymbolFileGmsRelease
                    """
                }
            }
        }
        stage('Build GMS AAB') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Build GMS AAB'
                    sh './gradlew clean app:bundleGmsRelease'
                }
            }
        }
        stage('Sign GMS AAB') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Sign GMS AAB'
                }
                withCredentials([
                        string(credentialsId: 'ANDROID_PRD_GMS_AAB_PASSWORD', variable: 'ANDROID_PRD_GMS_AAB_PASSWORD'),
                        file(credentialsId: 'ANDROID_PRD_GMS_AAB_KEYSTORE', variable: 'ANDROID_PRD_GMS_AAB_KEYSTORE')
                ]) {
                    script {
                        println("signing GMS AAB")
                        String gmsAabInput = "${WORKSPACE}/app/build/outputs/bundle/gmsRelease/app-gms-release.aab"
                        String gmsAabOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${common.readAppVersion2()}-gms-release.aab"
                        println("input = $gmsAabInput \noutput = $gmsAabOutput")
                        withEnv([
                                "GMS_AAB_INPUT=${gmsAabInput}",
                                "GMS_AAB_OUTPUT=${gmsAabOutput}"
                        ]) {
                            sh('jarsigner -keystore ${ANDROID_PRD_GMS_AAB_KEYSTORE} -storepass "${ANDROID_PRD_GMS_AAB_PASSWORD}" -signedjar ${GMS_AAB_OUTPUT} ${GMS_AAB_INPUT} megaandroid-upload')
                        }
                        println("Finish signing GMS AAB. ($gmsAabOutput) generated!")
                    }
                }
            }
        }
        stage('Archive files') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Archive files'
                    println("Uploading files to Artifactory repo....")

                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN')
                    ]) {

                        String targetPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/release/${common.artifactoryUploadPath()}/"

                        withEnv([
                                "TARGET_PATH=${targetPath}"
                        ]) {
                            common.createBriefBuildInfoFile()

                            sh '''
                                cd ${WORKSPACE}/archive
                                ls -l ${WORKSPACE}/archive

                                echo Uploading APK files
                                for FILE in *.apk; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done

                                echo Uploading AAB files
                                for FILE in *.aab; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done

                                echo Uploading documentation
                                for FILE in *.txt; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                            '''
                        }

                    }
                }
            }
        }

        stage('Delete old strings') {
            when {
                expression { triggeredByDeliverAppStore() || triggeredByDeleteOldString() }
            }
            steps {
                script {
                    withCredentials([
                            string(credentialsId: 'ANDROID_TRANSIFEX_BOT_TOKEN', variable: 'TRANSIFEX_BOT_TOKEN'),
                            string(credentialsId: 'ANDROID_TRANSIFEX_BOT_URL', variable: 'TRANSIFEX_BOT_URL'),
                            string(credentialsId: 'ANDROID_TRANSIFEX_TOKEN', variable: 'ANDROID_TRANSIFEX_TOKEN'),
                            gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')
                    ]) {
                        withEnv([
                                "TRANSIFEX_BOT_TOKEN=${TRANSIFEX_BOT_TOKEN}",
                                "TRANSIFEX_BOT_URL=${TRANSIFEX_BOT_URL}",
                                "TRANSIFEX_TOKEN=${ANDROID_TRANSIFEX_TOKEN}"
                        ]) {
                            sh 'echo $TRANSIFEX_BOT_TOKEN'
                            sh 'echo $TRANSIFEX_BOT_URL'
                            sh './gradlew deleteOldStrings'
                        }
                    }
                }
            }
        }

        stage('Deploy to Google Play Alpha') {
            when {
                expression { triggeredByDeliverAppStore() }
            }
            steps {
                script {
                    BUILD_STEP = 'Deploy to Google Play Alpha'
                }
                script {
                    withCredentials([
                            string(credentialsId: 'ANDROID_TRANSIFIX_AUTHORIZATION_TOKEN', variable: 'TRANSIFEX_TOKEN'),
                            gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')
                    ]) {
                        if (isMajorRelease(common)) {
                            sh './gradlew readReleaseNotes'
                            RELEASE_NOTES_CONTENT = common.releaseNotes(MAJOR_RELEASE_NOTES)
                            println("Major release notes: ${RELEASE_NOTES_CONTENT}")
                        } else {
                            RELEASE_NOTES_CONTENT = common.releaseNotes(RELEASE_NOTES)
                        }
                    }

                    // Upload the AAB to Google Play
                    androidApkUpload googleCredentialsId: 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL',
                            filesPattern: 'archive/*-gms-release.aab',
                            trackName: 'alpha',
                            rolloutPercentage: '0',
                            additionalVersionCodes: '233140859',
                            nativeDebugSymbolFilesPattern: "archive/${NATIVE_SYMBOLS_FILE}",
                            recentChangeList: common.getRecentChangeList(RELEASE_NOTES_CONTENT),
                            releaseName: common.readAppVersion1()
                }
            }
        }
        stage("Create Jira Version") {
            when {
                expression { triggeredByCreateJiraVersion() }
            }
            steps {
                script {
                    BUILD_STEP = 'Create Jira Version'

                    def parameters = parseCreateJiraVersionParameters(env.gitlabTriggerPhrase)
                    def releaseVersion = parameters[0]
                    def releaseDate = parameters[1]
                    withCredentials([
                            string(credentialsId: 'JIRA_TOKEN', variable: 'JIRA_TOKEN'),
                            string(credentialsId: 'JIRA_API_URL', variable: 'JIRA_API_URL'),
                            string(credentialsId: 'JIRA_PROJECT_NAME_AND_ID_TABLE', variable: 'JIRA_PROJECTS'),
                    ]) {
                        sh("./gradlew createJiraVersion --rv ${releaseVersion} --rd ${releaseDate}")
                    }
                }
            }
        }
        stage("Send Code Freeze Reminder") {
            when {
                expression { triggeredBySendCodeFreezeReminder() }
            }
            steps {
                script {
                    BUILD_STEP = 'Send Code Freeze Reminder'

                    def parameters = parseSendCodeFreezeReminderParameters(env.gitlabTriggerPhrase)
                    def currentVersion = parameters[0]
                    def nextVersion = parameters[1]
                    withCredentials([
                            string(credentialsId: 'ANDROID_TEAM_GROUP_ID_IN_SLACK', variable: 'ANDROID_TEAM_GROUP_ID_IN_SLACK'),
                            string(credentialsId: 'MOBILE_DEV_TEAM_SLACK_CHANNEL_ID', variable: 'MOBILE_DEV_TEAM_SLACK_CHANNEL_ID'),
                            string(credentialsId: 'RELEASE_ANNOUNCEMENT_SLACK_TOKEN', variable: 'RELEASE_ANNOUNCEMENT_SLACK_TOKEN'),
                    ]) {
                        sh("./gradlew sendCodeFreezeReminder --current-version ${currentVersion} --next-version ${nextVersion} --project MEGA")
                    }
                }
            }
        }
        stage("Post Release") {
            when {
                expression { triggeredByPostRelease() }
            }
            steps {
                script {
                    BUILD_STEP = 'Post Release'

                    def parameters = parsePostReleaseParameters(env.gitlabTriggerPhrase)
                    def releaseVersion = parameters[0]

                    withCredentials([
                            string(credentialsId: 'ANDROID_TRANSIFIX_AUTHORIZATION_TOKEN', variable: 'TRANSIFEX_TOKEN'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN'),
                            string(credentialsId: 'GITLAB_API_BASE_URL', variable: 'GITLAB_API_BASE_URL'),
                            string(credentialsId: 'GITLAB_PERSONAL_ACCESS_TOKEN_TEXT', variable: 'GITLAB_PERSONAL_ACCESS_TOKEN_TEXT'),
                            string(credentialsId: 'JIRA_API_URL', variable: 'JIRA_API_URL'),
                            string(credentialsId: 'JIRA_BASE_URL', variable: 'JIRA_BASE_URL'),
                            string(credentialsId: 'JIRA_TOKEN', variable: 'JIRA_TOKEN'),
                            gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default'),
                            // USERNAME and TOKEN are expected keywords to enable GPG Signing. Using
                            // other keywords would fail GPG Signing
                            usernamePassword(credentialsId: 'GitHub-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')
                    ]) {
                        util.useGpg() {
                            sh("./gradlew postRelease --rv ${releaseVersion}")
                            sh("./gradlew setReleaseStatus --rv ${releaseVersion}")
                            sh("./gradlew createGitlabRelease --rv ${releaseVersion}")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Check if this build is triggered by a GitLab Merge Request.
 * @return true if this build is triggered by a GitLab MR. False if this build is triggered
 * by a plain git push.
 */
private boolean hasGitLabMergeRequest() {
    return env.gitlabMergeRequestIid != null && !env.gitlabMergeRequestIid.isEmpty()
}

/**
 * compose the success message of "deliver_appStore" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @param common The common functions loaded from common.groovy
 * @return The success message to be sent
 */
private String releaseSuccessMessage(String lineBreak, Object common) {
    return ":rocket: Android Release uploaded to Google Play Alpha channel successfully!" +
            "${lineBreak}Version:\t${common.readAppVersion1()}" +
            "${lineBreak}Last Commit Msg:\t${common.lastCommitMessage()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

/**
 * Generate a message with all key release information. This message can be posted to MR and then
 * directly published by Release Process.
 * @param common The common functions loaded from common.groovy
 * @return the composed message
 */
private String getBuildVersionInfo(Object common) {
    println("entering getBuildVersionInfo")
    String artifactoryUrl = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/release/${common.artifactoryUploadPath()}"
    String artifactVersion = common.readAppVersion2()

    String gmsAabUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.aab"
    String gmsApkUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.apk"

    String sdkVersion = common.readPrebuiltSdkVersion()
    String appCommitLink = "${env.GITLAB_BASE_URL}/mobile/android/android/-/commit/" + common.appCommitId()
    String sdkCommitLink = "${env.GITLAB_BASE_URL}/sdk/sdk/-/commit/" + common.queryPrebuiltSdkProperty("sdk-commit", sdkVersion)
    String chatCommitLink = "${env.GITLAB_BASE_URL}/megachat/MEGAchat/-/commit/" + common.queryPrebuiltSdkProperty("chat-commit", sdkVersion)

    String appBranch = env.gitlabSourceBranch

    def message = """
    Version: ${common.readAppVersion1()} <br/>
    App Bundles and APKs: <br/>
       - Google (GMS):  [AAB](${gmsAabUrl}) | [APK](${gmsApkUrl}) <br/>
    Build info: <br/>
       - [Android commit](${appCommitLink}) (`${appBranch}`) <br/>
       - [SDK commit](${sdkCommitLink}) (`${common.queryPrebuiltSdkProperty("sdk-branch", sdkVersion)}`) <br/>
       - [Karere commit](${chatCommitLink}) (`${common.queryPrebuiltSdkProperty("chat-branch", sdkVersion)}`) <br/>
    """
    return message
}

private String getSlackBuildVersionInfo(Object common) {
    println("entering getSlackBuildVersionInfo")
    String artifactoryUrl = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/release/${common.artifactoryUploadPath()}"
    String artifactVersion = common.readAppVersion2()

    String gmsAabUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.aab"
    String gmsApkUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.apk"

    String sdkVersion = common.readPrebuiltSdkVersion()
    String appCommitLink = "${env.GITLAB_BASE_URL}/mobile/android/android/-/commit/" + common.appCommitId()
    String sdkCommitLink = "${env.GITLAB_BASE_URL}/sdk/sdk/-/commit/" + common.queryPrebuiltSdkProperty("sdk-commit", sdkVersion)
    String chatCommitLink = "${env.GITLAB_BASE_URL}/megachat/MEGAchat/-/commit/" + common.queryPrebuiltSdkProperty("chat-commit", sdkVersion)

    String appBranch = env.gitlabSourceBranch
    def (versionName, versionNameChannel, versionCode, appGitHash) = common.readAppVersion()
    def packageLink = ""
    withCredentials([
            string(credentialsId: 'JIRA_BASE_URL', variable: 'JIRA_BASE_URL'),
    ]) {
        packageLink = "${env.JIRA_BASE_URL}/issues/?jql=fixVersion%20%3D%20%22Android%20${versionName}%22%20%20ORDER%20BY%20priority%20DESC%2C%20updated%20DESC"
    }
    def appVersionNameAndVersionCode = versionName + versionNameChannel + "(" + versionCode + ")"
    String releaseNote = new JsonSlurperClassic().parseText(RELEASE_NOTES_CONTENT)['en-US']
    def message = """
    The Android team has uploaded a new ALPHA version `${appVersionNameAndVersionCode}` for QA testing.\n
    *App Bundles and APKs:*\n
    • Google (GMS):  <${gmsAabUrl}|AAB> | <${gmsApkUrl}|APK>\n
    *Build info:*\n
    • <${appCommitLink}|Android commit> (`${appBranch}`)\n
    • <${sdkCommitLink}|SDK commit> (`${common.queryPrebuiltSdkProperty("sdk-branch", sdkVersion)}`)\n
    • <${chatCommitLink}|Karere commit> (`${common.queryPrebuiltSdkProperty("chat-branch", sdkVersion)}`)\n
    *Full JIRA cross-project release package:* <${packageLink}|v${versionName + versionNameChannel}> (AND + AP + BAC + CC + CU + MEET + TRAN + SHR + FM + SAT + SAO).
    *Release notes:*\n
    ```${releaseNote}```
    """.stripIndent()
    return message
}

private String createJiraVersionFailureMessage() {
    return ":x: Create Jira Version failed!(${env.BUILD_NUMBER})"
}

private String createJiraVersionSuccessMessage() {
    return ":white_check_mark: Create Jira Version succeeded!(${env.BUILD_NUMBER})"
}

private String sendCodeFreezeReminderFailureMessage() {
    return ":x: Send code freeze reminder failed!(${env.BUILD_NUMBER})"
}

private String sendCodeFreezeReminderSuccessMessage() {
    return ":white_check_mark: Code freeze remind message sent successfully!(${env.BUILD_NUMBER})"
}

private String sendPostReleaseSuccessMessage() {
    return ":white_check_mark: Post Release successful!(${env.BUILD_NUMBER})"
}

private String sendPostReleaseFailureMessage() {
    return ":x: Post Release failed!(${env.BUILD_NUMBER})"
}

private def parsePostReleaseParameters(String fullCommand) {
    println("Parsing postRelease parameters")
    String[] parameters = fullCommand.split("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*\$)")

    Options options = new Options()
    Option releaseVersionOption = Option
            .builder("rv")
            .longOpt("release-version")
            .argName("Release Version")
            .hasArg()
            .desc("Release Version created in Jira")
            .build()
    options.addOption(releaseVersionOption)

    CommandLineParser commandLineParser = new DefaultParserWrapper()
    CommandLine commandLine = commandLineParser.parse(options, parameters)

    String releaseVersion = commandLine.getOptionValue("rv")

    println("releaseVersion: $releaseVersion")

    return [releaseVersion]
}

private def parseCreateJiraVersionParameters(String fullCommand) {
    println("Parsing createJiraVersion parameters")
    String[] parameters = fullCommand.split("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*\$)")

    Options options = new Options()
    Option releaseVersionOption = Option
            .builder("rv")
            .longOpt("release-version")
            .argName("Release Version")
            .hasArg()
            .desc("Release version to be created in Jira")
            .build()
    Option releaseDateOption = Option
            .builder("rd")
            .longOpt("release-date")
            .argName("Release Date")
            .hasArg()
            .desc("Expected date of release which is to create in Jira")
            .build()
    options.addOption(releaseVersionOption)
    options.addOption(releaseDateOption)

    CommandLineParser commandLineParser = new DefaultParserWrapper()
    CommandLine commandLine = commandLineParser.parse(options, parameters)

    String releaseVersion = commandLine.getOptionValue("rv")
    String releaseDate = commandLine.getOptionValue("rd")

    println("releaseVersion: $releaseVersion")
    println("releaseDate: $releaseDate")

    return [releaseVersion, releaseDate]
}

private def parseSendCodeFreezeReminderParameters(String fullCommand) {
    println("Parsing createJiraVersion parameters")
    String[] parameters = fullCommand.split("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*\$)")

    Options options = new Options()
    Option releaseVersionOption = Option
            .builder("cv")
            .longOpt("current-version")
            .argName("Current version")
            .hasArg()
            .desc("Version of current release")
            .build()
    Option releaseDateOption = Option
            .builder("nv")
            .longOpt("next-version")
            .argName("Next version")
            .hasArg()
            .desc("Version of next release")
            .build()
    options.addOption(releaseVersionOption)
    options.addOption(releaseDateOption)

    CommandLineParser commandLineParser = new DefaultParserWrapper()
    CommandLine commandLine = commandLineParser.parse(options, parameters)

    String currentVersion = commandLine.getOptionValue("cv")
    String nextVersion = commandLine.getOptionValue("nv")

    println("current-version: $currentVersion")
    println("releaseDate: $nextVersion")

    return [currentVersion, nextVersion]
}

private String skipMessage(String lineBreak) {
    return ":raising_hand: Android Release Upload skipped!" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Build can only be triggered on release branch by MR command."
}

/**
 * Check if the branch is a release branch
 * @return true if triggered branch is 'release/*', otherwise return false.
 */
private boolean isOnReleaseBranch() {
    return env.gitlabSourceBranch != null && env.gitlabSourceBranch.startsWith("release/")
}

private boolean isMajorRelease(Object common) {
    def versionName = common.readAppVersion()[0]
    def minor = versionName.split("\\.")[1]
    return minor == "0"
}

/**
 * Check if build is triggered by 'deliver_appStore' command.
 * @return true if build is triggered by 'deliver_appStore' command. Otherwise return false.
 */
private boolean triggeredByDeliverAppStore() {
    return isOnReleaseBranch() &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase == "deliver_appStore"
}

/**
 * Check if build is triggered by 'upload_symbol' command.
 * @return true if build is triggered by 'upload_symbol' command. Otherwise return false.
 */
private boolean triggeredByUploadSymbol() {
    return isOnReleaseBranch() &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase == "upload_symbol"
}

/**
 * Check if build is triggered by 'delete_oldStrings' command.
 * @return true if build is triggered by 'delete_oldStrings' command. Otherwise return false.
 */
private boolean triggeredByDeleteOldString() {
    return isOnReleaseBranch() &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase == "delete_oldStrings"
}

/**
 * Check if build is triggered by 'create_jira_version' command.
 * @return true if build is triggered by 'create_jira_version' command. Otherwise return false.
 */
private boolean triggeredByCreateJiraVersion() {
    return env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase.trim().startsWith("create_jira_version")
}

/**
 * Check if build is triggered by 'send_code_freeze_reminder' command.
 * @return true if build is triggered by 'send_code_freeze_reminder' command. Otherwise return false.
 */
private boolean triggeredBySendCodeFreezeReminder() {
    return isOnReleaseBranch() &&
            env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase.trim().startsWith("send_code_freeze_reminder")
}

/**
 * Check if build is triggered by 'postRelease' command.
 * @return true if build is triggered by 'postRelease' command. Otherwise return false.
 */
private boolean triggeredByPostRelease() {
    return env.gitlabTriggerPhrase != null &&
            env.gitlabTriggerPhrase.trim().startsWith("postRelease")
}