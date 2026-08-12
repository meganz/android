plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(plugin.plugins.kotlin.serialisation)
    alias(plugin.plugins.compose.screenshot)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.navigation.snowflake"

    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:analytics:analytics-tracker"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))
    implementation(platform(androidx.compose.bom))
    implementation(androidx.compose.icons)
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

    // screenshot tests
    screenshotTestImplementation(platform(androidx.compose.bom))
    screenshotTestImplementation(androidx.compose.ui.tooling)
    screenshotTestImplementation(testlib.compose.screenshot)
}