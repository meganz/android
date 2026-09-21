plugins {
    // `id` rather than `alias`: :domain already applies these, so the child project sees them
    // on the classpath with an unknown version and version-bearing aliases fail to resolve.
    id("mega.jvm.library")
    id("mega.jvm.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
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
