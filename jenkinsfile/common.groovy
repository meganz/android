/**
 * This file is a common module that hosts methods that are called by different CI/CD scripts.
 */


import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput

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

    def mrNumber = getMrNumberInCD()
    if (mrNumber == null) {
        mrNumber = getMrNumberInCI()
    }

    if (mrNumber != null && !mrNumber.isEmpty()) {
        withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
            env.MARKDOWN_LINK = message
            env.MERGE_REQUEST_URL = "${env.GITLAB_BASE_URL}/api/v4/projects/199/merge_requests/${mrNumber}/notes"
            sh 'curl --request POST --header PRIVATE-TOKEN:$TOKEN --form body=\"${MARKDOWN_LINK}\" ${MERGE_REQUEST_URL}'
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
    String version = sh(script: "./gradlew -q printPrebuildSdkVersion  | tail -n 1", returnStdout: true).trim()
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
        final String response = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${fileName} ${env.GITLAB_BASE_URL}/api/v4/projects/199/uploads", returnStdout: true).trim()
        link = new groovy.json.JsonSlurperClassic().parseText(response).markdown
        return link
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
 * @return The success message to be sent
 */
String releaseFailureMessage(String lineBreak) {
    String message = ":x: Android Release Failed!" +
            "${lineBreak}Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
    if (env.gitlabActionType == "PUSH") {
        message += "${lineBreak}Trigger Reason: git PUSH"
    } else if (env.gitlabActionType == "NOTE") {
        message += "${lineBreak}Trigger Reason: MR comment (${gitlabTriggerPhrase})"
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
            "${lineBreak}Version:\t${readAppVersion1()}" +
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
    String versionName = sh(script: "./gradlew -q printAppVersionName  | tail -n 1", returnStdout: true).trim()
    String versionNameChannel = sh(script: "./gradlew -q printAppVersionNameChannel | tail -n 1", returnStdout: true).trim()
    String appGitHash = sh(script: "./gradlew -q printAppGitHash | tail -n 1", returnStdout: true).trim()
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
        ./gradlew clean
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
                cd ${WORKSPACE}/jenkinsfile/
                cat $releaseNoteFile
                """,
            returnStdout: true).trim()
    return release_notes
}

/**
 * Get the SDK branch name for report. If build is specified by tag
 *
 * @return If SDK is specified by <code>SDK_BRANCH</code>, return the branch name. If SDK is specified
 *         by <code>SDK_TAG</code>, return the tag name.
 */
String sdkBranchName() {
    if (isDefined(SDK_TAG)) {
        return SDK_TAG
    } else {
        return SDK_BRANCH
    }
}

/**
 * Get the MEGAChat SDK branch name for report. If build is specified by tag
 *
 * @return If SDK is specified by <code>MEGACHAT_BRANCH</code>, return the branch name. If SDK is specified
 *         by <code>MEGACHAT_TAG</code>, return the tag name.
 */
String megaChatBranchName() {
    if (isDefined(MEGACHAT_TAG)) {
        return MEGACHAT_TAG
    } else {
        return MEGACHAT_BRANCH
    }
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
        unzip ${ARCHIVE_FOLDER}/${NATIVE_SYMBOLS_FILE} -d ${targetObjLocalLocation}
    """
}


void downloadDependencyLibForSdk() {
    gitlabCommitStatus(name: 'Download Dependency Lib for SDK') {
        sh """
            # we still have to download webrtc file for lint check. :( 
            cd "${WORKSPACE}/jenkinsfile/"
            bash download_webrtc.sh

            mkdir -p "${BUILD_LIB_DOWNLOAD_FOLDER}"
            cd "${BUILD_LIB_DOWNLOAD_FOLDER}"

            pwd 
            ls -lh
        """

        println("applying default google map api config... ")
        withCredentials([
                file(credentialsId: 'ANDROID_DEFAULT_GOOGLE_MAPS_API_FILE_DEBUG', variable: 'ANDROID_DEFAULT_GOOGLE_MAPS_API_FILE_DEBUG')
        ]) {
            String googleMapsApiFolder = "default_google_maps_api_unzipped"

            sh """
                cd ${WORKSPACE}
                unzip ${ANDROID_DEFAULT_GOOGLE_MAPS_API_FILE_DEBUG} -d ${googleMapsApiFolder}
                
                mkdir -p app/src/debug/res/values
                mkdir -p app/src/release/res/values
                cp -fv ${googleMapsApiFolder}/debug/res/values/google_maps_api.xml app/src/debug/res/values/google_maps_api.xml
                cp -fv ${googleMapsApiFolder}/release/res/values/google_maps_api.xml app/src/release/res/values/google_maps_api.xml
            """
        }
    }
}

return this
