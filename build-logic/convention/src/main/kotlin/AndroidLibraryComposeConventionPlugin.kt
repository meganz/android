import com.android.build.gradle.LibraryExtension
import mega.privacy.android.gradle.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Plugin for Android library with Compose
 */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    /**
     * apply the conventions
     *
     * @param target
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.getByType<LibraryExtension>()
            configureAndroidCompose(extension)
        }
    }
}
