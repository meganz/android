plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.core.coroutine"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(lib.coroutines.core)
    implementation(lib.logging.timber)

    //test
    testImplementation(project(":core-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
