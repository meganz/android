package mega.privacy.android.build

import org.gradle.api.Project
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone

/**
 * This file contains the commit gradle functions that may be used by multiple files.
 */

/**
 * For Continuous Delivery in Google Play Internal Channel <p/>
 * For Android builds in CD pipeline, read the environment variable and assign to version code
 * For local builds, compute string with formatted date and assign to version code
 * @return app's version code
 */
fun readVersionCode(): Int = System.getenv("APK_VERSION_CODE_FOR_CD")
    ?.takeIf { it.isNotBlank() }?.toInt()
    ?: SimpleDateFormat("yyDDDHHmm", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }.format(Date()).toInt()


/**
 * For Continuous Delivery with Firebase AppDistribution <p/>
 * For Android builds in CD pipeline, read the environment variable and
 * add to standard version name, in order to distinguish versions on Firebase.
 * For normal builds, use default version name format.
 * @return app's version name tag
 */
fun readVersionNameTag(): String =
    System.getenv("APK_VERSION_NAME_TAG_FOR_CD").orEmpty().trim()


/**
 * For Continuous Delivery in Google Play Internal Channel <p/>
 * For Android builds in CD pipeline, read the environment variable and
 * add to standard version name, in order to distinguish versions on Internal Channel.
 * For normal builds, use default version name format.
 * @return app's version name channel
 */
fun readVersionNameChannel(): String =
    System.getenv("APK_VERSION_NAME_CHANNEL_FOR_CD").orEmpty().trim()


/**
 * For Continuous Delivery with Firebase AppDistribution <p/>
 * Get release note string from environment variable.
 * @return release note to be shown on Firebase. If environment variable is not set,
 *         return "Release Note not available"
 */
fun readReleaseNotes(): String =
    System.getenv("RELEASE_NOTES_FOR_CD")?.takeIf { it.isNotBlank() }
        ?: "Release Note not available"

/**
 * For Continuous Delivery with Firebase AppDistribution
 * Get tester list from environment. Tester list should be
 * comma-separated list of email address.
 * @return tester list or "" if there is no tester
 */
fun readTesters(): String =
    System.getenv("TESTERS_FOR_CD").orEmpty().trim()

/**
 * For Continuous Delivery with Firebase AppDistribution
 * Get tester group list from environment. Tester group list should be
 * comma-separated list of group names.
 * @return tester group list or default group list if there is no environment setting
 */
fun readTesterGroupList(): String =
    System.getenv("TESTER_GROUP_FOR_CD")?.takeIf { it.isNotBlank() }
        ?: "internal_qa, internal_dev, external_qa, external_dev, internal_design"


/**
 * whether the build is executed on CI server
 */
fun isServerBuild(): Boolean = System.getenv("BUILD_NUMBER") != null

/**
 * whether the build is executed on CI job, not CD or other job.
 */
fun isCiBuild(): Boolean =
    System.getenv("IS_CI_BUILD")?.let { it == "true" } ?: false

/**
 * Check whether we should use combine lint reports into a single file
 *
 * @return true if environment variable "USE_PREBUILT_SDK" is true. Otherwise return false.
 */
fun shouldCombineLintReports(): Boolean =
    System.getenv("COMBINE_LINT_REPORTS")?.let { it == "true" } ?: false


/**
 * Get the Git hash for the app.
 *
 * @return commit ID of the app
 */
fun getAppGitHash(project: Project): String {
    // Read commit ID from local
    val stdout = ByteArrayOutputStream()
    project.exec {
        workingDir = File("./")
        commandLine = listOf("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    val commit = stdout.toString().trim()

    println("app_commit = $commit")
    return commit
}

/**
 * Get flag to activate Greeter developer tool from local.properties file.
 */
fun shouldActivateGreeter(project: Project): Boolean =
    project.rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            Properties()
                .apply { load(FileInputStream(file)) }["activate_greeter"]
                ?.let { it == "true" }
        } ?: false

/**
 * Get flag to activate Nocturn developer tool from local.properties file.
 */
fun shouldActivateNocturn(project: Project): Boolean =
    project.rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            Properties()
                .apply { load(FileInputStream(file)) }["activate_nocturn"]
                ?.let { it == "true" }
        } ?: false


/**
 * Get timeout in millis for Nocturn to decide the app is in ANR state.
 */
fun getNocturnTimeout(project: Project): Long {
    val defaultTimeout = 2000L
    return project.rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            Properties()
                .apply { load(FileInputStream(file)) }["nocturn_timeout"]
                ?.let { (it as String).toLongOrNull() }
        } ?: defaultTimeout
}

/**
 * Get test account user name local.properties file.
 */
fun getTestAccountUserName(project: Project): String? =
    project.rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            Properties()
                .apply { load(FileInputStream(file)) }["test_account_username"]
                ?.let { it as String }
        }

/**
 * Get test account password local.properties file.
 */
fun getTestAccountPassword(project: Project): String? =
    project.rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            Properties()
                .apply { load(FileInputStream(file)) }["test_account_password"]
                ?.let { it as String }
        }

/**
 * Get Karma ADB plugin port.
 */
fun getKarmaPluginPort(project: Project): Int {
    val defaultPort = 6379
    return project.rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            Properties()
                .apply { load(FileInputStream(file)) }["karma_plugin_port"]
                ?.let { (it as String).toIntOrNull() }
        } ?: defaultPort
}

/**
 * Check if the gradle command parameter matches the build type.
 *
 * @param type the type keyword to check. Possible values:
 *      - "qa"  QA build
 *      - "debug" Debug build
 *      - "lint" Lint check build
 *      - "test" Test case build
 * @param taskList – list of gradle parameters, e.g. ["clean", "app:assembleGmsDebug"]
 * @return
 */
fun buildTypeMatches(type: String, taskList: List<String>): Boolean =
    taskList
        .map { it.lowercase(Locale.getDefault()) }
        .filter { it.contains("clean").not() }
        .any { name -> name.contains(type.lowercase(Locale.getDefault())) }

/**
 * Check whether to apply default configuration.
 *
 * @param project
 * @return
 */
fun shouldApplyDefaultConfiguration(project: Project): Boolean =
    !project.gradle.startParameter.taskNames.any { it.contains(":test") && it.contains("UnitTest") }

/**
 * Ensure that the native libs folder exists and return its relative path.
 * @return path of native library symbols. The returned path is relative to root of app module
 */
fun nativeLibsDir(project: Project): String {
    project.exec {
        workingDir = File("../")
        commandLine = listOf("mkdir", "-p", "sdk/src/main/obj/local")
    }
    return "../sdk/src/main/obj/local"
}

/**
 *
 *
 * @param property the property to query.
 *                  possible value: 'sdk-commit', 'chat-commit', 'sdk-branch', 'chat-branch'
 * @param version version of the pre-built SDK. It can be read at the value of "megaSdkVersion" in
 * project build.gradle file.
 * @return property value
 */
fun queryPrebuiltSdkProperty(property: String, version: String, project: Project): String {
    val default = "N/A"
    // Only query SDK property in CI. Skip this step in local build to reduce compile time
    if (isServerBuild()) {
        val stdout = ByteArrayOutputStream()
        System.getenv("ARTIFACTORY_BASE_URL")
        val url =
            "${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/api/storage/mega-gradle/mega-sdk-android/nz/mega/sdk/sdk/${version}/sdk-${version}.aar?properties"
        project.exec {
            workingDir = File(".")
            commandLine = listOf("curl", url)
            standardOutput = stdout
        }
        val response = stdout.toString().trim()
        val jsonObject = JSONObject(response)

        return jsonObject.getJSONObject("properties")?.takeIf { it.has(property) }
            ?.getJSONArray(property)
            ?.getString(0) ?: default
    }
    return default
}

/**
 * Get the Git hash for SDK.
 * If build uses prebuilt SDK, fetch the value from Artifactory.
 * Else, read the value from local SDK folder
 *
 * @param version version of the pre-built SDK.
 * @return commit ID of SDK
 */
fun getSdkGitHash(version: String, project: Project): String {
    val commit: String
    if (shouldUsePrebuiltSdk()) {
        commit = queryPrebuiltSdkProperty(
            property = "sdk-commit",
            version = version,
            project = project
        )
    } else {
        // Read commit ID from local SDK folder
        val stdout = ByteArrayOutputStream()
        project.exec {
            workingDir = File("../sdk/src/main/jni/mega/sdk")
            commandLine = listOf("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        commit = stdout.toString().trim()
    }

    println("sdk_commit = $commit")
    return commit
}


/**
 * Get the Git hash for MEGAchat SDK.
 * If build uses prebuilt SDK, fetch the value from Artifactory.
 * Else, read the value from local MEGAchat SDK folder
 *
 * @param version version of the pre-built SDK.
 * @return commit ID of MEGAchat SDK
 */
fun getChatGitHash(version: String, project: Project): String {
    val commit: String
    if (shouldUsePrebuiltSdk()) {
        commit = queryPrebuiltSdkProperty("chat-commit", version, project)
    } else {
        // Read commit ID from local SDK folder
        val stdout = ByteArrayOutputStream()
        project.exec {
            workingDir = File("../sdk/src/main/jni/megachat/sdk")
            commandLine = listOf("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        commit = stdout.toString().trim()
    }

    println("chat_commit = $commit")
    return commit
}

/**
 * Check whether we should use prebuilt SDK or local sdk module
 *
 * @return false if environment variable "USE_PREBUILT_SDK" is false. Otherwise return true.
 */
fun shouldUsePrebuiltSdk(): Boolean =
    System.getenv("USE_PREBUILT_SDK")?.let { it != "false" } ?: true