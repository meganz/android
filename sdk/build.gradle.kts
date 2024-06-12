import mega.privacy.android.build.isServerBuild
import mega.privacy.android.build.shouldUsePrebuiltSdk
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(plugin.plugins.mega.artifactory.publish.convention)
}

android {
    val compileSdkVersion: Int by rootProject.extra
    compileSdk = compileSdkVersion
    val buildTools: String by rootProject.extra
    buildToolsVersion = buildTools

    defaultConfig {
        val minSdkVersion: Int by rootProject.extra
        minSdk = minSdkVersion

        val targetSdkVersion: Int by rootProject.extra
        targetSdk = targetSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets.getByName("main") {
        java.srcDirs(
            "src/main/jni/mega/sdk/bindings/java",
            "src/main/jni/megachat/sdk/bindings/java"
        )
        java.exclude("**/MegaApiSwing.java")
    }

    compileOptions {
        val javaVersion: JavaVersion by rootProject.extra
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
    namespace = "nz.mega.sdk"
}

dependencies {
    implementation(androidx.exifinterface)
    implementation(files("src/main/jni/megachat/webrtc/libwebrtc.jar"))

    testImplementation(testlib.junit)
    androidTestImplementation(testlib.junit.test.ktx)
    androidTestImplementation(testlib.espresso)

    // temporary fix reference to sdk/src/main/jni/ExoPlayer/library/common/build.gradle
    // due to guava uses old dependencies
    // https://github.com/google/ExoPlayer/blob/release-v2/library/common/build.gradle
    // https://github.com/google/ExoPlayer/blob/release-v2/constants.gradle
    api("com.google.guava:guava:33.0.0-android") {
        exclude(group = "com.google.code.findbugs", module = "jsr305")
        exclude(group = "org.checkerframework", module = "checker-compat-qual")
        exclude(group = "org.checkerframework", module = "checker-qual")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "com.google.j2objc", module = "j2objc-annotations")
        exclude(group = "org.codehaus.mojo", module = "animal-sniffer-annotations")
    }
}

// Logic to publish prebuilt SDK to Artifactory
if (!shouldUsePrebuiltSdk() || isServerBuild()) {
    val sdkCommit = getEnvironmentVariable("SDK_COMMIT")
    val chatCommit = getEnvironmentVariable("CHAT_COMMIT")
    val sdkBuilderName = getEnvironmentVariable("gitlabUserName")
    val sdkBranch = getEnvironmentVariable("SDK_BRANCH")
    val chatBranch = getEnvironmentVariable("MEGACHAT_BRANCH")

    println("SDK_COMMIT = $sdkCommit")
    println("CHAT_COMMIT = $chatCommit")
    println("SDK_BUILDER_NAME = $sdkBuilderName")
    println("SDK_BRANCH = $sdkBranch")
    println("MEGACHAT_BRANCH = $chatBranch")

    megaPublish {
        repoKey = "mega-sdk-android"
        groupId = "nz.mega.sdk"
        artifactId = "sdk"
        version = sdkVersion() + "-" + sdkLibType()
        libPath = "${layout.buildDirectory.get()}/outputs/aar/${project.name}-release.aar"
        sourcePath = "${layout.buildDirectory.get()}/libs/${project.name}-sources.jar"
        properties = mapOf(
            "sdk-commit" to sdkCommit,
            "chat-commit" to chatCommit,
            "sdk-branch" to sdkBranch,
            "chat-branch" to chatBranch,
            "builder" to sdkBuilderName,
        )
        dependentTasks = listOf("assembleRelease", "releaseSourcesJar")
    }
}


/**
 * Generate the SDK version string. This version is used in the published SDK library and
 * is used by library users.
 * Note: the sdk version is in the pattern of yyyyMMdd.HHmmss and is formatted to UTC,
 * not local time.
 *
 * @return SDK lib's version
 */
fun sdkVersion(): String {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss")
    return now.format(formatter)
}

/**
 * Get the SDK lib type, either "rel" or "dev".
 *
 * @return
 */
fun sdkLibType(): String =
    if (System.getenv("SDK_PUBLISH_TYPE") == "rel") "rel" else "dev"

/**
 * Get the environment variable value.
 *
 * @param param the environment variable name
 * @return the environment variable value
 */
fun getEnvironmentVariable(param: String): String =
    System.getenv(param).takeIf { !it.isNullOrEmpty() } ?: "N/A"
