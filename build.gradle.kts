
apply(from = "tools/util.gradle")
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        jcenter()
        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/megagradle")
        }
    }
    dependencies {
        classpath(plugin.build.tools)
        classpath(plugin.kotlin.gradle)
        classpath(plugin.navigation.safeargs)
        classpath(plugin.google.services)
        classpath(plugin.hilt.android)
        classpath(plugin.firebase.crashlytics)
        classpath(plugin.firebase.performance)
        classpath(plugin.firebase.app.distribution)
        classpath(plugin.jacoco)
        classpath(plugin.paparazzi)
        classpath(plugin.jfrog)
        classpath(plugin.junit5)
        classpath(plugin.kotlin.gradle)
        classpath("androidx.benchmark:benchmark-baseline-profile-gradle-plugin:1.2.0-beta02")
        classpath("mega.privacy:megagradle:0.1")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/mega-sdk-android")
        }
        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/mobile-analytics")
        }
        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/dev-tools")
        }
    }
    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
    }
    apply(plugin = "com.jfrog.artifactory")
    apply(plugin = "maven-publish")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}


// Define versions in a single place
// App
extra["appVersion"] = "9.1"

// Sdk and tools
extra["compileSdkVersion"] = 34
extra["minSdkVersion"] = 24
extra["targetSdkVersion"] = 34
extra["buildTools"] = "34.0.0"

// Prebuilt MEGA SDK version
extra["megaSdkVersion"] = "20230905.051909-dev"

//JDK and Java Version
extra["jdk"] = "17"
extra["javaVersion"] = JavaVersion.VERSION_17

/**
 * Checks if it is CI Build
 */
val isServerBuild: groovy.lang.Closure<Boolean> by ext

/**
 * Checks whether to use Prebuilt Sdk
 */
val shouldUsePrebuiltSdk: groovy.lang.Closure<Boolean> by ext

/**
 * Checks whether to Suppress Warnings
 */
val shouldSuppressWarnings by extra(
    fun(): Boolean = isServerBuild() && System.getenv("DO_NOT_SUPPRESS_WARNINGS") != "true"
)

if (!shouldUsePrebuiltSdk() || isServerBuild()) {
    apply(from = "${project.rootDir}/tools/prebuilt-sdk.gradle")
}

tasks.register<mega.privacy.megagradle.PreReleaseTask>("preRelease")
tasks.register<mega.privacy.megagradle.ReleaseTask>("release")
