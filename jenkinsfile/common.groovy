/**
 * This file is a common module that hosts methods that are called by different CI/CD scripts.
 */

/**
 * Check out mega chat SDK by commit ID
 *
 * @param megaChatCommitId commit ID
 */
void checkoutMegaChatSdkByCommit(String megaChatCommitId) {
    println("####### Entering common.checkoutMegaChatSdkByCommit() #######")
    sh """
    echo checkoutMegaChatSdkByCommit
    cd $WORKSPACE
    cd sdk/src/main/jni/megachat/sdk
    git checkout $megaChatCommitId
    cd $WORKSPACE
    """
}

/**
 * checkout SDK by commit ID
 * @param sdkCommitId the commit ID to checkout
 */
void checkoutSdkByCommit(String sdkCommitId) {
    println("####### Entering common.checkoutSdkByCommit() #######")
    sh """
    echo checkoutSdkByCommit
    cd $WORKSPACE
    cd sdk/src/main/jni/mega/sdk
    git checkout $sdkCommitId
    cd $WORKSPACE
    """
}

/**
 * set up SDK submodules and check out to latest develop branch
 */
void fetchSdkSubmodules() {
    println("####### Entering common.fetchSdkSubmodules() #######")
    gitlabCommitStatus(name: 'Fetch SDK Submodules') {
        withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
            script {
                sh '''
                    cd ${WORKSPACE}
                    git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".url https://code.developers.mega.co.nz/sdk/sdk.git
                    git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".branch develop
                    git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".url https://code.developers.mega.co.nz/megachat/MEGAchat.git
                    git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".branch develop
                    git submodule sync
                    git submodule update --init --recursive --remote 
                    cd sdk/src/main/jni/mega/sdk
                    git fetch
                    git checkout develop
                    git pull
                    cd ../../megachat/sdk
                    git fetch
                    git checkout develop
                    git pull
                    cd ${WORKSPACE}
                '''
            }
        }
    }
}

/**
 * Check if this build is triggered by a GitLab Merge Request.
 * @return true if this build is triggered by a GitLab MR. False if this build is triggerd
 * by a plain git push.
 * This method can be used for both CI and CD pipeline.
 */
private boolean hasGitLabMergeRequest() {
    def hasMergeRequestInCD = env.gitlabMergeRequestIid != null && !env.gitlabMergeRequestIid.isEmpty()
    def hasMergeRequestInCI = env.BRANCH_NAME != null && env.BRANCH_NAME.startsWith('MR-')

    return hasMergeRequestInCD || hasMergeRequestInCI
}

/**
 * Get the MergeRequest ID for CI
 * @return MR number if job is triggered in CI. Otherwise return null.
 */
def getMrNumberInCI() {
    def branchName = env.BRANCH_NAME
    if (branchName != null && branchName.startsWith('MR-')) {
        return branchName.replace('MR-', '')
    } else {
        return null
    }
}

/**
 * Get MergeRequest ID for CD
 *
 * @return MR Number if job is triggered in CD. Otherwise return null.
 */
def getMrNumberInCD() {
    return env.gitlabMergeRequestIid
}

/**
 * send message to GitLab MR comment
 * @param message message to send
 */
void sendToMR(String message) {
    println("####### Entering common.sendToMR() #######")

    def mrNumber = getMrNumberInCD()
    if (mrNumber == null) {
        mrNumber = getMrNumberInCI()
    }

    if (mrNumber != null && !mrNumber.isEmpty()) {
        withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
            env.MARKDOWN_LINK = message
            env.MERGE_REQUEST_URL = "https://code.developers.mega.co.nz/api/v4/projects/199/merge_requests/${mrNumber}/notes"
            sh 'curl --request POST --header PRIVATE-TOKEN:$TOKEN --form body=\"${MARKDOWN_LINK}\" ${MERGE_REQUEST_URL}'
        }
    }
}

/**
 * download jenkins build console log and save to file.
 */
void downloadJenkinsConsoleLog(String downloaded) {
    withCredentials([usernameColonPassword(credentialsId: 'Jenkins-Login', variable: 'CREDENTIALS')]) {
        sh "curl -u $CREDENTIALS ${BUILD_URL}/consoleText -o ${downloaded}"
    }
}

return this