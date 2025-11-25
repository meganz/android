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
    implementation(project(":shared:resources"))

    // AndroidX
    implementation(androidx.navigation.compose)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.navigation3.ui)
    implementation(androidx.material3)

    // Core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.analytics)

    // Logging
    implementation(lib.logging.timber)

    implementation(lib.javax.inject)

    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.truth)
    testImplementation(testlib.truth.ext)
}