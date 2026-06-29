plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
}

android {
    namespace = "mega.privacy.android.feature.documentscanner"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":navigation"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:coroutine"))

    // Compose
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.hilt.navigation)
    implementation(androidx.material3)
    implementation(androidx.navigation3.runtime)
    implementation(lib.compose.state.events)
    implementation(lib.logging.timber)
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    // CameraX
    implementation(androidx.camera.camera2)
    implementation(androidx.camera.lifecycle)
    implementation(androidx.camera.view)

    // LiteRT (TFLite) runtime for UNet-based boundary detection
    implementation(lib.litert)
    implementation(lib.litert.gpu)

    // OkHttp to download the model artifact on first use (not bundled in the APK)
    implementation(lib.okhttp3)

    // WorkManager runs the background model download
    implementation(androidx.work.ktx)
    implementation(androidx.hilt.work)

    // Jetpack DataStore for the small cellular-consent preference
    implementation(androidx.datastore.preferences)

    // Testing
    testImplementation(project(":core-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(androidx.work.test)
}
