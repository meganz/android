/**
 * This script is to automatically clean up the remote gradle caches
 */


/**
 * common.groovy file with common methods
 */
def common

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }
    triggers { cron('0 5 * * 1-5') }   // every day at 5.00am NZ time, except Saturday and Sunday
    options {
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
        timeout(time: 2, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }
    environment {
        // Jenkins build log will be saved in this file.
        CONSOLE_LOG_FILE = "console.txt"
        PATH = "/opt/brew/bin:$PATH"
    }
    post {
        failure {
            script {
                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                slackSend color: 'danger', message: cleanupFailureMessage()
                slackUploadFile filePath: CONSOLE_LOG_FILE, initialComment: 'Jenkins Log'
            }
        }
        success {
            script {
                slackSend color: "good", message: cleanupSuccessMessage("\n")
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
                    // load the common library script
                    common = load('jenkinsfile/common.groovy')
                }
            }
        }
        stage("Clean up") {
            steps {
                script {
                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN'),
                    ]) {
                        withEnv([
                                "ARTIFACTORY_USER=${ARTIFACTORY_USER}",
                                "ARTIFACTORY_ACCESS_TOKEN=${ARTIFACTORY_ACCESS_TOKEN}"
                        ]) {
                            String daysToKeep = "3"
                            sh """
                                echo #### querying old cache files....
                                curl -u ${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -X POST -k -H 'Content-Type:text/plain' ${ARTIFACTORY_BASE_URL}/artifactory/api/search/aql --data 'items.find({\"repo\": \"android-mega\",\"path\": {\"\$match\": \"*gradle-cache*\"},\"created\": {\"\$before\": \"${daysToKeep}d\"}})' > aql_results.json

                                echo #### summary
                                tail aql_results.json
                                                                
                                echo #### deleting files
                                for name in \$(jq -r '.results[].name' < aql_results.json); do
                                    echo deleting \$name; 
                                    curl -u ${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -X DELETE "${ARTIFACTORY_BASE_URL}/artifactory/android-mega/gradle-cache/\$name"; 
                                done
                            """
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compose the success message of cache clean up.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>"
 * @return The success message to be sent
 */
private String cleanupSuccessMessage(String lineBreak) {
    String cleanedCaches =  sh(script: "jq -r '.range.total' < aql_results.json", returnStdout: true).trim()
    return ":rocket: Remote Gradle Cache clean-up succeeded!" +
            "${lineBreak}cleaned caches:\t${cleanedCaches}"
}

String cleanupFailureMessage() {
    return  ":x: Remote Gradle Cache clean-up failed!"
}

