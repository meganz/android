import mega.privacy.android.gradle.enableParallelTest
import mega.privacy.android.gradle.useJUnit5
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * Convention plugin for Jvm module tests
 */
class JvmTestConventionPlugin : Plugin<Project> {
    /**
     * Apply this plugin to the given target object.
     *
     * @param target target project
     */
    override fun apply(target: Project) {
        with(target) {
            useJUnit5()
            enableParallelTest()
            setTestReportPath()
        }
    }

    private fun Project.setTestReportPath() = tasks.withType<Test> {
        reports {
            val path = "${layout.buildDirectory.get()}/unittest"

            html.required.set(true)
            html.outputLocation.set(file("$path/html"))

            junitXml.required.set(true)
            junitXml.outputLocation.set(file("$path/junit"))
        }
    }
}