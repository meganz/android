import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import mega.privacy.android.gradle.configureKotlinAndroid
import mega.privacy.android.gradle.testlib
import mega.privacy.android.gradle.useJUnit5
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate

/**
 * Convention plugin for Android library modules
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {

    /**
     * Apply this plugin to the given target object.
     *
     * @param target The target object
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("mega.android.library.jacoco")
                apply("mega.android.test")
                apply("mega.lint")
                apply("mega.android.architecture")
            }

            extensions.configure<LibraryExtension> {
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                packaging {
                    // JUnit 5 platform jars each ship these; without excluding them the
                    // androidTest resource merge fails with duplicate-file errors.
                    resources.excludes += "/META-INF/LICENSE.md"
                    resources.excludes += "/META-INF/LICENSE-notice.md"
                }
                configureKotlinAndroid(this)
            }

            extensions.configure<LibraryAndroidComponentsExtension> {
                // A module with no instrumented tests still declares AndroidJUnitRunner but never
                // bundles it, so connectedAndroidTest launches an empty APK that crashes with
                // ClassNotFoundException. Skip building androidTest for modules with no sources.
                val hasAndroidTestSources = projectDir.resolve("src/androidTest")
                    .walkTopDown().any { it.isFile }
                beforeVariants { variant ->
                    if (!hasAndroidTestSources) {
                        variant.androidTest.enable = false
                    }
                }
            }

            useJUnit5()
            dependencies {
                add("testRuntimeOnly", platform(testlib.findLibrary("junit5-bom").get()))
                add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")

                // Base instrumented-test stack so any module can add a src/androidTest test
                // without repeating the runner/JUnit4 setup. Modules needing more (Espresso,
                // Truth, Hilt test, ...) add it on top.
                add("androidTestImplementation", testlib.findLibrary("runner").get())
                add("androidTestImplementation", testlib.findLibrary("junit").get())
                add("androidTestImplementation", testlib.findLibrary("test-ext-junit").get())
            }
        }
    }
}