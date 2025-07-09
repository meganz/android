plugins {
    alias(convention.plugins.mega.android.library)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.navigation.contract"
}

dependencies {
    implementation(androidx.navigation.compose)
    implementation(lib.mega.analytics)
}