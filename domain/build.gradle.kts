import mega.privacy.android.build.shouldApplyDefaultConfiguration

plugins {
    alias(convention.plugins.mega.jvm.library)
    alias(convention.plugins.mega.jvm.test)
    alias(convention.plugins.mega.jvm.jacoco)
    alias(convention.plugins.mega.lint)
    alias(convention.plugins.mega.jvm.hilt)
    kotlin("plugin.serialization") version "1.9.21"
}

lint {
    abortOnError = true
    warningsAsErrors = true
}

dependencies {
    lintChecks(project(":lint"))
    implementation(lib.coroutines.core)
    implementation(lib.javax.inject)

    implementation(androidx.paging.core)

    implementation(lib.kotlin.serialisation)

    // Testing dependencies
    testImplementation(testlib.bundles.unit.test)
    testImplementation(lib.bundles.unit.test)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}