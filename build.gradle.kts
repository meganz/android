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
 * Paths in alwaysFullPaths are the ones whose blast radius is the whole build. Paths in
 * ignoredPaths change nothing the build produces. Everything else is owned by a declared module
 * or, belonging to no module, forces a full run anyway.
 *
 * ignoredPaths is the only list here that can select too *few* modules, so an entry must be unable
 * to change a build output; anything arguable belongs in alwaysFullPaths, which costs time and
 * nothing else. The other direction is deliberate: selecting too many modules only costs time,
 * while selecting too few silently skips tests that would have failed.
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
    ignoredPaths = listOf(
        ".claude",
        ".cursor",
        ".github",
        ".gitignore",
        ".gitleaks",
        ".gitleaksignore",
        ".idea",
        ".testrail",
        "CREDITS.md",
        "LICENCE.md",
        "README.md",
    )
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
extra["targetSdkVersion"] = 37

extra["buildTools"] = "37.0.0"

// Prebuilt MEGA SDK version
extra["megaSdkVersion"] = "20261013.064158-dev"

//JDK and Java Version
extra["jdk"] = "25"
extra["javaVersion"] = JavaVersion.VERSION_25

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

tasks.register("runAffectedUnitTestsWithCoverage") {
    group = "Verification"
    description = "Runs unit tests for the modules listed in -PaffectedModules, as CI does for a selective build"

    // An absent property is a caller mistake, and the safe reading of "I do not know which modules"
    // is never "run none of them". An empty property is different and legitimate: it is what the
    // detector reports for a change that touches no module, such as documentation only.
    val requested = providers.gradleProperty("affectedModules").orNull
    requireNotNull(requested) {
        "runAffectedUnitTestsWithCoverage needs -PaffectedModules=<comma separated module paths>. " +
                "Use runAllUnitTestsWithCoverage to run everything."
    }

    val directories = requested.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val selected = rootProject.subprojects
        .filter { directories.contains(it.path.removePrefix(":").replace(":", "/")) }
    val testTasks = selected.flatMap { sub ->
        listOfNotNull(
            sub.tasks.findByName("testDebugUnitTestCoverage")?.path
                ?: sub.tasks.findByName("createUnitTestCoverageReport")?.path
                ?: sub.tasks.findByName("jacocoTestReport")?.path
        )
    }.distinct()

    val unknown = directories - selected.map { it.path.removePrefix(":").replace(":", "/") }.toSet()
    if (unknown.isNotEmpty()) {
        logger.warn("runAffectedUnitTestsWithCoverage: no such module(s): ${unknown.sorted()}")
    }
    println("Running ${testTasks.size} test task(s) for ${directories.size} selected module(s): $testTasks")
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
