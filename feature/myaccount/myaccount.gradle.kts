plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    lint {
        disable += "CoroutineCreationDuringComposition"
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.myaccount"
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {
    implementation(project(":navigation"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":domain"))
    implementation(project(":icon-pack"))
    implementation(project(":shared:resources"))
    implementation(project(":shared:original-core-ui"))
    implementation(project(":core:ui-components:shared-components"))

    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.hilt.navigation)
    implementation(androidx.material3.adaptive.navigation.suite)
    implementation(androidx.navigation3.runtime)
    implementation(lib.kotlin.serialisation)
    implementation(lib.mega.analytics)
    implementation(lib.mega.core.ui)
    implementation(lib.logging.timber)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(testlib.bundles.ui.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
