/**
 * This script is to automatically clean up the gradle cache on Jenkins agents.
 */

def common

pipeline {
    agent { label 'macmini2' }
    triggers { cron('0 15 * * 6') }   // every day at 15:00 NZ time on Saturday
    options {
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
        timeout(time: 1, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }
    environment {
        LC_ALL = "en_US.UTF-8"
        LANG = "en_US.UTF-8"

        NDK_ROOT = "/opt/buildtools/android-sdk/ndk/21.3.6528147"
        JAVA_HOME = "/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx"
        ANDROID_HOME = "/opt/buildtools/android-sdk"

        // PATH for necessary commands
        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu17.42.19-ca-jdk17.0.7-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        // Jenkins build log will be saved in this file.
        CONSOLE_LOG_FILE = "console.txt"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
    }
    post {
        failure {
            script {
                common = load('jenkinsfile/common.groovy')
                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                slackSend color: 'danger', message: "Jenkins gradle clean up failed! <@U02G29K2065>"
                slackUploadFile filePath: CONSOLE_LOG_FILE, initialComment: 'Jenkins Log'
            }
        }
        success {
            script {
                slackSend color: "good", message: "Jenkins gradle clean up succeeded!"
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
        stage("clean all agents") {
            parallel {
                stage('macmini4') {
                    agent { label 'macmini4' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
                stage('macmini5') {
                    agent { label 'macmini5' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
                stage('macmini6') {
                    agent { label 'macmini6' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
                stage('macstudio1') {
                    agent { label 'macstudio1' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
                stage('macstudio2') {
                    agent { label 'macstudio2' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
                stage('macstudio5') {
                    agent { label 'macstudio5' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
                stage('macstudio6') {
                    agent { label 'macstudio6' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
                stage('macstudio7') {
                    agent { label 'macstudio7' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
                stage('macstudio8') {
                    agent { label 'macstudio8' }
                    steps {
                        script {
                            cleanCache()
                            buildProject()
                        }
                    }
                }
            }
        }
        stage('macmini2 self') {
            steps {
                script {
                    cleanCache()
                    buildProject()
                }
            }
        }
    }
}

void cleanCache() {
    sh "rm -fr ${env.HOME}/.gradle/caches"
}

/**
 * build the project and run tests and lint
 */
void buildProject() {
    def common = load('jenkinsfile/common.groovy')
    common.downloadDependencyLibForSdk()

    withCredentials([
            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN'),
    ]) {
        withEnv([
                "ARTIFACTORY_USER=${ARTIFACTORY_USER}",
                "ARTIFACTORY_ACCESS_TOKEN=${ARTIFACTORY_ACCESS_TOKEN}"
        ]) {
            sh "./gradlew clean"
            sh "./gradlew app:assembleGmsDebug"
            sh "./gradlew lint"
            sh "./gradlew app:testGmsDebugUnitTest"
            sh "./gradlew domain:test"
            sh "./gradlew feature:devicecenter:testDebugUnitTest"
            sh "./gradlew feature:sync:testDebugUnitTest"
            sh "./gradlew core-ui:testDebugUnitTest"
        }
    }
}

