import mega.privacy.android.gradle.configureTestOptionsIfAndroidApplication
import mega.privacy.android.gradle.configureTestOptionsIfAndroidLibrary
import mega.privacy.android.gradle.enableParallelTest
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for Android tests.
 */
class AndroidTestConventionPlugin : Plugin<Project> {

    /**
     * Apply this plugin to the given target object.
     *
     * @param target The target object
     */
    override fun apply(target: Project) {
        with(target) {
            configureTestOptionsIfAndroidLibrary()
            configureTestOptionsIfAndroidApplication()
            enableParallelTest()
        }
    }
}

