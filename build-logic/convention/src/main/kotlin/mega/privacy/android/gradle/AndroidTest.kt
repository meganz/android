package mega.privacy.android.gradle


import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.UnitTestOptions
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.tasks.ManagedDeviceInstrumentationTestTask
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType

/**
 * Make sure unit tests are run in parallel. taking full use of all multiple CPU cores
 *
 */
fun Project.enableParallelTest() = tasks.withType<Test> {
    maxParallelForks =
        (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    failOnNoDiscoveredTests.set(false)
}

/**
 * Use JUnit5 in unit tests
 *
 */
fun Project.useJUnit5() {
    tasks.withType<Test> {
        useJUnitPlatform()
        failOnNoDiscoveredTests.set(false)
    }
}

/**
 * configure test options for Android application
 */
fun Project.configureTestOptionsIfAndroidApplication() {
    extensions.findByType<ApplicationExtension>()?.let {
        extensions.configure<ApplicationExtension> {
            testOptions.unitTests {
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
                setTestReportPath(this)
            }
        }
    }
}

/**
 * configure test options for Android library
 */
fun Project.configureTestOptionsIfAndroidLibrary() {
    extensions.findByType<LibraryExtension>()?.let {
        extensions.configure<LibraryExtension> {
            testOptions.unitTests {
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
                setTestReportPath(this)
            }
        }
    }
}

private fun Project.setTestReportPath(unitTestOptions: UnitTestOptions) {
    unitTestOptions.all {
        it.reports {
            val path = "${layout.buildDirectory.get()}/unittest"

            html.required.set(true)
            html.outputLocation.set(file("$path/html"))

            junitXml.required.set(true)
            junitXml.outputLocation.set(file("$path/junit"))
        }
    }
}

/**
 * Report the instrumented test a Gradle Managed Device task is currently running.
 *
 * AGP prints nothing between the start of such a task and its report at the end, which leaves a
 * full run of `:app:ciPixelGmsDebugAndroidTest` opaque for the best part of an hour. Set the
 * `mega.androidTest.progress` Gradle property to `false` to turn this off.
 */
fun Project.logManagedDeviceTestProgress() {
    if (providers.gradleProperty("mega.androidTest.progress").orNull?.toBoolean() == false) return

    val testListFile = layout.buildDirectory.file("androidTestProgress/test-list.txt")

    tasks.withType<ManagedDeviceInstrumentationTestTask>().configureEach {
        // Scoped to the project: convention plugin classes reach separate projects through
        // separate classloaders, and one shared name hands them an instance they cannot cast.
        val progressService = project.gradle.sharedServices.registerIfAbsent(
            "megaAndroidTestProgress${project.path}",
            AndroidTestProgressService::class.java,
        ) {}
        usesService(progressService)
        val resultsDir = getResultsDir()
        doFirst {
            val taskLogger = logger
            progressService.get().start(
                resultsDir = resultsDir.get().asFile,
                testListFile = testListFile.get().asFile,
                log = { line -> taskLogger.lifecycle(line) },
            )
        }
    }
}
