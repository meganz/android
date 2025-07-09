plugins {
    alias(convention.plugins.mega.android.library)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.shared.resources"

    dependencies {
        lintChecks(project(":lint"))
    }
}
