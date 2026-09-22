package mega.privacy.android.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * The per-module entry point CI and the root aggregate task call.
 */
internal const val ACCESSIBILITY_CHECK_TASK_NAME = "accessibilityCheck"

/**
 * The Gradle property CI sets to pick between the per-merge-request gate run and the weekly
 * design report run, and the runner argument it is forwarded to. `AccessibilityRunMode` in
 * `:a11y-test` reads the argument and holds the accepted values; leaving the property unset runs
 * both policies at once.
 */
internal const val ACCESSIBILITY_RUN_MODE_PROPERTY = "megaAccessibilityRunMode"

internal fun Project.configureAccessibilityTest(commonExtension: CommonExtension) {
    val runMode = providers.gradleProperty(ACCESSIBILITY_RUN_MODE_PROPERTY)

    commonExtension.apply {
        defaultConfig.testInstrumentationRunnerArguments["useTestStorageService"] = "true"
        runMode.orNull?.let {
            defaultConfig.testInstrumentationRunnerArguments[ACCESSIBILITY_RUN_MODE_PROPERTY] = it
        }
    }

    configurations.configureEach {
        if (name.startsWith("androidTest") || name.contains("AndroidTest")) {
            // The accessibility test framework needs a protobuf runtime, and the javalite one
            // DataStore pulls in repeats every class of the full runtime the production modules
            // already carry. A test APK cannot hold both, so settle it the way `:app` and
            // `:data` already settle it for the production APK: keep the full runtime.
            exclude(mapOf("module" to "protolite-well-known-types"))
            exclude(mapOf("module" to "protobuf-javalite"))
        }
    }

    dependencies {
        add("androidTestImplementation", platform(androidx.findLibrary("compose-bom").get()))
        add("androidTestImplementation", google.findLibrary("protobuff").get())
        // Every sweep mounts its content into a host activity through createAndroidComposeRule.
        add("androidTestImplementation", androidx.findLibrary("compose-activity").get())
        add("androidTestImplementation", testlib.findLibrary("androidx-compose-ui-test-junit4").get())
        add(
            "androidTestImplementation",
            testlib.findLibrary("androidx-compose-ui-test-junit4-accessibility").get(),
        )
        add("debugImplementation", testlib.findLibrary("androidx-compose-ui-testManifest").get())
        // Test Storage is how the harness gets its JSON reports off the device; the service has
        // to be installed alongside the test APK for the runner argument above to have a peer.
        add("androidTestUtil", testlib.findLibrary("test-services").get())

        // The harness itself, unless this is the harness module.
        if (path != HARNESS_PROJECT_PATH) {
            add("androidTestImplementation", project(HARNESS_PROJECT_PATH))
        }
    }

    tasks.register(ACCESSIBILITY_CHECK_TASK_NAME) {
        group = "Verification"
        description =
            "Runs this module's accessibility checks on the $CI_MANAGED_DEVICE_NAME managed device"
        dependsOn("${CI_MANAGED_DEVICE_NAME}DebugAndroidTest")
    }
}

private const val HARNESS_PROJECT_PATH = ":a11y-test"
