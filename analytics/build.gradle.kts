import groovy.lang.Closure
import mega.privacy.android.build.shouldApplyDefaultConfiguration

plugins {
    alias(convention.plugins.mega.android.library)
    id("kotlin-kapt")
    id("de.mannodermaus.android-junit5")
}

android {
    namespace = "mega.privacy.android.analytics"

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }

}

dependencies {
    lintChecks(project(":lint"))
    implementation(project(":domain"))

    // Analytics
    implementation(lib.mega.analytics)

    // DI
    implementation(lib.javax.inject)

    if (shouldApplyDefaultConfiguration(project)) {
        kapt(google.hilt.android.compiler)
    }

    implementation(google.hilt.core)
    implementation(google.hilt.android)

    // Framework
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
    implementation(lib.kotlin.ktx)
    implementation(androidx.appcompat)

    // Logging
    implementation(lib.bundles.logging)

    // Testing
    testImplementation(testlib.junit)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
    testImplementation(testlib.junit.test.ktx)

    testImplementation(testlib.bundles.unit.test)

    testImplementation(testlib.compose.junit)
    testImplementation(testlib.mockito)
    testImplementation(testlib.mockito.kotlin)
    testImplementation(testlib.mockito.android)
}