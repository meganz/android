import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Plugin that applies Android Lint conventions to the project.
 */
class AndroidLintConventionPlugin : Plugin<Project> {

    /**
     * Applies the Android Lint conventions to the project.
     * @param target the project to apply the conventions to
     */
    override fun apply(target: Project) = target.run {
        when {
            pluginManager.hasPlugin("com.android.application") ->
                configure<ApplicationExtension> {
                    lint { configure(target) }
                }

            pluginManager.hasPlugin("com.android.library") ->
                configure<LibraryExtension> {
                    lint { configure(target) }
                }

            else -> {
                pluginManager.apply("com.android.lint")
                configure<Lint> { configure(target) }
            }
        }
    }
}

private fun Lint.configure(project: Project) {
    abortOnError = false
    xmlReport = true
    xmlOutput = project.file("build/reports/lint-results.xml")
}
