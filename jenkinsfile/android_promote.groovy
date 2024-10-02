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
        // Stop the build early in case of compile or test failures
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
        timeout(time: 2, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }

    parameters {
        choice(name: 'TRACK', choices: ['Select', 'Internal', 'Alpha', 'Beta', 'Production'], description: 'Target track/channel to promote to')
        choice(name: 'PERCENTAGE', choices: ['Select', '0', '25', '50', '100'], description: 'Rollout percentage')
        string(name: 'VERSION', defaultValue: '', description: 'Version of the build to be promoted, e.g. 14.1(242330236)')
    }

    stages {
        stage('Validation') {
            steps {
                script {
                    validateParams(params)
                    if (!env.BUILD_NUMBER) {
                        error("Pipeline must be run the Jenkins dashboard")
                    }
                }
            }
        }

        stage('Print Provided Params') {
            steps {
                echo "Target Track: ${params.TRACK.toLowerCase()}, Rollout: ${params.PERCENTAGE}, Version: ${params.VERSION}"
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
        error("'PERCENTAGE' parameter wasn't selected. Please choose a valid rollout percentage (e.g., 25, 50, or 100)")
    } else if (!(params.PERCENTAGE.isInteger() && params.PERCENTAGE.toInteger() in 0..100)) {
        error("Invalid 'PERCENTAGE' value. Percentage must be an integer between 0 and 100.")
    }

    if (!params.VERSION) {
        error("'VERSION' parameter is missing. Please provide the version of the build (e.g., 14.1(242330236)).")
    } else if (!isValidReleaseVersion(params.VERSION)) {
        error("Invalid 'VERSION' format. Please provide a valid release version (e.g., 14.1(242330236))")
    }
}

/**
 * Validate release version
 * @param version
 * @return
 */
static def isValidReleaseVersion(String version) {
    def pattern = ~/^\d+\.\d+\(\d+\)$/
    return pattern.matcher(version).matches()
}