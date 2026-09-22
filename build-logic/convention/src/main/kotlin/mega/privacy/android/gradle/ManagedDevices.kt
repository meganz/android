package mega.privacy.android.gradle

import com.android.build.api.dsl.CommonExtension

/**
 * The Gradle Managed Device every CI instrumented-test run uses, and the only definition of it.
 *
 * One device for the whole repository keeps runs comparable. The accessibility checks in
 * particular measure rendered pixels and touch target sizes, so a run is only comparable to the
 * baseline it is judged against when the density, font scale and platform accessibility
 * behaviour are identical.
 */
internal const val CI_MANAGED_DEVICE_NAME = "ciPixel"

/**
 * Register the shared CI managed device on a module, giving it a
 * `<device name>...AndroidTest` task.
 */
internal fun CommonExtension.configureCiManagedDevice() {
    testOptions.managedDevices.localDevices.create(CI_MANAGED_DEVICE_NAME) {
        device = "Pixel 10"
        apiLevel = 36
        systemImageSource = "google"
        // Pinned so GMD never falls back to the x86_64 image on a mismatched host; CI runs
        // this device on Apple Silicon agents only.
        testedAbi = "arm64-v8a"
    }
}
