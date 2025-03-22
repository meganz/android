/**
 * This script is to promote Alpha builds to Beta or Production in Google Play Store
 */


import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials
import hudson.model.Item
import org.jenkinsci.plugins.googleplayandroidpublisher.CredentialsHandler
import org.jenkinsci.plugins.googleplayandroidpublisher.Util

@Library('jenkins-android-shared-lib') _
/**
 * common.groovy file with common methods
 */
def common

/**
 * To store the current build step
 */
BUILD_STEP = ''

properties([
        parameters([
                activeChoice(
                        choiceType: 'PT_SINGLE_SELECT',
                        description: 'Application',
                        filterLength: 1,
                        filterable: false,
                        name: 'PACKAGE',
                        randomName: 'choice-parameter-162835245454545',
                        script: groovyScript(
                                fallbackScript: [classpath: [], oldScript: '', sandbox: false, script: ''],
                                script: [classpath: [], oldScript: '', sandbox: false, script: 'return ["Select", "MEGA", "MEGA VPN", "MEGA Pass"]']
                        )
                ),
                activeChoice(
                        choiceType: 'PT_SINGLE_SELECT',
                        description: 'Target track/channel to promote to',
                        filterLength: 1,
                        filterable: false,
                        name: 'TRACK',
                        randomName: 'choice-parameter-162835218411553',
                        script: groovyScript(
                                fallbackScript: [classpath: [], oldScript: '', sandbox: false, script: ''],
                                script: [classpath: [], oldScript: '', sandbox: false, script: 'return ["Select", "Beta", "Production"]']
                        )
                ),
                reactiveChoice(
                        choiceType: 'PT_SINGLE_SELECT',
                        description: 'Rollout percentage or halt',
                        filterLength: 1,
                        filterable: false,
                        name: 'PERCENTAGE',
                        randomName: 'choice-parameter-162835226979567',
                        referencedParameters: '',
                        script: groovyScript(
                                fallbackScript: [classpath: [], oldScript: '', sandbox: false, script: ''],
                                script: [classpath: [], oldScript: '', sandbox: false, script: '''
                                    return ["Select":"Select", "0":"0%", "25":"25%", "50":"50%", "100":"100%", "Halt Rollout":"Halt Rollout"]
                                ''']
                        )
                ),
                reactiveChoice(
                        choiceType: 'PT_SINGLE_SELECT',
                        description: 'Version of the build to be promoted',
                        name: 'VERSION',
                        filterLength: 1,
                        filterable: false,
                        randomName: 'choice-parameter-162835228262015',
                        referencedParameters: 'PACKAGE',
                        script: groovyScript(
                                fallbackScript: [classpath: [], oldScript: '', sandbox: false, script: ''],
                                script: [classpath: [], oldScript: '', sandbox: false, script: '''
                                    // Groovy script needs to be written in plain text due to the Active Choices plugin limitation,
                                    // Any script changes needs to be approved in Jenkins > Android-Promote > Configure
                                    
                                    import com.google.api.services.androidpublisher.AndroidPublisher
                                    import com.google.api.services.androidpublisher.model.Track
                                    import com.google.api.services.androidpublisher.model.TrackRelease
                                    import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials
                                    import org.jenkinsci.plugins.googleplayandroidpublisher.Util
                                    import org.jenkinsci.plugins.googleplayandroidpublisher.CredentialsHandler
                                    import hudson.model.Item;
                                    import com.google.api.services.androidpublisher.model.TrackRelease
                                    import jenkins.model.Jenkins

                                    def googleCredentialsId = 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL'
                                    def packageName
                                    if (PACKAGE == "MEGA VPN") {
                                        packageName = "mega.vpn.android.app"
                                    } else if (PACKAGE == "MEGA Pass") {
                                        packageName = "mega.pwm.android.app"
                                    } else {
                                        packageName = "mega.privacy.android.app"
                                    }
                                  
                                    
                                    // Get credentials
                                    Item currentItem = Jenkins.instance.getItemByFullName("Android-Promote")
                                    GoogleRobotCredentials credentials = CredentialsHandler.getById(googleCredentialsId, currentItem)
                                    def publisher = Util.getPublisherClient(credentials, "4.2")
                                
                                    // Create a new edit session
                                    AndroidPublisher.Edits edits = publisher.edits()
                                    def edit = edits.insert(packageName, null).execute()
                                    String editId = edit.getId()
                                
                                    // Get versions from all track
                                    List<String> trackNames = ["alpha", "beta", "production"]
                                    List<String> versions = []
                                    versions.add("Select")
                                    trackNames.each { name ->
                                        // Get the track details for the current track
                                        def track = edits.tracks().get(packageName, editId, name).execute()
                                        track.getReleases().each { release ->
                                            def status = release.getStatus()
                                            def isFullRolloutInProduction = status == "completed" && name == "production"
                                            if(!isFullRolloutInProduction) {
                                                def userFraction = release.getUserFraction()
                                                def rolloutInfo = ""
                                                if (status == "inProgress") {
                                                    rolloutInfo = "at ${userFraction * 100 as int}% rollout"
                                                } else if (status == "completed") {
                                                    rolloutInfo = "at 100% rollout"
                                                } else if (status == "halted") {
                                                    rolloutInfo = "at halted status)"
                                                } 
                                                // Remove hash from the version name if exists, e.g. 14.1(242330236)(a476f33326) to 14.1(242330236)
                                                String versionName = release.getName().replaceFirst(/([^()]+\\([^)]*\\)).*/, { fullMatch, versionPart ->
                                                    return "${versionPart} - Currently in ${name} ${rolloutInfo}"
                                                })
                                                versions.add(versionName)
                                            }
                                        }
                                    }
                                    edits.delete(packageName, editId).execute();
                                    return versions
                                    ''']
                        )
                ),
                activeChoiceHtml(
                        choiceType: 'ET_FORMATTED_HIDDEN_HTML',
                        name: 'Override CSS',
                        omitValueField: false,
                        randomName: 'choice-parameter-162835235332942',
                        referencedParameters: '',
                        script: groovyScript(
                                fallbackScript: [classpath: [], oldScript: '', sandbox: false, script: ''],
                                script: [classpath: [], oldScript: '', sandbox: false, script: '''return """<style>.active-choice select{-webkit-appearance:none;-moz-appearance:none;appearance:none;border:2px solid var(--input-border);border-radius:var(--form-input-border-radius);box-shadow:0 0 0 10px transparent;display:block;max-width:100%!important;min-height:38px;padding:var(--form-input-padding);transition:var(--standard-transition);width:100%!important}.active-choice{position:relative;width:100%}.active-choice:after{background-color:currentColor;bottom:0;content:"";-webkit-mask-image:url("data:image/svg+xml;charset=UTF-8,%3c?xml version='1.0' encoding='UTF-8'?%3e%3csvg width='336px' height='192px' viewBox='0 0 336 192' version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'%3e%3ctitle%3ePath%3c/title%3e%3cg id='Page-1' stroke='none' stroke-width='1' fill='none' fill-rule='evenodd'%3e%3cg id='arrow' transform='translate(0.000000, 0.000000)' fill='%23FF0000' fill-rule='nonzero'%3e%3cpath d='M7.02943725,7.02943725 C16.3053957,-2.24652118 31.2852799,-2.34214962 40.6788451,6.74255194 L40.9705627,7.02943725 L168,134.059 L295.029437,7.02943725 C304.305396,-2.24652118 319.28528,-2.34214962 328.678845,6.74255194 L328.970563,7.02943725 C338.246521,16.3053957 338.34215,31.2852799 329.257448,40.6788451 L328.970563,40.9705627 L184.970563,184.970563 C175.694604,194.246521 160.71472,194.34215 151.321155,185.257448 L151.029437,184.970563 L7.02943725,40.9705627 C-2.34314575,31.5979797 -2.34314575,16.4020203 7.02943725,7.02943725 Z' id='Path'%3e%3c/path%3e%3c/g%3e%3c/g%3e%3c/svg%3e");mask-image:url("data:image/svg+xml;charset=UTF-8,%3c?xml version='1.0' encoding='UTF-8'?%3e%3csvg width='336px' height='192px' viewBox='0 0 336 192' version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'%3e%3ctitle%3ePath%3c/title%3e%3cg id='Page-1' stroke='none' stroke-width='1' fill='none' fill-rule='evenodd'%3e%3cg id='arrow' transform='translate(0.000000, 0.000000)' fill='%23FF0000' fill-rule='nonzero'%3e%3cpath d='M7.02943725,7.02943725 C16.3053957,-2.24652118 31.2852799,-2.34214962 40.6788451,6.74255194 L40.9705627,7.02943725 L168,134.059 L295.029437,7.02943725 C304.305396,-2.24652118 319.28528,-2.34214962 328.678845,6.74255194 L328.970563,7.02943725 C338.246521,16.3053957 338.34215,31.2852799 329.257448,40.6788451 L328.970563,40.9705627 L184.970563,184.970563 C175.694604,194.246521 160.71472,194.34215 151.321155,185.257448 L151.029437,184.970563 L7.02943725,40.9705627 C-2.34314575,31.5979797 -2.34314575,16.4020203 7.02943725,7.02943725 Z' id='Path'%3e%3c/path%3e%3c/g%3e%3c/g%3e%3c/svg%3e");-webkit-mask-position:center;mask-position:center;-webkit-mask-repeat:no-repeat;mask-repeat:no-repeat;-webkit-mask-size:contain;mask-size:contain;pointer-events:none;position:absolute;right:13px;top:0;width:12px}</style>"""''']
                        )
                )
        ]),
])

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

    post {
        failure {
            script {
                // Don't send message for validation errors
                if (BUILD_STEP != 'Validation') {
                    common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                    String jenkinsLog = common.uploadFileToArtifactory("android_promote", CONSOLE_LOG_FILE)
                    String buildLog = "Build Log: <${jenkinsLog}|${CONSOLE_LOG_FILE}>"
                    String track = params.TRACK.toLowerCase()
                    String versionName = extractVersionName(params.VERSION)
                    String versionCode = extractVersionCode(params.VERSION)
                    String fullVersion = "v$versionName($versionCode)"
                    String percentage = params.PERCENTAGE
                    String app = params.PACKAGE
                    String message = "${buildLog}\n"
                    if (isHaltRollout(percentage)) {
                        message += "Hi <!subteam^S02B2PB5SG7>, halting the $track rollout for $app $fullVersion has failed, please check."
                    } else {
                        message += "Hi <!subteam^S02B2PB5SG7>, the promotion to $track has failed for $app $fullVersion at ${percentage}% rollout, please check."
                    }

                    // Limit to MEGA app for now because success message is not supported yet for VPN and PWM
                    if (app == "MEGA") {
                        def slackChannelId = common.fetchSlackChannelIdsByReleaseVersion(versionName)[0]
                        if (slackChannelId == "") {
                            slackSend(channel: "android", color: "danger", message: message)
                        } else {
                            slackSend channel: slackChannelId, color: "danger", message: message, replyBroadcast: true
                        }
                    }
                }
            }
        }

        success {
            script {
                String track = params.TRACK.toLowerCase()
                String versionName = extractVersionName(params.VERSION)
                String versionCode = extractVersionCode(params.VERSION)
                String fullVersion = "v$versionName($versionCode)"
                String percentage = params.PERCENTAGE
                String app = params.PACKAGE
                String packageLink = ""
                withCredentials([string(credentialsId: 'JIRA_BASE_URL', variable: 'JIRA_BASE_URL')]) {
                    if (app == "MEGA VPN") {
                        packageLink = "${env.JIRA_BASE_URL}/issues?jql=project%20%3D%20VPN%20AND%20fixVersion%20%3D%20%22VPN%20Android%20$versionName%22%20%20ORDER%20BY%20priority%20DESC%2C%20updated%20DESC"
                    } else if (app == "MEGA Pass") {
                        packageLink = "${env.JIRA_BASE_URL}/issues?jql=project%20%3D%20PASS%20AND%20fixVersion%20%3D%20%22PWM%20Android%20$versionName%22%20%20ORDER%20BY%20priority%20DESC%2C%20updated%20DESC"
                    } else {
                        packageLink = "${env.JIRA_BASE_URL}/issues/?jql=fixVersion%20%3D%20%22Android%20$versionName%22%20%20ORDER%20BY%20priority%20DESC%2C%20updated%20DESC"
                    }
                }
                String userType = track == 'production' ? "users" : "testers"

                String message = ""
                if (isHaltRollout(percentage)) {
                    message = "The Android team has halted the $track roll out process of the $app ($fullVersion) on Google Play."
                } else {
                    message = "The Android team has started the $track roll out process in Google Play for $app ($fullVersion). It is now being offered to ${percentage}% of $track $userType.\n" +
                            "Here is the <$packageLink|v$versionName> Jira Release Package."
                }

                // Limit to MEGA app for now because success message is not supported yet for VPN and PWM
                if (app == "MEGA") {
                    def slackChannelId = common.fetchSlackChannelIdsByReleaseVersion(versionName)[0]
                    if (slackChannelId == "") {
                        slackSend(channel: "android", message: message)
                    } else {
                        slackSend channel: slackChannelId, message: message, replyBroadcast: true
                    }
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

                    def googleCredentialsId = 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL'
                    def packageName = getPackageName(params.PACKAGE)
                    def versionCode = extractVersionCode(params.VERSION)
                    def trackName = params.TRACK.toLowerCase()
                    def rolloutPercentage = params.PERCENTAGE
                    if (isHaltRollout(rolloutPercentage)) {
                        haltRollout(
                                googleCredentialsId,
                                packageName,
                                trackName,
                                versionCode
                        )
                    } else {
                        def versionCodes = params.PACKAGE == 'MEGA' ? "233140859, $versionCode" : "$versionCode"
                        def releaseName = extractReleaseName(params.VERSION)

                        androidApkMove googleCredentialsId: "$googleCredentialsId",
                                applicationId: "$packageName",
                                trackName: "$trackName",
                                rolloutPercentage: "$rolloutPercentage",
                                fromVersionCode: true,
                                releaseName: "$releaseName",
                                versionCodes: "$versionCodes"
                    }
                }
            }
        }
    }
}

private static def isHaltRollout(percentage) {
    return percentage == 'Halt Rollout'
}

private def haltRollout(googleCredentialsId, packageName, trackName, versionCode) {
    // Generate GoogleRobotCredentials credentials with scope
    Item item = currentBuild.rawBuild.parent
    GoogleRobotCredentials credentials = CredentialsHandler.getById(googleCredentialsId, item)

    // Use Google Play Android Publisher Jenkins plugin's util method to create a publisher client
    def publisher = Util.getPublisherClient(credentials, "4.2")

    // Create a new edit session
    AndroidPublisher.Edits edits = publisher.edits()
    def edit = edits.insert(packageName, null).execute()
    String editId = edit.getId()

    // Prepare the halted release
    List<Long> apkVersionCodes = []
    apkVersionCodes.add(233140859) // Additional version
    apkVersionCodes.add(versionCode)

    // Update the track with the halted release
    def release = new TrackRelease()
            .setStatus("halted")
            .setVersionCodes(apkVersionCodes)
            .setUserFraction(0.0d)

    // Create a track object
    def track = new Track()
            .setTrack(trackName)
            .setReleases(Collections.singletonList(release));

    edits.tracks().update(packageName, editId, trackName, track).execute()

    // Commit the changes
    edits.commit(packageName, editId).execute()

    echo "Staged rollout halted for track: $trackName for version code: $versionCode"
}

/**
 * Validate pipeline parameters
 * @param params
 */
def validateParams(params) {
    if (!params.PACKAGE) {
        error("'PACKAGE' parameter is missing. Please specify the application.")
    } else if (params.PACKAGE == 'Select') {
        error("'PACKAGE' was not selected. Please choose a valid application (e.g MEGA, MEGA VPN, MEGA Pass).")
    }

    if (!params.TRACK) {
        error("'TRACK' parameter is missing. Please specify the target track.")
    } else if (params.TRACK == 'Select') {
        error("'TRACK' was not selected. Please choose a valid target track (e.g., Beta or Production).")
    }

    if (!params.PERCENTAGE) {
        error("'PERCENTAGE' parameter is missing. Please specify the rollout percentage.")
    } else if (params.PERCENTAGE == 'Select') {
        error("'PERCENTAGE' parameter was not selected. Please choose a valid rollout percentage (e.g., 25, 50, 100, or Halt Rollout).")
    }

    if (!params.VERSION || params.VERSION.trim().isEmpty()) {
        error("'VERSION' parameter is missing. Please specify the version of the build (e.g., 14.1(242330236)).")
    } else if (params.VERSION == 'Select') {
        error("'VERSION' parameter was not selected. Please choose a version (e.g., 14.1(242330236)).")
    } else if (!isValidVersion(params.VERSION)) {
        error("Invalid 'VERSION' parameter was selected. Please choose a valid version (e.g., 14.1(242330236)). Selected value: '${params.VERSION}'.")
    }
}

/**
 * Validate release version
 * @param version
 * @return
 */
private static def isValidVersion(String version) {
    def pattern = ~/^\d+\.\d+(\.\d+)?\(\d+\)(.+)$/
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
 * Extract release name
 * @param input eg. 14.1(242330236) - Currently in alpha at 100% rollout
 * @return release name eg. 14.1(242330236)
 *
 * Explanation:
 * (\d+\.\d+) → Captures the version name, e.g., "2.4".
 * (?:\.\d+)? → Optionally captures the patch version, e.g., "2.4.1".
 * \( → Matches the literal opening parenthesis (.
 * (\d+) → Captures the version code, e.g., "250510417".
 * \) → Matches the literal closing parenthesis ).
 */
private static def extractReleaseName(String input) {
    def matcher = input =~ /(\d+\.\d+(?:\.\d+)?)\((\d+)\)/
    if (matcher.find()) {
        return "${matcher.group(1)}(${matcher.group(2)})"
    }
    return null
}

private static def getPackageName(String name) {
    if (name == "MEGA VPN") {
        return "mega.vpn.android.app"
    } else if (name == "MEGA Pass") {
        return "mega.pwm.android.app"
    } else {
        return "mega.privacy.android.app"
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