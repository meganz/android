import com.android.build.api.dsl.ApplicationExtension
import mega.privacy.android.gradle.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType


/**
 * Plugin to apply Android application convention with Compose
 */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {

    /**
     * Apply the convention
     *
     * @param target
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidCompose(extension)
        }
    }

}
