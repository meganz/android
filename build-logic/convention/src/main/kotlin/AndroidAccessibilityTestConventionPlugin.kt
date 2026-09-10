import com.android.build.api.dsl.LibraryExtension
import mega.privacy.android.gradle.configureAccessibilityTest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin that lets a module own the accessibility checks for the screens it owns.
 *
 * Applying it is the whole setup: the module gets the shared `ciPixel` managed device, the
 * compose accessibility test stack, the Test Storage wiring the harness writes its reports
 * through, the `:a11y-test` harness itself, and an `accessibilityCheck` task the root aggregate
 * task picks up. The module then adds `src/androidTest` sweeps that mount its own composables —
 * `internal` ones included, which is why the checks live here rather than in one central module.
 */
class AndroidAccessibilityTestConventionPlugin : Plugin<Project> {

    /**
     * Apply this plugin to the given target object.
     *
     * @param target The target object
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("mega.android.library")
            pluginManager.apply("mega.android.library.compose")

            configureAccessibilityTest(extensions.getByType<LibraryExtension>())
        }
    }
}
