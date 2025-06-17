import mega.privacy.android.build.shouldApplyDefaultConfiguration

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.lint)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.de.mannodermaus.android.junit5)
}

android {
    namespace = "mega.privacy.android.analytics"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    lintChecks(project(":lint"))
    implementation(project(":domain"))

    // Analytics
    implementation(lib.mega.analytics)

    // DI
    implementation(lib.javax.inject)

    // Framework
    implementation(androidx.bundles.compose.bom)
    implementation(lib.kotlin.ktx)
    implementation(androidx.appcompat)

    // Logging
    implementation(lib.bundles.logging)

    // Testing
    testImplementation(testlib.junit)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
    testImplementation(testlib.junit.test.ktx)

    testImplementation(testlib.bundles.unit.test)

    testImplementation(testlib.mockito)
    testImplementation(testlib.mockito.kotlin)
    testImplementation(testlib.mockito.android)
}