plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.hilt)
}

android {
    namespace = "mega.privacy.android.core.formatter"
}

dependencies {
    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}