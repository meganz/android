/**
 * This file is a common module that hosts methods that are called by different CI/CD scripts.
 */


import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

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
                sh """
                    cd ${WORKSPACE}
                    git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".url ${env.GITLAB_BASE_URL}/sdk/sdk.git
                    git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".branch develop
                    git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".url ${env.GITLAB_BASE_URL}/megachat/MEGAchat.git
                    git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".branch develop
                    git submodule sync
                    git submodule update --init --recursive --remote
                    cd sdk/src/main/jni/mega/sdk
                    git fetch
                    git checkout ${SDK_BRANCH}
                    git pull || true
                    cd ../../megachat/sdk
                    git fetch
                    git checkout ${MEGACHAT_BRANCH}
                    git pull || true
                    cd ${WORKSPACE}
                """
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

    def mrNumber = getMrNumber()
    if (mrNumber != null && !mrNumber.isEmpty()) {
        withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
            env.MESSAGE_BODY = message
            env.MERGE_REQUEST_URL = "${env.GITLAB_BASE_URL}/api/v4/projects/199/merge_requests/${mrNumber}/notes"
            sh 'curl --request POST --header PRIVATE-TOKEN:$TOKEN --form body=\"${MESSAGE_BODY}\" ${MERGE_REQUEST_URL}'
        }
    }
}

/**
 * Send a large markdown file as a GitLab MR comment using a JSON payload.
 * Wraps the content in a collapsible <details> block and uses jq to safely
 * encode the file as JSON. Use this instead of sendToMR() for large content
 * such as code review reports.
 *
 * @param filePath absolute or workspace-relative path to the markdown file to send
 * @param summary the summary of the MR comment
 */
void sendFileToMRComment(String filePath, String summary) {
    println("####### Entering common.sendLargeTextToMR() #######")

    def mrNumber = getMrNumber()
    if (mrNumber != null && !mrNumber.isEmpty()) {
        withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
            def jsonPayloadFile = "${WORKSPACE}/.mr_comment_payload_${System.currentTimeMillis()}.json"
            env.MR_COMMENT_SOURCE_FILE = filePath
            env.MR_COMMENT_PAYLOAD_FILE = jsonPayloadFile
            env.COMMENT_SUMMARY = summary
            env.MERGE_REQUEST_URL = "${env.GITLAB_BASE_URL}/api/v4/projects/199/merge_requests/${mrNumber}/notes"
            sh '''
                jq -n --rawfile body ${MR_COMMENT_SOURCE_FILE} \
                    --arg summary "${COMMENT_SUMMARY}" \
                    '{"body": ("<details><summary>Code Review Report\n\n" + $summary + "\n\n</summary>" + $body + "</details>")}' \
                    > ${MR_COMMENT_PAYLOAD_FILE}
                curl --request POST \
                     --header "PRIVATE-TOKEN:$TOKEN" \
                     --header "Content-Type: application/json" \
                     --data @${MR_COMMENT_PAYLOAD_FILE} \
                     ${MERGE_REQUEST_URL}
                rm -f ${MR_COMMENT_PAYLOAD_FILE}
            '''
        }
    }
}

/**
 * download jenkins build console log and save to file.
 */
void downloadJenkinsConsoleLog(String downloaded) {
    println("entering downloadJenkinsConsoleLog()")
    withCredentials([usernameColonPassword(credentialsId: 'Jenkins-Login', variable: 'CREDENTIALS')]) {
        withEnv([
                "DOWNLOADED=$downloaded"
        ]) {
            sh 'curl -u ${CREDENTIALS} ${BUILD_URL}consoleText -o ${DOWNLOADED}'
        }
    }
}

/**
 * checkout SDK by git tag
 * @param sdkTag the tag to checkout
 */
void checkoutSdkByTag(String sdkTag) {
    sh """
    echo checkoutSdkByTag
    cd $WORKSPACE
    cd sdk/src/main/jni/mega/sdk
    git checkout tags/$sdkTag
    cd $WORKSPACE
    """
}

/**
 * checkout MEGAchat SDK by git tag
 * @param megaChatTag the tag to checkout
 */
void checkoutMegaChatSdkByTag(String megaChatTag) {
    sh """
    echo checkoutMegaChatSdkByTag
    cd $WORKSPACE
    cd sdk/src/main/jni/megachat/sdk
    git checkout tags/$megaChatTag
    cd $WORKSPACE
    """
}

/**
 * Read the prebuilt SDK version from project build.gradle.kts
 * @return version of prebuilt SDK
 */
String readPrebuiltSdkVersion() {
    String version = sh(script: "./gradlew --no-daemon -q printPrebuildSdkVersion  | tail -n 1", returnStdout: true).trim()
    println("readPrebuiltSdkVersion version = $version")
    return version
}

/**
 * Query prebuit SDK properties from Artifactory Maven repo <p>
 *
 * @param property the property to query. possible value: 'sdk-commit', 'chat-commit'
 * @param version version of the pre-built SDK. It can be read at the value of megaSdkVersion in
 * project build.gradle.kts file.
 * @return property value
 */
def queryPrebuiltSdkProperty(String property, String version) {
    def commit = "N/A"
    def cmd = "curl ${env.ARTIFACTORY_BASE_URL}/artifactory/api/storage/mega-gradle/mega-sdk-android/nz/mega/sdk/sdk/${version}/sdk-${version}.aar?properties"
    def response = sh(script: cmd, returnStdout: true).trim()
    def properties = new JsonSlurperClassic().parseText(response).properties
    if (properties != null) {
        commit = properties[property][0]
    }
    println("$property = $commit")
    return commit
}

/**
 * checkout SDK by branch
 * @param sdkBranch the branch to checkout
 */
void checkoutSdkByBranch(String sdkBranch) {
    sh "echo checkoutSdkByBranch"
    sh "cd \"$WORKSPACE\""
    sh "git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".url ${env.GITLAB_BASE_URL}/sdk/sdk.git"
    sh "git config --file=.gitmodules submodule.\"sdk/src/main/jni/mega/sdk\".branch \"$sdkBranch\""
    sh 'git submodule sync'
    sh 'git submodule update --init --recursive --remote'
}

/**
 * checkout MEGAchat SDK by branch
 * @param megaChatBranch the branch to checkout
 */
void checkoutMegaChatSdkByBranch(String megaChatBranch) {
    sh "echo checkoutMegaChatSdkByBranch"
    sh "cd \"$WORKSPACE\""
    sh "git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".url ${env.GITLAB_BASE_URL}/megachat/MEGAchat.git"
    sh "git config --file=.gitmodules submodule.\"sdk/src/main/jni/megachat/sdk\".branch \"${megaChatBranch}\""
    sh 'git submodule sync'
    sh 'git submodule update --init --recursive --remote'
}

/**
 * Upload file to GitLab and return the GitLab link
 * @param fileName the local file to be uploaded.
 * @return file link on GitLab
 */
String uploadFileToGitLab(String fileName) {
    String link = ""
    withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
        try {
            final String response = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${fileName} ${env.GITLAB_BASE_URL}/api/v4/projects/199/uploads", returnStdout: true).trim()
            link = new groovy.json.JsonSlurperClassic().parseText(response).markdown
        } catch (Exception e) {
            link = "Failed to upload file ${fileName} to GitLab(${e.toString()})"
        }
    }
    return link
}

/**
 *  Check the feature flag json file and set the feature flag.
 *  If the feature_flag.json file already contains the flagName, set the flagValue.
 *  Otherwise add the flagName and set the flagValue.
 *  If featureFlagFile does not exist, a new file will be created.<p/>
 *
 * @param featureFlagFile relative path of the feature_flag.json file
 * @param flagName name of the feature flag
 * @param flagValue boolean value of the flag
 */
def setFeatureFlag(String featureFlagFile, String flagName, boolean flagValue) {
    def flagList
    if (fileExists(featureFlagFile)) {
        def fileContents = readFile(featureFlagFile)
        flagList = new JsonSlurperClassic().parseText(fileContents)
    } else {
        println("setFeatureFlag() $featureFlagFile not exist!")
        flagList = new ArrayList()
    }

    def exist = false
    for (feature in flagList) {
        def name = feature["name"]
        if (name == flagName) {
            feature["value"] = flagValue
            exist = true
            break
        }
    }

    if (!exist) {
        def newFeature = new HashMap<String, String>()
        newFeature["value"] = flagValue
        newFeature["name"] = flagName
        flagList.add(newFeature)
    }

    def result = JsonOutput.prettyPrint(JsonOutput.toJson(flagList))
    writeFile file: featureFlagFile, text: result.toString()
}

/**
 * Compose the failure message of "deliver_appStore" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The failure message to be sent
 */
String releaseFailureMessage(String lineBreak) {
    return releaseFailureMessage(lineBreak, "")
}

/**
 * A variant of releaseFailureMessage method
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * @param postfix additional message to be appended to the end of the message
 * @return The failure message to be sent
 */
String releaseFailureMessage(String lineBreak, String postfix) {
    String message = ":x: Android Release Failed!" +
            "${lineBreak}Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
    if (env.gitlabActionType == "PUSH") {
        message += "${lineBreak}Trigger Reason: git PUSH"
    } else if (env.gitlabActionType == "NOTE") {
        message += "${lineBreak}Trigger Reason: MR comment (${gitlabTriggerPhrase})"
    }
    if (!postfix.isEmpty()) {
        message += "${lineBreak}${postfix}"
    }
    return message
}

/**
 * compose the success message of "upload_symbol" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
String uploadSymbolFailureMessage(String lineBreak) {
    return ":x: Android Firebase Crashlytics symbol upload Failed!" +
            "${lineBreak}Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

/**
 * compose the success message of "upload_symbol" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @return The success message to be sent
 */
String uploadSymbolSuccessMessage(String lineBreak) {
    return ":rocket: Firebase Crashlytics symbol uploaded successfully!" +
            "${lineBreak}Last Commit Msg:\t${lastCommitMessage()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

String sdkCommitId() {
    String commitId = sh(
            script: """
                cd ${WORKSPACE}/sdk/src/main/jni/mega/sdk
                git rev-parse HEAD
                """,
            returnStdout: true).trim()
    println("sdk commit id = ${commitId}")
    return commitId
}

String appCommitId() {
    String commitId = sh(
            script: """
                cd ${WORKSPACE}
                git rev-parse HEAD
                """,
            returnStdout: true).trim()
    println("Android commit id = ${commitId}")
    return commitId
}

String megaChatSdkCommitId() {
    String commitId = sh(
            script: """
                cd ${WORKSPACE}/sdk/src/main/jni/megachat/sdk
                git rev-parse HEAD
                """,
            returnStdout: true).trim()
    println("chat sdk commit id = ${commitId}")
    return commitId
}

/**
 * create a build info file with key version information of build.
 * This file will be uploaded to Artifactory repo.
 *
 */
def createBriefBuildInfoFile() {
    def content = """
Version: v${readAppVersion1()}
Upload Time: ${new Date().toString()}
Android: branch(${env.gitlabSourceBranch}) - commit(${appCommitId()})
SDK version: ${readPrebuiltSdkVersion()}
"""
    sh "rm -fv ${ARTIFACTORY_BUILD_INFO}"
    sh "echo \"${content}\" >> ${WORKSPACE}/${ARCHIVE_FOLDER}/${ARTIFACTORY_BUILD_INFO}"
}

/**
 * read the version name from source code(build.gradle.kts)
 * read the version code from environment variable
 *
 * @return a tuple of version code and version name
 */
def readAppVersion() {
    String versionCode = APK_VERSION_CODE_FOR_CD
    String versionName = sh(script: "./gradlew --no-daemon -q printAppVersionName  | tail -n 1", returnStdout: true).trim()
    String versionNameChannel = sh(script: "./gradlew --no-daemon -q printAppVersionNameChannel | tail -n 1", returnStdout: true).trim()
    String appGitHash = sh(script: "./gradlew --no-daemon -q printAppGitHash | tail -n 1", returnStdout: true).trim()
    return [versionName, versionNameChannel, versionCode, appGitHash]
}

/**
 * get app version in a format like "7.2(230111014)(5cf9df7410c)"
 * @return version string
 */
String readAppVersion1() {
    def (versionName, versionNameChannel, versionCode, appGitHash) = readAppVersion()
    return versionName + versionNameChannel + "(" + versionCode + ")" + "(" + appGitHash + ")"
}

/**
 * get app version in a format like "230111014_5cf9df7410c_7_2" (for 7.2(230111014)(5cf9df7410c))
 * @return version string
 */
String readAppVersion2() {
    def (versionName, versionNameChannel, versionCode, appGitHash) = readAppVersion()
    return "${versionCode}_${appGitHash}_${versionName.replaceAll("\\.", "_")}${versionNameChannel.replaceAll("-", "_")}"
}

/**
 * read the last git commit message
 * @return last git commit message
 */
String lastCommitMessage() {
    return sh(script: "git log --pretty=format:\"%x09%s\" -1", returnStdout: true).trim()
}

void deleteAllFilesExcept(String folder, String except) {
    println("Deleting all files except ${except} in folder ${folder}")
    sh """
        cd ${folder}
        mv -v ${except} /tmp/
        rm -fr *
        mv -v /tmp/${except} .
    """
}

/**
 * get relative path of artifactory folder
 * @return relative path.
 */
String artifactoryUploadPath() {
    def (versionName, versionNameChannel, versionCode, appGitHash) = readAppVersion()
    return "v${versionName}${versionNameChannel.replaceAll("-", "_")}/${versionCode}_${appGitHash}"
}

/**
 * clean SDK
 */
void cleanSdk() {
    println("clean SDK")
    sh """
        cd $WORKSPACE/sdk/src/main/jni
        bash build.sh clean
    """
}

/**
 * clean Android project
 */
void cleanAndroid() {
    println("clean Android code")
    sh """
        cd $WORKSPACE
        ./gradlew --no-daemon clean
    """
}

/**
 * print the size of workspace.
 * @param prompt a prompt message can be printed before the size value.
 */
void printWorkspaceSize(String prompt) {
    println(prompt)
    sh """
        cd ${WORKSPACE}
        du -sh
    """
}


/**
 * Get the list of recent changes (release note) json string input
 * and return a formatted list following below example
 * [
 *  [language: 'en-GB', text: "Please test the changes from Jenkins build ${env.BUILD_NUMBER}."],
 *  [language: 'de-DE', text: "Bitte die Änderungen vom Jenkins Build ${env.BUILD_NUMBER} testen."]
 * ]
 *
 * @param input the json string to parse
 * @return the list of recent changes formatted
 */
def getRecentChangeList(input) {
    def map = []
    def languages = new groovy.json.JsonSlurperClassic().parseText(input)
    def keyList = languages.keySet()
    keyList.each { language ->
        def languageMap = [:]
        languageMap["language"] = "${language}"
        languageMap["text"] = "${languages[language]}"
        map.add(languageMap)
    }
    return map
}

/**
 * Get release notes content from releaseNoteFile
 * releaseNoteFile should be in json format
 *
 * @return a String with the content of releaseNoteFile
 */
String releaseNotes(releaseNoteFile) {
    String release_notes = sh(
            script: """
                cat ${WORKSPACE}/$releaseNoteFile
                """,
            returnStdout: true).trim()
    return release_notes
}

/**
 * check if a certain value is defined by checking the tag value
 * @param value value of tag
 * @return true if tag has a value. false if tag is null or zero length
 */
boolean isDefined(String value) {
    return value != null && !value.isEmpty()
}

/**
 * Get the value from GitLab MR description by key
 * @param key the key to check and read
 * @return actual value of key if key is specified. null otherwise.
 */
String getValueInMRDescriptionBy(String key) {
    if (key == null || key.isEmpty()) return null
    def description = env.gitlabMergeRequestDescription
    if (description == null) return null
    String[] lines = description.split('\n')
    for (String line : lines) {
        line = line.trim()
        if (line.startsWith(key)) {
            String value = line.substring(key.length() + 1)
            print("getValueInMRDescriptionBy(): " + key + " ==> " + value)
            return value
        }
    }
    return null
}

void downloadAndExtractNativeSymbols() {
    String nativeSymbolLocation = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/cicd/native-symbol/${readPrebuiltSdkVersion()}.zip"
    String targetObjLocalLocation = "sdk/src/main/obj/local"
    sh """
        cd ${WORKSPACE}
        curl -u ${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -o ${ARCHIVE_FOLDER}/${NATIVE_SYMBOLS_FILE} ${nativeSymbolLocation}
        rm -frv ${targetObjLocalLocation}
        mkdir -p ${targetObjLocalLocation}
        unzip -o ${ARCHIVE_FOLDER}/${NATIVE_SYMBOLS_FILE} -d ${targetObjLocalLocation}
    """
}


/**
 * Enable Artifactory and call the closure function
 * @param closure
 */
void useArtifactory(Closure closure) {
    withCredentials([
            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN'),
    ]) {
        withEnv([
                "ARTIFACTORY_USER=${ARTIFACTORY_USER}",
                "ARTIFACTORY_ACCESS_TOKEN=${ARTIFACTORY_ACCESS_TOKEN}"
        ]) {
            closure.call()
        }
    }

}

/**
 * Download a file from remoteUrl and save it to localTarget
 * @param remoteUrl
 * @param localTarget
 */
void downloadFromArtifactory(String remoteUrl, String localTarget) {
    useArtifactory() {
        sh """
            cd ${WORKSPACE}
            rm -fv ${localTarget}
            curl -f -u ${env.ARTIFACTORY_USER}:${env.ARTIFACTORY_ACCESS_TOKEN} -o ${localTarget} ${remoteUrl}
            ls
        """
    }
}

String getMrNumber() {
    def mrNumber = getMrNumberInCD()
    if (mrNumber == null) {
        mrNumber = getMrNumberInCI()
    }
    return mrNumber
}

/**
 * Upload a localFile to a remoteTarget on Artifactory
 * @param remoteTarget
 * @param localFile
 */
void uploadToArtifactory(String localFile, String remoteTarget) {
    useArtifactory() {
        sh """
            cd ${WORKSPACE}
            curl -u ${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${localFile} ${remoteTarget}
            ls   
        """
    }
}

/**
 * Upload a file to Artifactory.
 * The file will be uploaded to "android-mega/pipeline-uploads" folder in Artifactory.
 * The file will be uploaded to a folder and named "MR-<MR_NUMBER>/<BUILD_NUMBER>-<FILE_NAME>".
 * @param fileName the file to upload
 * @return the URL of the uploaded file.
 */
String uploadFileToArtifactory(String fileName) {
    def mrNumber = getMrNumber()
    if (mrNumber == null || mrNumber.isEmpty()) {
        return "NA"
    }
    String targetFolder = "artifactory/android-mega/pipeline-uploads"
    String targetFile = "MR-${mrNumber}/${env.BUILD_NUMBER}-${fileName}"

    useArtifactory() {
        String remoteTargetPath = "${env.ARTIFACTORY_BASE_URL}/${targetFolder}/${targetFile}"
        String uploadCmd = "curl -u ${env.ARTIFACTORY_USER}:${env.ARTIFACTORY_ACCESS_TOKEN} -T ${fileName} ${remoteTargetPath}"
        sh """
            cd ${WORKSPACE}
            ${uploadCmd}
            ls
        """
    }
    return "${env.ARTIFACTORY_BASE_URL}:443/${targetFolder}/${targetFile}"
}

/**
 * Upload a file to Artifactory.
 * The file will be uploaded to "android-mega/pipeline-uploads" folder in Artifactory.
 * The file will be uploaded to a folder and named "folder/<BUILD_NUMBER>-<FILE_NAME>".
 * @param fileName the file to upload
 * @return the URL of the uploaded file.
 */
String uploadFileToArtifactory(String folder, String fileName) {
    String targetFolder = "artifactory/android-mega/pipeline-uploads"
    String targetFile = "${folder}/${env.BUILD_NUMBER}-${fileName}"

    useArtifactory() {
        String remoteTargetPath = "${env.ARTIFACTORY_BASE_URL}/${targetFolder}/${targetFile}"
        String uploadCmd = "curl -u ${env.ARTIFACTORY_USER}:${env.ARTIFACTORY_ACCESS_TOKEN} -T ${fileName} ${remoteTargetPath}"
        sh """
            cd ${WORKSPACE}
            ${uploadCmd}
            ls
        """
    }
    return "${env.ARTIFACTORY_BASE_URL}:443/${targetFolder}/${targetFile}"
}

/**
 * Fetch slack channel->thread IDs by release version from Artifactory
 * @param version the release version
 * @return a list of slack channel IDs. The first element is the Slack #android channel->release thread ID for release version, the second element is the #qa > release thread ID.
 */
def fetchSlackChannelIdsByReleaseVersion(String version) {
    // Ignore hotfix version so that the same channel/thread is used if the version has hotfix
    def formattedVersionName = version.split("\\.")[0..1].join(".")
    def slackInfoFileName = "slack_info.txt"

    // Fetch slack channel->thread id from Artifactory if exists
    String slackInfoPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/release/v${formattedVersionName}/${slackInfoFileName}"
    try {
        downloadFromArtifactory(slackInfoPath, slackInfoFileName)
    } catch (Exception ignored) {
        println("slack_info.txt not found in Artifactory.")
    }

    def androidChannelThreadId = ""
    def qaChannelThreadId = ""
    if (fileExists(WORKSPACE + "/" + slackInfoFileName)) {
        def content = readFile(WORKSPACE + "/" + slackInfoFileName).trim()
        def slackInfo = content.split(",")
        if (slackInfo.size() > 0) {
            androidChannelThreadId = slackInfo[0]
        }
        if (slackInfo.size() > 1) {
            qaChannelThreadId = slackInfo[1]
        }
    }

    return [androidChannelThreadId, qaChannelThreadId]
}


/**
 * Gets list of all module paths in the project by parsing output from printSubprojectPaths task.
 * @return List of module paths like ["app", "domain", "feature/chat"]
 */
ArrayList<String> getModuleList() {
    def moduleListRaw = sh(
        script: "./gradlew printSubprojectPaths --no-daemon -q",
        returnStdout: true
    ).trim()

    def moduleList = moduleListRaw.readLines()
        .findAll { it.startsWith("SUBPROJECT_PATH:") }
        .collect { it.replace("SUBPROJECT_PATH:", "").trim() }
        .findAll {
            // Filter out modules that do not have a gradle.kts file
            def files = sh(
                    script: "ls -1 ${WORKSPACE}/${it}",
                    returnStdout: true
            ).trim().readLines()
            files?.any { fileName -> fileName.endsWith("gradle.kts") } ?: false
        }

    print("MODULE_LIST: ${moduleList}")
    return new ArrayList<String>(moduleList)
}

/**
 * Gets list of all modules that have unit tests by parsing output from printModulesWithUnitTest task.
 * 
 * @return List of module paths like ["app", "domain", "feature/chat"] that contain unit tests
 */
ArrayList<String> getUnitTestModuleList() {
    def unitTestModuleListRaw = sh(
        script: "./gradlew printModulesWithUnitTest --no-daemon -q",
        returnStdout: true
    ).trim()

    def moduleList = unitTestModuleListRaw.readLines()
        .findAll { it.startsWith("UNIT-TEST-MODULE:") }
        .collect { it.replace("UNIT-TEST-MODULE:", "").trim() }

    print("UNIT_TEST_MODULE_LIST: ${moduleList}")
    return new ArrayList<String>(moduleList)
}


/**
 * Build a Map of build statistics for the current Jenkins build.
 * Reads env vars + currentBuild + params, returns a Map. No I/O, safe to call from any post block.
 *
 * @param agentAcquiredMs   When the agent was acquired and execution started on a slave
 *                          (System.currentTimeMillis() captured at the top of the first agent stage).
 * @param stageDurationsMs  map keyed by 'build_apk_ms', 'unit_test_ms', 'lint_ms'.
 *                          Missing keys are emitted as null (stage never ran or was skipped).
 * @param stageNodeNames    map keyed by 'build_apk', 'unit_test', 'lint' — agent name that ran
 *                          each parallel stage. Missing keys emit null.
 * @param status            currentBuild.currentResult (SUCCESS / FAILURE / UNSTABLE / ABORTED).
 * @param skipped           true if the build was skipped (Draft/WIP MR).
 * @param codeReviewOnly    true if only the Code Review stage ran.
 */
Map collectBuildStats(long agentAcquiredMs, Map stageDurationsMs, Map stageNodeNames,
                      String status, boolean skipped, boolean codeReviewOnly) {
    long endMs = System.currentTimeMillis()
    long scheduledMs = (currentBuild.timeInMillis ?: agentAcquiredMs) as long
    long queueWaitMs = Math.max(0L, agentAcquiredMs - scheduledMs)

    String mrNumber = getMrNumber()
    String mrUrl = env.CHANGE_URL
    if ((mrUrl == null || mrUrl.isEmpty()) && mrNumber != null && !mrNumber.isEmpty()) {
        String homepage = env.gitlabSourceRepoHomepage
        if (homepage != null && !homepage.isEmpty()) {
            mrUrl = "${homepage}/-/merge_requests/${mrNumber}"
        }
    }

    Map stages = [
            build_apk_ms: stageDurationsMs['build_apk_ms'],
            unit_test_ms: stageDurationsMs['unit_test_ms'],
            lint_ms     : stageDurationsMs['lint_ms'],
    ]

    Map nodes = [
            build_apk: stageNodeNames['build_apk'],
            unit_test: stageNodeNames['unit_test'],
            lint     : stageNodeNames['lint'],
    ]

    return [
            schema_version  : 1,
            status          : status?.toLowerCase(),
            skipped         : skipped,
            code_review_only: codeReviewOnly,
            build_number    : env.BUILD_NUMBER,
            build_url       : env.BUILD_URL,
            commit_id       : env.GIT_COMMIT,
            mr_number       : mrNumber,
            mr_url          : mrUrl,
            source_branch   : env.CHANGE_BRANCH ?: env.gitlabSourceBranch,
            target_branch   : env.GITLAB_OA_TARGET_BRANCH ?: env.CHANGE_TARGET,
            author          : env.CHANGE_AUTHOR_DISPLAY_NAME ?: env.CHANGE_AUTHOR ?: env.gitlabUserName,
            trigger_kind    : env.gitlabActionType ?: env.GITLAB_OBJECT_KIND,
            scheduled_ts    : formatBuildStatsTimestamp(scheduledMs),
            build_start_ts  : formatBuildStatsTimestamp(agentAcquiredMs),
            build_end_ts    : formatBuildStatsTimestamp(endMs),
            queue_wait_ms   : queueWaitMs,
            duration_ms     : endMs - agentAcquiredMs,
            total_ms        : endMs - scheduledMs,
            stages          : stages,
            nodes           : nodes,
    ]
}

/**
 * Upload per-build stats as a JSON file to Artifactory for offline CI performance analysis.
 * Path: android-mega/cicd/build-stats/<YYYY>/<MM>/<UTC_TS>-<BUILD>-<MR>.json
 * One file per build — no shared state, no race conditions.
 *
 * Wrapped in try/catch so a stats upload failure NEVER fails the pipeline.
 */
void recordBuildStats(Map stats) {
    try {
        Date now = new Date()
        TimeZone utc = TimeZone.getTimeZone("UTC")
        String utcTs = now.format("yyyyMMdd'T'HHmmss'Z'", utc)
        String monthPath = now.format("yyyy/MM", utc)
        String mrPart = (stats.mr_number ?: 'no-mr').toString()
        String fileName = "${utcTs}-${env.BUILD_NUMBER}-${mrPart}.json"
        String remoteUrl = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/cicd/build-stats/${monthPath}/${fileName}"

        String jsonText = JsonOutput.prettyPrint(JsonOutput.toJson(stats))
        writeFile file: fileName, text: jsonText

        useArtifactory() {
            sh """
                cd ${WORKSPACE}
                curl -f -u ${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${fileName} ${remoteUrl}
            """
        }
        println("[build-stats] uploaded ${remoteUrl}")
    } catch (Exception e) {
        println("[build-stats] upload failed: ${e}")
    }
}

private String formatBuildStatsTimestamp(long ms) {
    return new Date(ms).format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))
}

return this


