plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.navigation.contract"
}

dependencies {
    implementation(project(":domain"))

    // AndroidX
    implementation(androidx.navigation.compose)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.material3)

    // Core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.analytics)

    // Logging
    implementation(lib.logging.timber)
}