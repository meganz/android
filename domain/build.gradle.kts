import mega.privacy.android.build.shouldApplyDefaultConfiguration

plugins {
    alias(convention.plugins.mega.jvm.library)
    alias(convention.plugins.mega.jvm.test)
    alias(convention.plugins.mega.jvm.jacoco)
    id("com.android.lint")
    id("kotlin-kapt")
    kotlin("plugin.serialization") version "1.9.21"
}

lint {
    abortOnError = false
    xmlOutput = file("build/reports/lint-results.xml")
}

dependencies {
    lintChecks(project(":lint"))
    implementation(lib.coroutines.core)
    implementation(lib.javax.inject)

    implementation(google.hilt.core)
    implementation(androidx.paging.core)

    implementation(lib.kotlin.serialisation)

    if (shouldApplyDefaultConfiguration(project)) {
        kapt(google.hilt.android.compiler)
    }

    // Testing dependencies
    testImplementation(testlib.bundles.unit.test)
    testImplementation(lib.bundles.unit.test)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}