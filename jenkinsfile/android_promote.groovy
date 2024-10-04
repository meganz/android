/**
 * This script is to promote Alpha builds to Beta or Production in Google Play Store
 */

@Library('jenkins-android-shared-lib') _

/**
 * common.groovy file with common methods
 */
def common

/**
 * To store the current build step
 */
BUILD_STEP = ''

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }

    options {
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
        timeout(time: 2, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }

    environment {
        LC_ALL = 'en_US.UTF-8'
        LANG = 'en_US.UTF-8'
        CONSOLE_LOG_FILE = 'console.txt'
    }

    parameters {
        choice(name: 'TRACK', choices: ['Select', 'Alpha', 'Beta', 'Production'], description: 'Target track/channel to promote to')
        choice(name: 'PERCENTAGE', choices: ['Select', '0', '25', '50', '100'], description: 'Rollout percentage')
        string(name: 'VERSION', defaultValue: '', description: 'Version of the build to be promoted, e.g. 14.4(242750450)')
    }

    post {
        failure {
            script {
                // Don't send message for validation errors
                if (BUILD_STEP != 'Validation') {
                    common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                    String track = params.TRACK.toLowerCase()
                    String version = params.VERSION
                    String versionName = extractVersionName(version)
                    String message = "Hi <!subteam^S02B2PB5SG7>, the promotion to $track has failed for v${version} at ${params.PERCENTAGE}% rollout, please check."

                    def slackChannelId = common.fetchSlackChannelIdsByReleaseVersion(versionName)[0]

                    if (slackChannelId == "") {
                        slackSend(channel: "android", color: "danger", message: message)
                    } else {
                        slackSend channel: slackChannelId, color: "danger", message: message, replyBroadcast: true
                    }
                    slackUploadFile filePath: CONSOLE_LOG_FILE, initialComment: 'Jenkins Log'
                }
            }
        }

        success {
            script {
                String track = params.TRACK.toLowerCase()
                String version = params.VERSION
                String versionName = extractVersionName(version)
                String packageLink = ""
                withCredentials([string(credentialsId: 'JIRA_BASE_URL', variable: 'JIRA_BASE_URL')]) {
                    packageLink = "${env.JIRA_BASE_URL}/issues/?jql=fixVersion%20%3D%20%22Android%20$versionName%22%20%20ORDER%20BY%20priority%20DESC%2C%20updated%20DESC"
                }
                String userType = track == 'production' ? "users" : "testers"
                String message = "The Android team has started the $track roll out process in Google Play for v$version. It is now being offered to ${params.PERCENTAGE}% of $track $userType.\n" +
                        "Here is the <$packageLink|v$versionName> Jira Release Package."

                def slackChannelId = common.fetchSlackChannelIdsByReleaseVersion(versionName)[0]

                if (slackChannelId == "") {
                    slackSend(channel: "android", message: message)
                } else {
                    slackSend channel: slackChannelId, message: message, replyBroadcast: true
                }
            }
        }
    }

    stages {
        stage('Load Common Script') {
            steps {
                script {
                    BUILD_STEP = 'Preparation'
                    common = load('jenkinsfile/common.groovy')
                    util.printEnv()
                }
            }
        }

        stage('Validation') {
            steps {
                script {
                    BUILD_STEP = 'Validation'
                    validateParams(params)
                    if (!env.BUILD_NUMBER) {
                        error("Pipeline must be run from the Jenkins dashboard")
                    }
                }
            }
        }

        stage('Deploy to Google Play') {
            steps {
                script {
                    BUILD_STEP = 'Deploy'
                    def versionCode = extractVersionCode(params.VERSION)
                    androidApkMove  googleCredentialsId : 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL',
                                    applicationId       : 'mega.privacy.android.app',
                                    trackName           : "${params.TRACK.toLowerCase()}",
                                    rolloutPercentage   : "${params.PERCENTAGE}",
                                    fromVersionCode     : true,
                                    versionCodes        : "233140859, $versionCode"
                }
            }
        }
    }
}

/**
 * Validate pipeline parameters
 * @param params
 */
def validateParams(params) {
    if (!params.TRACK) {
        error("'TRACK' parameter is missing. Please specify the target track.")
    } else if (params.TRACK == 'Select') {
        error("'TRACK' was not selected. Please choose a valid target track (e.g., Beta or Production).")
    }

    if (!params.PERCENTAGE) {
        error("'PERCENTAGE' parameter is missing. Please specify the rollout percentage.")
    } else if (params.PERCENTAGE == 'Select') {
        error("'PERCENTAGE' parameter was not selected. Please choose a valid rollout percentage (e.g., 25, 50, or 100).")
    } else if (!(params.PERCENTAGE.isInteger() && params.PERCENTAGE.toInteger() in 0..100)) {
        error("Invalid 'PERCENTAGE' value. Percentage must be an integer between 0 and 100.")
    }

    if (!params.VERSION) {
        error("'VERSION' parameter is missing. Please provide the version of the build (e.g., 14.1(242330236)).")
    } else if (!isValidVersion(params.VERSION)) {
        error("Invalid 'VERSION' format. Please provide a valid version (e.g., 14.1(242330236))")
    }
}

/**
 * Validate release version
 * @param version
 * @return
 */
private static def isValidVersion(String version) {
    def pattern = ~/^\d+\.\d+(\.\d+)?\(\d+\)$/
    return pattern.matcher(version).matches()
}

/**
 * Extract version code
 * @param version eg. 14.1(242330236)
 * @return version code eg. 242330236
 */
private def extractVersionCode(String version) {
    def matcher = version =~ /\((\d+)\)/
    if (matcher.find()) {
        return matcher[0][1]
    } else {
        error("Unable to extract version code")
    }
}

/**
 * Extract version name
 * @param version eg. 14.1(242330236)
 * @return version name eg. 14.1
 */
private def extractVersionName(String version) {
    def matcher = version =~ /(.*)\(\d+\)/
    if (matcher.find()) {
        return matcher[0][1].trim()
    } else {
        error("Unable to extract version name")
    }
}