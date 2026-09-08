plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    alias(plugin.plugins.compose.screenshot)
}

android {
    lint {
        disable += "CoroutineCreationDuringComposition"
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.myaccount"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {
    implementation(project(":navigation"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:coroutine"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))
    implementation(project(":shared:original-core-ui"))
    implementation(project(":shared:account"))
    implementation(project(":legacy-core-ui"))
    implementation(project(":feature:myaccount:myaccount-snowflake-components"))
    implementation(project(":core:ui-components:shared-components"))
    implementation(project(":core:formatter"))
    implementation(project(":third-party-lib:twemoji"))
    implementation(project(":core:analytics:analytics-tracker"))


    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.hilt.navigation)
    implementation(androidx.material3.adaptive.navigation.suite)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.constraintlayout.compose)
    implementation(lib.kotlin.serialisation)
    implementation(lib.compose.state.events)
    implementation(lib.mega.analytics)
    implementation(lib.mega.core.ui)
    implementation(lib.logging.timber)
    implementation(lib.coil3)
    implementation(lib.coil.compose)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(testlib.bundles.ui.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    // screenshot tests
    screenshotTestImplementation(platform(androidx.compose.bom))
    screenshotTestImplementation(androidx.compose.ui.tooling)
    screenshotTestImplementation(testlib.compose.screenshot)
}
