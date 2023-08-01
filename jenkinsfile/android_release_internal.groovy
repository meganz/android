/**
 * This script is to build and upload Android AAB to Google Play Store Internal
 */

BUILD_STEP = ''

/**
 * Flag to decide whether we do clean before build SDK.
 * Possible values: yes|no
 */
REBUILD_SDK = "no"

/**
 * Flag to decide whether do clean up for SDK and Android code
 */
DO_CLEANUP = true

/**
 * Folder to contain build outputs, including APK, AAG and symbol files
 */
ARCHIVE_FOLDER = "archive"
NATIVE_SYMBOLS_FILE = "symbols.zip"
ARTIFACTORY_BUILD_INFO = "buildinfo.txt"

/**
 * Default release notes content files
 */
RELEASE_NOTES = "default_release_notes.json"

/**
 * common.groovy file with common methods
 */
def common

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }
    triggers { cron('0 7 * * 1-5') }   // every day at 7.00am NZ time, except Saturday and Sunday
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

        // Channel to add in the suffix of the version name
        APK_VERSION_NAME_CHANNEL_FOR_CD = "-internal"

        // default gitlabUserName for this script
        gitlabUserName = "Jenkins Pipeline"
        // default gitlabSourceBranch for this script
        gitlabSourceBranch = "develop"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
    }
    post {
        failure {
            script {
                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                slackSend color: 'danger', message: common.releaseFailureMessage("\n")
                slackUploadFile filePath: CONSOLE_LOG_FILE, initialComment: 'Jenkins Log'
            }
        }
        success {
            script {
                slackSend color: "good", message: releaseSuccessMessage("\n", common)
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
                }
            }
        }
        stage('Preparation') {
            steps {
                script {
                    BUILD_STEP = 'Preparation'

                    REBUILD_SDK = common.getValueInMRDescriptionBy("REBUILD_SDK")

                    sh("rm -frv ${WORKSPACE}/$ARCHIVE_FOLDER")
                    sh("mkdir -p ${WORKSPACE}/${ARCHIVE_FOLDER}")
                    sh("rm -fv ${CONSOLE_LOG_FILE}")
                    sh('set')
                }
            }
        }
        stage('Fetch native symbols') {
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
            steps {
                script {
                    BUILD_STEP = 'Apply Google Map API Key'

                    withCredentials([
                            file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_DEBUG', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_DEBUG'),
                            file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_RELEASE', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_RELEASE')
                    ]) {
                        println("applying production google map api config... ")
                        sh 'mkdir -p app/src/debug/res/values'
                        sh 'mkdir -p app/src/release/res/values'
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_DEBUG} app/src/debug/res/values/google_maps_api.xml"
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_RELEASE} app/src/release/res/values/google_maps_api.xml"
                    }
                }
            }
        }
        stage('Enable Permanent Logging') {
            steps {
                script {
                    BUILD_STEP = 'Enable Permanent Logging'

                    def featureFlagFile = "app/src/main/assets/featuretoggle/feature_flags.json"
                    common.setFeatureFlag(featureFlagFile, "PermanentLogging", true)
                    sh("cat $featureFlagFile")
                }
            }
        }
        stage('Build GMS APK') {
            steps {
                script {
                    BUILD_STEP = 'Build GMS APK'

                    sh './gradlew clean app:assembleGmsRelease'
                }
            }
        }
        stage('Sign GMS APK') {
            steps {
                script {
                    BUILD_STEP = 'Sign GMS APK'

                    withCredentials([
                            file(credentialsId: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE', variable: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE'),
                            file(credentialsId: 'ANDROID_PRD_GMS_APK_KEYSTORE', variable: 'ANDROID_PRD_GMS_APK_KEYSTORE')
                    ]) {
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
                              
                            echo Copy the signed production APK to original folder, for firebase upload in next step
                            rm -fv ${gmsApkInput}
                            cp -fv ${gmsApkOutput}  ${WORKSPACE}/app/build/outputs/apk/gms/release/
                        """
                        println("Finish signing APK. ($gmsApkOutput) generated!")
                    }
                }
            }
        }
        stage('Upload APK(GMS) to Firebase') {
            steps {
                script {
                    BUILD_STEP = 'Upload APK(GMS) to Firebase'

                    withCredentials([
                            file(credentialsId: 'android_firebase_credentials', variable: 'FIREBASE_CONFIG')
                    ]) {
                        withEnv([
                                "GOOGLE_APPLICATION_CREDENTIALS=$FIREBASE_CONFIG",
                                "RELEASE_NOTES_FOR_CD=${readReleaseNotesForFirebase()}"
                        ]) {
                            sh './gradlew appDistributionUploadGmsRelease'
                        }
                    }
                }
            }
        }
        stage('Upload Firebase Crashlytics symbol files') {
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
        stage('Build QA APK(GMS)') {
            steps {
                script {
                    BUILD_STEP = 'Build QA APK(GMS)'
                    withEnv([
                            "APK_VERSION_NAME_TAG_FOR_CD=_QA"
                    ]) {
                        sh './gradlew app:assembleGmsQa'
                    }
                }
            }
        }
        stage('Upload QA APK(GMS) to Firebase') {
            steps {
                script {
                    BUILD_STEP = 'Upload QA APK(GMS) to Firebase'

                    withCredentials([
                            file(credentialsId: 'android_firebase_credentials', variable: 'FIREBASE_CONFIG')
                    ]) {
                        withEnv([
                                "GOOGLE_APPLICATION_CREDENTIALS=$FIREBASE_CONFIG",
                                "RELEASE_NOTES_FOR_CD=${readReleaseNotesForFirebase()}"
                        ]) {
                            sh './gradlew appDistributionUploadGmsQa'
                        }
                    }
                }
            }
        }
        stage('Build GMS AAB') {
            steps {
                script {
                    BUILD_STEP = 'Build GMS AAB'

                    sh './gradlew clean app:bundleGmsRelease'
                }
            }
        }
        stage('Sign GMS AAB') {
            steps {
                script {
                    BUILD_STEP = 'Sign GMS AAB'

                    withCredentials([
                            string(credentialsId: 'ANDROID_PRD_GMS_AAB_PASSWORD', variable: 'ANDROID_PRD_GMS_AAB_PASSWORD'),
                            file(credentialsId: 'ANDROID_PRD_GMS_AAB_KEYSTORE', variable: 'ANDROID_PRD_GMS_AAB_KEYSTORE')
                    ]) {
                        println("signing GMS AAB")
                        String gmsAabInput = "${WORKSPACE}/app/build/outputs/bundle/gmsRelease/app-gms-release.aab"
                        String gmsAabOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${common.readAppVersion2()}-gms-release.aab"
                        println("input = $gmsAabInput \noutput = $gmsAabOutput")
                        withEnv([
                                "GMS_AAB_INPUT=${gmsAabInput}",
                                "GMS_AAB_OUTPUT=${gmsAabOutput}"
                        ]) {
                            sh('jarsigner -sigalg SHA1withRSA -digestalg SHA1 -keystore ${ANDROID_PRD_GMS_AAB_KEYSTORE} -storepass "${ANDROID_PRD_GMS_AAB_PASSWORD}" -signedjar ${GMS_AAB_OUTPUT} ${GMS_AAB_INPUT} megaandroid-upload')
                        }
                        println("Finish signing GMS AAB. ($gmsAabOutput) generated!")
                    }
                }
            }
        }
        stage('Archive files') {
            steps {
                script {
                    BUILD_STEP = 'Archive files'
                    println("Uploading files to Artifactory repo....")

                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN')
                    ]) {

                        String targetPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/internal/${common.artifactoryUploadPath()}/"

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
        stage('Deploy to Google Play Internal') {
            steps {
                script {
                    BUILD_STEP = 'Deploy to Google Play Internal'

                    // Get the formatted release notes
                    String release_notes = common.releaseNotes(RELEASE_NOTES)

                    // Upload the AAB to Google Play
                    androidApkUpload googleCredentialsId: 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL',
                            filesPattern: 'archive/*-gms-release.aab',
                            trackName: 'internal',
                            rolloutPercentage: '100',
                            additionalVersionCodes: '476,487',
                            nativeDebugSymbolFilesPattern: "archive/${NATIVE_SYMBOLS_FILE}",
                            recentChangeList: common.getRecentChangeList(release_notes),
                            releaseName: common.readAppVersion1()
                }
            }
        }
    }
}

/**
 * compose the success message of "deliver_appStore" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @param common The common functions loaded from common.groovy
 * @return The success message to be sent
 */
private String releaseSuccessMessage(String lineBreak, Object common) {
    return ":rocket: Android Release uploaded to Google Play Internal channel successfully!" +
            "${lineBreak}Version:\t${common.readAppVersion1()}" +
            "${lineBreak}Last Commit Msg:\t${common.lastCommitMessage()}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

/**
 * Create the release notes for Firebase App Distribution
 * @return release notes
 */
private String readReleaseNotesForFirebase() {
    String baseRelNotes = "Triggered by: $gitlabUserName" +
            "\nTrigger Reason: Push to develop branch" +
            "\nLast 5 git commits:\n${sh(script: "git log --pretty=format:\"(%h,%an)%x09%s\" -5", returnStdout: true).trim()}"
    return baseRelNotes
}
