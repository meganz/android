plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.de.mannodermaus.android.junit5)
    id("kotlin-android")
}

android {
    lint {
        abortOnError = true
    }
    defaultConfig {
        testInstrumentationRunner = "mega.privacy.android.app.HiltTestRunner"
    }
    namespace = "mega.privacy.android.shared.account"
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {
    lintChecks(project(":lint"))

    implementation(project(":domain"))
    implementation(project(":resources:string-resources"))
    implementation(project(":resources:icon-pack"))

    //core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    implementation(project(":core:analytics:analytics-tracker"))
    implementation(project(":core:navigation-contract"))
    implementation(lib.mega.analytics)

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)

    implementation(androidx.hilt.navigation)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)

    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(project(":core:analytics:analytics-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
