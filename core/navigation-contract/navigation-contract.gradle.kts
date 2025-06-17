plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.test)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.navigation.contract"
}

dependencies {
    implementation(androidx.navigation.compose)
}