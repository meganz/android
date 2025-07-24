plugins {
    alias(convention.plugins.mega.android.library)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.navigation.contract"
}

dependencies {
    implementation(androidx.navigation.compose)
    implementation(androidx.navigation3.runtime)
    implementation(lib.mega.analytics)
}