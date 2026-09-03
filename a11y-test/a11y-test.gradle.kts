plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
}

android {
    namespace = "mega.privacy.android.a11ytest"

    defaultConfig {
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"
    }

    testOptions {
        managedDevices {
            localDevices {
                create("ciPixel") {
                    device = "Pixel 10"
                    apiLevel = 36
                    systemImageSource = "google"
                    // Pinned so GMD never falls back to the x86_64 image on a mismatched
                    // host; CI runs this device on Apple Silicon agents only.
                    testedAbi = "arm64-v8a"
                }
            }
        }
    }
}

dependencies {
    implementation(platform(androidx.compose.bom))
    implementation(testlib.androidx.compose.ui.test.junit4.accessibility)
    implementation(testlib.hamcrest)
    implementation(testlib.test.storage)
    implementation(google.guava)

    androidTestImplementation(platform(androidx.compose.bom))
    androidTestImplementation(androidx.compose.ui)
    androidTestImplementation(androidx.compose.foundation)
    androidTestImplementation(androidx.compose.activity)
    androidTestImplementation(androidx.material3)
    // Screen-level sweeps mount stateless feature-module screens directly, so the modules that
    // own them and the modules declaring their UI-state types are compile-time dependencies.
    androidTestImplementation(project(":feature:settings"))
    androidTestImplementation(project(":feature:transfers:transfers"))
    androidTestImplementation(project(":feature:payment:payment-snowflake-components"))
    androidTestImplementation(project(":domain"))
    androidTestImplementation(project(":core:analytics:analytics-test"))
    androidTestImplementation(project(":resources:string-resources"))
    androidTestImplementation(lib.mega.core.ui)
    androidTestImplementation(lib.kotlinx.collections.immutable)
    androidTestImplementation(testlib.androidx.compose.ui.test.junit4)
    androidTestImplementation(testlib.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(testlib.truth)
    debugImplementation(testlib.androidx.compose.ui.testManifest)
    androidTestUtil(testlib.test.services)
}
