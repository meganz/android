import mega.privacy.android.build.preBuiltSdkDependency

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
    namespace = "mega.privacy.android.feature.sharelink"
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":core:navigation-contract"))
    implementation(project(":navigation"))
    implementation(project(":domain"))
    implementation(project(":core:analytics:analytics-tracker"))
    implementation(project(":core:coroutine"))
    implementation(project(":core:formatter"))
    implementation(project(":core:ui-components:shared-components"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))
    implementation(project(":shared:nodes"))

    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)
    implementation(lib.mega.analytics)

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(androidx.navigation3.runtime)

    // Compose
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.compose.viewmodel)
    implementation(androidx.compose.activity)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)
    implementation(androidx.hilt.navigation)
    implementation(lib.compose.state.events)
    implementation(lib.coil.compose)

    // screenshot tests
    screenshotTestImplementation(platform(androidx.compose.bom))
    screenshotTestImplementation(androidx.compose.ui.tooling)
    screenshotTestImplementation(testlib.compose.screenshot)
    screenshotTestImplementation(project(":core-test"))

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(project(":core:analytics:analytics-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
