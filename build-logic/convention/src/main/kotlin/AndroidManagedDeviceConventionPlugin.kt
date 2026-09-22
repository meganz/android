import com.android.build.api.dsl.CommonExtension
import mega.privacy.android.gradle.configureCiManagedDevice
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin that gives a module the shared CI Gradle Managed Device.
 *
 * A managed device is build configuration on the module's own `android.testOptions`, not a code
 * artifact, so a module cannot inherit one through a dependency. Applying this plugin is the
 * whole setup, and it is what keeps every module's CI instrumented-test run on the same device,
 * API level and ABI.
 *
 * It deliberately configures nothing else: the test orchestrator, Test Storage and anything else
 * a particular suite needs stays with the module or the plugin that needs it.
 */
class AndroidManagedDeviceConventionPlugin : Plugin<Project> {

    /**
     * Apply this plugin to the given target object.
     *
     * @param target The target object
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.base") {
                extensions.getByType<CommonExtension>().configureCiManagedDevice()
            }
        }
    }
}
