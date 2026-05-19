@Library('jenkins-android-shared-lib') _

BUILD_STEP = ""

ASK_AI_CMD = "@ai"
ASK_AI_OUTPUT_FILE = "ask_ai_output.md"
ASK_AI_ERROR_REPORT_FILE = "ask_ai_error_report.txt"

NODE_LABELS = 'mac-jenkins-slave-android'

def common

pipeline {
    agent { label NODE_LABELS }
    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
        timeout(time: 15, unit: 'MINUTES')
        gitLabConnection('GitLabConnection')
    }
    environment {
        LC_ALL = "en_US.UTF-8"
        LANG = "en_US.UTF-8"
        JAVA_HOME = "/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx"
        PATH = "/opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:$PATH"
        CONSOLE_LOG_FILE = "console.txt"
        IS_CI_BUILD = 'true'
    }
    post {
        failure {
            script {
                common = load('jenkinsfile/common.groovy')

                String errorMessage = 'Unknown error'
                if (fileExists(ASK_AI_ERROR_REPORT_FILE)) {
                    errorMessage = readFile(ASK_AI_ERROR_REPORT_FILE).trim()
                }

                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                String folder = "android-ask-ai/MR-${common.getMrNumber()}"
                String jenkinsLog = common.uploadFileToArtifactory(folder, CONSOLE_LOG_FILE)

                String failMsg = ":x: **${ASK_AI_CMD} failed** (Build: ${env.BUILD_NUMBER})<br/>" +
                        "Stage: ${BUILD_STEP}<br/>" +
                        "Error: ${errorMessage}<br/>" +
                        "[Build log](${jenkinsLog})"
                common.sendToMR(failMsg)

                // Mirror the failure to Slack so the team is alerted even when
                // the MR post fails (token rotated, project access lost, etc.).
                slackSend color: 'danger', message: failMsg.replace('<br/>', '\n')
            }
        }
        cleanup {
            cleanWs(cleanWhenFailure: true)
        }
    }
    stages {
        stage('Load Common Script') {
            steps {
                script {
                    BUILD_STEP = 'Preparation'
                    common = load('jenkinsfile/common.groovy')
                }
            }
        }

        stage('Ask AI') {
            when {
                expression { shouldRunAskAi() }
            }
            steps {
                gitlabCommitStatus(name: 'Ask AI') {
                    script {
                        BUILD_STEP = 'Ask AI'

                        def skillFile = "${WORKSPACE}/.claude/skills/ask-ai/SKILL.md"
                        def mrUrl = resolveMrUrl()
                        // Pass the comment verbatim; the model understands that @ai
                        // refers to itself (see SKILL.md). Preserves capitalization,
                        // punctuation, and the reviewer's natural phrasing.
                        def question = (env.gitlabTriggerPhrase ?: '').trim()

                        if (!mrUrl) {
                            error("${ASK_AI_CMD}: missing MR URL, gitlabSourceRepoHomepage and gitlabMergeRequestIid must be set by the GitLab webhook")
                        }
                        if (!question) {
                            error("${ASK_AI_CMD}: empty comment body from gitlabTriggerPhrase")
                        }
                        if (!fileExists(skillFile)) {
                            error("${ASK_AI_CMD}: skill file not found at ${skillFile}")
                        }

                        echo "${ASK_AI_CMD} MR: ${mrUrl}"
                        echo "${ASK_AI_CMD} Q:  ${question}"

                        util.useGitLab() {
                            withCredentials([
                                    string(credentialsId: 'ANTHROPIC_API_KEY', variable: 'ANTHROPIC_API_KEY'),
                                    usernamePassword(
                                            credentialsId: 'Gitlab-Access-Token',
                                            usernameVariable: 'GITLAB_USERNAME',
                                            passwordVariable: 'GITLAB_TOKEN',
                                    ),
                            ]) {
                                // The GitLab Jenkins Plugin's Note Hook does not expose the
                                // discussion ID, so look it up via REST API by matching the
                                // trigger phrase against the MR's discussions.
                                def discussionId = lookupDiscussionIdViaApi(env.gitlabMergeRequestIid)
                                if (!discussionId) {
                                    error("${ASK_AI_CMD}: could not resolve discussion ID via GitLab API lookup")
                                }
                                echo "${ASK_AI_CMD} discussion: ${discussionId}"

                                // Pass the question via env so shell metacharacters in it
                                // can't break the gradle command line.
                                withEnv(["ASK_AI_QUESTION=${question}"]) {
                                    sh "./gradlew --no-daemon askAi " +
                                            "--skill '${skillFile}' " +
                                            "--mr-url '${mrUrl}' " +
                                            "--discussion-id '${discussionId}' " +
                                            "--question \"\$ASK_AI_QUESTION\" " +
                                            "--model 'claude-sonnet-4-6' " +
                                            "--output '${ASK_AI_OUTPUT_FILE}' " +
                                            "--error-report '${ASK_AI_ERROR_REPORT_FILE}'"
                                }

                                echo "${ASK_AI_CMD} reply posted to discussion ${discussionId}"
                            }
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "${ASK_AI_OUTPUT_FILE},${ASK_AI_ERROR_REPORT_FILE}", allowEmptyArchive: true
                }
            }
        }
    }
}

/**
 * Run the Ask AI stage when this build was triggered by an MR comment whose
 * body starts with @ai.
 */
def shouldRunAskAi() {
    boolean isNote = env.gitlabActionType == "NOTE" || env.GITLAB_OBJECT_KIND == "note"
    if (!isNote) {
        echo "shouldRunAskAi: skipping, not a NOTE event (gitlabActionType=${env.gitlabActionType}, GITLAB_OBJECT_KIND=${env.GITLAB_OBJECT_KIND})"
        return false
    }
    String body = env.gitlabTriggerPhrase
    if (!hasAiMention(body)) {
        echo "shouldRunAskAi: skipping, comment does not contain a ${ASK_AI_CMD} mention: '${(body ?: '').take(120)}'"
        return false
    }
    return true
}

/**
 * True when the body contains a standalone `@ai` mention. Case-insensitive.
 * The lookbehind blocks false positives like `email@ai.com`; the word
 * boundary after `ai` blocks `@aim`.
 */
def hasAiMention(String body) {
    return body != null && (body =~ /(?i)(?<!\w)@ai\b/)
}

/**
 * Compose the MR URL from the GitLab Plugin's `gitlabSourceRepoHomepage`
 * (repo URL) plus `gitlabMergeRequestIid`.
 */
def resolveMrUrl() {
    String homepage = env.gitlabSourceRepoHomepage
    String iid = env.gitlabMergeRequestIid
    return (homepage && iid) ? "${homepage}/-/merge_requests/${iid}" : null
}

/**
 * Look up the discussion ID of the @ai comment by matching against the MR's
 * discussions via the GitLab REST API. Must be called inside a
 * withCredentials block exposing GITLAB_TOKEN. Hardcodes project ID 199 to
 * match the convention used by common.groovy.
 *
 * Walks newest-first and prefers an exact body match against the trigger
 * phrase; falls back to an @ai-prefix match.
 *
 * @return the discussion ID, or null if no matching discussion was found.
 */
def lookupDiscussionIdViaApi(String mrIid) {
    if (!mrIid) {
        echo "lookupDiscussionIdViaApi: no MR IID, skipping"
        return null
    }

    env.DISCUSSION_LOOKUP_URL = "${env.GITLAB_BASE_URL}/api/v4/projects/199/merge_requests/${mrIid}/discussions?per_page=50"
    String response = sh(
            script: 'curl -s --header "PRIVATE-TOKEN: $GITLAB_TOKEN" "$DISCUSSION_LOOKUP_URL"',
            returnStdout: true,
    ).trim()

    def discussions
    try {
        discussions = new groovy.json.JsonSlurperClassic().parseText(response)
    } catch (Exception e) {
        echo "lookupDiscussionIdViaApi: failed to parse API response: ${e.message}"
        return null
    }

    def reversed = discussions.reverse()
    String trigger = (env.gitlabTriggerPhrase ?: '').trim()

    if (trigger) {
        for (def disc : reversed) {
            for (def note : (disc.notes ?: [])) {
                if (((note.body ?: '') as String).trim() == trigger) {
                    return disc.id
                }
            }
        }
    }

    for (def disc : reversed) {
        for (def note : (disc.notes ?: [])) {
            if (((note.body ?: '') as String) =~ /(?i)(?<!\w)@ai\b/) {
                return disc.id
            }
        }
    }

    echo "lookupDiscussionIdViaApi: no discussion matched @ai on MR ${mrIid}"
    return null
}

// -----------------------------------------------------------------------------
// Setup checklist (one-time, not executed):
//
//   1. Jenkins job → GitLab Plugin trigger:
//        ✓ "Comments" enabled
//        ✓ trigger phrase regex: (.*@ai .*)
//          (matches @ai anywhere in the comment; kept in sync with the
//          @ai detection in hasAiMention() above)
//
//   2. GitLab project → Settings → Webhooks: tick "Comments" only, point at
//      the Jenkins job's webhook URL.
//
//   3. Bot user: create a GitLab user named `ai`, give it project access,
//      and bind its PAT (api scope) to the Gitlab-Access-Token credential.
//      Replies will appear under that user, matching the @ai mention.
//
//   4. Loopback prevention: shouldRunAskAi() already short-circuits because
//      the bot's reply does not start with @ai. As an extra safety net, you
//      can exclude the `ai` user in the GitLab Plugin's trigger config.
// -----------------------------------------------------------------------------
