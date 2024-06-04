import mega.privacy.android.build.isServerBuild
import mega.privacy.android.build.shouldUsePrebuiltSdk

plugins {
    alias(plugin.plugins.ksp) apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("mega.android.release") version lib.versions.megagradle.get()
    id("mega.android.cicd") version lib.versions.megagradle.get()
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        jcenter()
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
        classpath("androidx.benchmark:benchmark-baseline-profile-gradle-plugin:1.2.3")
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
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/core-ui")
        }
        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/dev-tools")
        }
        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/karma")
        }
    }
    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
    }
    apply(plugin = "com.jfrog.artifactory")
    apply(plugin = "maven-publish")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory.get())
}


// Define versions in a single place
// App
extra["appVersion"] = "13.3"

// Sdk and tools
extra["compileSdkVersion"] = 34
extra["minSdkVersion"] = 26
extra["targetSdkVersion"] = 34
extra["buildTools"] = "34.0.0"

// Prebuilt MEGA SDK version
extra["megaSdkVersion"] = "20240604.113356-rel"

//JDK and Java Version
extra["jdk"] = "17"
extra["javaVersion"] = JavaVersion.VERSION_17

/**
 * Checks whether to Suppress Warnings
 */
val shouldSuppressWarnings by extra(
    fun(): Boolean = isServerBuild() && System.getenv("DO_NOT_SUPPRESS_WARNINGS") != "true"
)

if (!shouldUsePrebuiltSdk() || isServerBuild()) {
    apply(from = "${project.rootDir}/tools/prebuilt-sdk.gradle")
}
