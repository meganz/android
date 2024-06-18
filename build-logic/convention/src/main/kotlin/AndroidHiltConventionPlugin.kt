import mega.privacy.android.gradle.androidx
import mega.privacy.android.gradle.google
import mega.privacy.android.gradle.shouldApplyDefaultConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Plugin to apply Hilt to Android project.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    /**
     * Apply the plugin to the project.
     *
     * @param target the project to apply the plugin to
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("kotlin-kapt")

            if (shouldApplyDefaultConfiguration()) {
                pluginManager.apply("dagger.hilt.android.plugin")

                dependencies {
                    "kapt"(google.findLibrary("hilt.android.compiler").get())
                    "kapt"(androidx.findLibrary("hilt.compiler").get())
                }
            }

            dependencies {
                "implementation"(google.findLibrary("hilt.android").get())
                "implementation"(google.findLibrary("hilt.core").get())
            }
        }
    }
}
