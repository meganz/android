plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
}

android {
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
    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.material3)

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
