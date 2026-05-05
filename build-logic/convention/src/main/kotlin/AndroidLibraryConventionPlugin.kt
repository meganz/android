import com.android.build.api.dsl.LibraryExtension
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
                configureKotlinAndroid(this)
            }

            useJUnit5()
            dependencies {
                add("testRuntimeOnly", platform(testlib.findLibrary("junit5-bom").get()))
                add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
            }
        }
    }
}