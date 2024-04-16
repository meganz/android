package mega.privacy.android.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType


fun Project.enableParallelTest() = tasks.withType<Test> {
    maxParallelForks =
        (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

fun Project.useJUnit5() {
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

fun Project.configureTestOptionsIfAndroidApplication() {
    extensions.findByType<ApplicationExtension>()?.let {
        extensions.configure<ApplicationExtension> {
            testOptions.unitTests {
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
            }
        }
    }
}

fun Project.configureTestOptionsIfAndroidLibrary() {
    extensions.findByType<LibraryExtension>()?.let {
        extensions.configure<LibraryExtension> {
            testOptions.unitTests {
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
            }
        }
    }
}

fun Project.configureAndroidTestDependencies() {
    dependencies {
        add("testImplementation", testlib.findLibrary("junit").get())
        add("testImplementation", testlib.findLibrary("junit-test.ktx").get())
        add("testImplementation", testlib.findLibrary("espresso").get())
        add("testImplementation", testlib.findLibrary("compose-junit").get())

        add("testImplementation", testlib.findBundle("ui-test").get())
        add("testImplementation", testlib.findBundle("unit-test").get())
        add("testImplementation", testlib.findLibrary("arch-core-test").get())
        add("testImplementation", testlib.findLibrary("test-core-ktx").get())
        add("testImplementation", testlib.findLibrary("mockito").get())
        add("testImplementation", testlib.findLibrary("mockito-kotlin").get())
        add("testImplementation", testlib.findLibrary("mockito-android").get())

        add("testRuntimeOnly", testlib.findLibrary("junit.jupiter.engine").get())
        add("testImplementation", platform(testlib.findLibrary("junit5-bom").get()))
        add("testImplementation", testlib.findBundle("junit5-api").get())
    }
}