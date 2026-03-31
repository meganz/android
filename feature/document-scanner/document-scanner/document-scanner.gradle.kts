plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    id("kotlin-android")
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

    // Testing
    testImplementation(project(":core-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
}
