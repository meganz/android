plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    alias(plugin.plugins.compose.screenshot)
}

android {
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    lint {
        abortOnError = true
    }
    namespace = "mega.privacy.android.core.passcode"
}

dependencies {
    lintChecks(project(":lint"))

    implementation(project(":domain"))
    implementation(project(":core:passcode:passcode-snowflake-components"))
    implementation(project(":core:analytics:analytics-tracker"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:coroutine"))
    implementation(project(":resources:string-resources"))
    implementation(lib.logging.timber)
    implementation(lib.mega.analytics)
    implementation(lib.mega.core.ui)
    implementation(androidx.appcompat)
    implementation(androidx.biometric)
    implementation(androidx.hilt.navigation)
    implementation(androidx.lifecycle.runtime)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.compose.activity)
    implementation(androidx.compose.runtime.retain)
    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.material3)
    implementation(lib.compose.state.events)

    // screenshot tests
    screenshotTestImplementation(platform(androidx.compose.bom))
    screenshotTestImplementation(androidx.compose.ui.tooling)
    screenshotTestImplementation(testlib.compose.screenshot)
    screenshotTestImplementation(project(":core-test"))

    testImplementation(project(":core-test"))
    testImplementation(project(":core:analytics:analytics-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.compose.junit)
    testImplementation(testlib.compose.manifest)
    testImplementation(google.hilt.android.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
