import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.data.test"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(lib.coroutines.core)

    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.truth.ext)
    testImplementation(lib.bundles.unit.test)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
