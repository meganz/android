import mega.privacy.android.gradle.enableParallelTest
import mega.privacy.android.gradle.useJUnit5
import org.gradle.api.Plugin
import org.gradle.api.Project

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
        }
    }
}