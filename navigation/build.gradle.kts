plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.test)
}

android {
    namespace = "mega.privacy.android.navigation"
}

dependencies {
    implementation(project(":domain"))
    implementation(androidx.appcompat)

    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.arch.core.test)
    testImplementation(testlib.test.core.ktx)
    testImplementation(testlib.junit)
    testImplementation(testlib.junit.test.ktx)
}