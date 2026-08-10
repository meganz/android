plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.compose.screenshot)
}

android {
    namespace = "mega.privacy.android.shared.contact"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":resources:string-resources"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":third-party-lib:twemoji"))

    implementation(lib.vdurmont.emoji)
    implementation(lib.logging.timber)

    // core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    // Compose
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)

    // Inject annotations
    implementation(lib.javax.inject)

    testImplementation(platform(testlib.junit5.bom))
    testImplementation(project(":core-ui-test"))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    // screenshot tests
    screenshotTestImplementation(platform(androidx.compose.bom))
    screenshotTestImplementation(androidx.compose.ui.tooling)
    screenshotTestImplementation(testlib.compose.screenshot)
}
