/**
 * This script is to promote Alpha builds to Beta or Production in Google Play Store
 */

@Library('jenkins-android-shared-lib') _

/**
 * common.groovy file with common methods
 */
def common

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }

    options {
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
        timeout(time: 2, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }

    parameters {
        choice(name: 'TRACK', choices: ['Select', 'Alpha', 'Beta', 'Production'], description: 'Target track/channel to promote to')
        choice(name: 'PERCENTAGE', choices: ['Select', '0', '25', '50', '100'], description: 'Rollout percentage')
        string(name: 'VERSION', defaultValue: '', description: 'Version code of the build to be promoted, e.g. 242330236')
    }

    stages {
        stage('Validation') {
            steps {
                script {
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
                    androidApkMove  googleCredentialsId : 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL',
                                    applicationId       : 'mega.privacy.android.app',
                                    trackName           : "${params.TRACK.toLowerCase()}",
                                    rolloutPercentage   : "${params.PERCENTAGE}",
                                    fromVersionCode     : true,
                                    versionCodes        : "233140859, ${params.VERSION}"
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
        error("'VERSION' parameter is missing. Please provide the version code of the build (e.g., 242330236).")
    } else if (!params.VERSION.isInteger()) {
        error("Invalid 'VERSION' value. Please provide a valid version code of the build (e.g., 242330236).")
    }
}