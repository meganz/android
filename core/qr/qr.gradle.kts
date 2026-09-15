plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
}

android {
    namespace = "mega.privacy.android.core.qr"
}

dependencies {
    implementation(google.zxing)
    implementation(lib.kotlin.ktx)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)

    //test
    testImplementation(project(":core-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
