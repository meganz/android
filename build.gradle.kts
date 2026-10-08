import mega.privacy.android.build.isServerBuild

plugins {
    alias(plugin.plugins.kotlin.compose) apply false
    alias(plugin.plugins.ksp) apply false
    alias(plugin.plugins.mega.android.cicd)
    alias(plugin.plugins.mega.android.release)
    alias(plugin.plugins.jfrog.artifactory) apply false
    alias(plugin.plugins.mega.artifactory.publish.convention) apply false
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
        classpath(plugin.jfrog)
        classpath(plugin.kotlin.gradle)
        classpath(lib.kotlin.serialisation)
        classpath(plugin.benchmark.baseline.profile)
        classpath(lib.okhttp3)
        classpath(lib.okio)
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
        flatDir {
            dirs("${rootProject.projectDir}/third-party-lib/pdfiumAndroid")
        }
    }
    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
    }
}

/**
 * Which modules CI has to verify for a given merge request.
 *
 * Paths listed here are the ones whose blast radius is the whole build; everything else is either
 * owned by a declared module or, if it belongs to no module at all, forces a full run anyway. That
 * fail-safe is deliberate: selecting too many modules only costs time, while selecting too few
 * silently skips tests that would have failed.
 *
 * AffectedModulesConfigTest in build-logic keeps this list honest against the repository layout.
 */
affectedModules {
    targetBranch = "develop"
    alwaysFullPaths = listOf(
        ".gitattributes",
        ".gitlab-ci.yml",
        ".gitmodules",
        "build-logic",
        "build.gradle.kts",
        "buildSrc",
        "custom_lint.xml",
        "gradle",
        "gradle.properties",
        "gradlew",
        "gradlew.bat",
        "jenkinsfile",
        "settings.gradle.kts",
        "tools",
    )
    releaseBranchPrefixes = listOf("release/")
    excludedModules = listOf(":baselineprofile")
    narrowableResourceModules = listOf(":resources:string-resources")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory.get())
}


// Define versions in a single place
// App
extra["appVersion"] = "16.14"

// Sdk and tools
extra["compileSdkVersion"] = 37
extra["minSdkVersion"] = 28
extra["targetSdkVersion"] = 36

extra["buildTools"] = "37.0.0"

// Prebuilt MEGA SDK version
extra["megaSdkVersion"] = "20261008.012553-rel"

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

tasks.register("runAllAccessibilityChecks") {
    group = "Verification"
    description = "Runs every module's accessibility checks same as CI/CD pipeline"
    val checkTasks = rootProject.subprojects.mapNotNull { sub ->
        // Registered by the mega.android.a11y.test convention plugin, so the aggregate covers
        // whatever set of modules currently owns accessibility checks.
        sub.tasks.findByName("accessibilityCheck")?.path
    }.distinct()
    println("Running Accessibility Check Tasks: $checkTasks")
    dependsOn(checkTasks)
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
