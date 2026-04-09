plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    id("kotlin-android")
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
    implementation(lib.logging.timber)
    implementation(androidx.appcompat)
    implementation(androidx.lifecycle.runtime)
    implementation(androidx.compose.activity)
    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))

    testImplementation(project(":core-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
