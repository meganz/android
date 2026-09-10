package mega.privacy.android.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * The managed device every accessibility check runs on, and the only definition of it.
 *
 * The checks measure rendered pixels and touch target sizes, so a run is only comparable to the
 * baseline it is judged against when the density, font scale and platform accessibility
 * behaviour are identical. Pinning one device for every module keeps the whole suite comparable.
 */
internal const val ACCESSIBILITY_DEVICE_NAME = "ciPixel"

/**
 * The per-module entry point CI and the root aggregate task call.
 */
internal const val ACCESSIBILITY_CHECK_TASK_NAME = "accessibilityCheck"

internal fun Project.configureAccessibilityTest(commonExtension: CommonExtension) {
    commonExtension.apply {
        defaultConfig.testInstrumentationRunnerArguments["useTestStorageService"] = "true"

        testOptions.managedDevices.localDevices.create(ACCESSIBILITY_DEVICE_NAME) {
            device = "Pixel 10"
            apiLevel = 36
            systemImageSource = "google"
            // Pinned so GMD never falls back to the x86_64 image on a mismatched host; CI runs
            // this device on Apple Silicon agents only.
            testedAbi = "arm64-v8a"
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
            "Runs this module's accessibility checks on the $ACCESSIBILITY_DEVICE_NAME managed device"
        dependsOn("${ACCESSIBILITY_DEVICE_NAME}DebugAndroidTest")
    }
}

private const val HARNESS_PROJECT_PATH = ":a11y-test"
