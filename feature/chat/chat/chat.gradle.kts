plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.room)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    alias(plugin.plugins.compose.screenshot)
}

android {
    namespace = "mega.privacy.android.feature.chat"

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))
    implementation(project(":domain"))
    implementation(project(":navigation"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:coroutine"))
    implementation(project(":core:formatter"))
    implementation(project(":shared:chats"))

    implementation(lib.mega.core.ui)
    implementation(lib.mega.analytics)
    implementation(lib.kotlinx.collections.immutable)
    implementation(androidx.material3.window)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)
    implementation(androidx.hilt.navigation)
    implementation(lib.kotlin.serialisation)
    implementation(androidx.navigation.compose)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.navigation3.ui)
    implementation(lib.logging.timber)

    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(google.hilt.android.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    // screenshot tests
    screenshotTestImplementation(platform(androidx.compose.bom))
    screenshotTestImplementation(androidx.compose.ui.tooling)
    screenshotTestImplementation(testlib.compose.screenshot)
}