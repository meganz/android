plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.feature_flags"
}

dependencies {
    implementation(project(":domain"))
}