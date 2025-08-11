plugins {
    alias(convention.plugins.mega.android.library)
    alias(plugin.plugins.kotlin.serialisation)
    alias(convention.plugins.mega.android.hilt)
    alias(convention.plugins.mega.android.library.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.navigation"
}

dependencies {
    implementation(project(":domain"))
    implementation(androidx.appcompat)
    implementation(androidx.navigation.compose)
    implementation(androidx.navigation3.runtime)
    implementation(lib.mega.analytics)

    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.arch.core.test)
    testImplementation(testlib.test.core.ktx)
    testImplementation(testlib.junit)
    testImplementation(testlib.junit.test.ktx)
}