import mega.privacy.android.build.isServerBuild

plugins {
    alias(plugin.plugins.kotlin.compose) apply false
    alias(plugin.plugins.ksp) apply false
    alias(plugin.plugins.mega.android.cicd)
    alias(plugin.plugins.mega.android.release)
    alias(plugin.plugins.jfrog.artifactory) apply false
    alias(plugin.plugins.mega.artifactory.publish.convention) apply false
    alias(plugin.plugins.de.mannodermaus.android.junit5) apply false
    alias(plugin.plugins.jetbrains.kotlin.android) apply false
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        mavenCentral()
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
        classpath(lib.kotlin.serialisation)
        classpath("androidx.benchmark:benchmark-baseline-profile-gradle-plugin:1.3.3")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:${plugin.versions.jfrog.artifactory.get()}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
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

        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/mega-telephoto")
        }

        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/mega-ucrop-n-edit")
        }
    }
    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory.get())
}


// Define versions in a single place
// App
extra["appVersion"] = "15.18"

// Sdk and tools
extra["compileSdkVersion"] = 36
extra["minSdkVersion"] = 26
extra["targetSdkVersion"] = 36

extra["buildTools"] = "36.0.0"

// Prebuilt MEGA SDK version
extra["megaSdkVersion"] = "20250917.232128-rel"

//JDK and Java Version
extra["jdk"] = "21"
extra["javaVersion"] = JavaVersion.VERSION_21

/**
 * Checks whether to Suppress Warnings
 */
val shouldSuppressWarnings by extra(
    fun(): Boolean = isServerBuild() && System.getenv("DO_NOT_SUPPRESS_WARNINGS") != "true"
)

tasks.register("printSubprojectPaths") {
    doLast {
        rootProject.subprojects.forEach {
            println("SUBPROJECT_PATH: " + it.path.removePrefix(":").replace(":", "/"))
        }
    }
}

tasks.register("runAllUnitTestsWithCoverage") {
    group = "Verification"
    description = "Runs all unit tests same as CI/CD pipeline"
    val testTasks = rootProject.subprojects.flatMap { sub ->
        listOfNotNull(
            // We only add one test task for each module, which is the most fitted one.
            sub.tasks.findByName("testDebugUnitTestCoverage")?.path   // for Android library
                ?: sub.tasks.findByName("createUnitTestCoverageReport")?.path // for Android app
                ?: sub.tasks.findByName("jacocoTestReport")?.path     // for JVM module
        )
    }.distinct()
    println("Running Test Tasks: $testTasks")
    dependsOn(testTasks)
}

tasks.register("printModulesWithUnitTest") {
    doLast {
        val unitTestTaskNames = listOf(
            "testDebugUnitTestCoverage", // Android library
            "createUnitTestCoverageReport", // Android app
            "jacocoTestReport" // JVM module
        )
        rootProject.subprojects.forEach { sub ->
            val hasUnitTestTask = unitTestTaskNames.any { sub.tasks.findByName(it) != null }
            if (hasUnitTestTask) {
                println("UNIT-TEST-MODULE: ${sub.path.removePrefix(":").replace(":", "/")}")
            }
        }
    }
}
