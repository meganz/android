plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.lint)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.shared.resources"

    dependencies {
        lintChecks(project(":lint"))
    }
}
