plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.navigation.snowflake"
}

dependencies {
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:analytics:analytics-tracker"))
    implementation(platform(androidx.compose.bom))
    implementation(androidx.navigation.compose)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.material3.adaptive.navigation.suite)
    implementation(lib.kotlinx.collections.immutable)
    implementation(lib.logging.timber)

    implementation(lib.mega.analytics)

    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    debugImplementation(testlib.compose.manifest)

    // Test dependencies
    testImplementation(project(":core:analytics:analytics-test"))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(lib.kotlin.serialisation)
}