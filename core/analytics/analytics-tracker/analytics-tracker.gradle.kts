plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
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
    implementation(project(":core:navigation-contract"))

    // Analytics
    implementation(lib.mega.analytics)

    // Firebase
    implementation(platform(google.firebase.bom))
    implementation(google.firebase.analytics)

    // DI
    implementation(lib.javax.inject)

    // Framework
    implementation(androidx.bundles.compose.bom)
    implementation(lib.kotlin.ktx)
    implementation(androidx.appcompat)
    implementation(androidx.navigation3.runtime)

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