import mega.privacy.android.gradle.google
import mega.privacy.android.gradle.shouldApplyDefaultConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Plugin to apply Hilt to JVM project.
 */
class JvmHiltConventionPlugin : Plugin<Project> {
    /**
     * Apply the plugin to the project.
     *
     * @param target the project to apply the plugin to
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")

            dependencies {
                add("implementation", google.findLibrary("hilt.core").get())

                if (shouldApplyDefaultConfiguration()) {
                    "ksp"(google.findLibrary("hilt.android.compiler").get())
                }
            }
        }
    }
}