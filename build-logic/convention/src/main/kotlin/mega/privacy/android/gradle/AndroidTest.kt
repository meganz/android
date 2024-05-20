package mega.privacy.android.gradle


import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.UnitTestOptions
import com.android.build.gradle.LibraryExtension
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
}

/**
 * Use JUnit5 in unit tests
 *
 */
fun Project.useJUnit5() {
    tasks.withType<Test> {
        useJUnitPlatform()
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
