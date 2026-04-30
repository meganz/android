plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.shared.resources"

    dependencies {
        lintChecks(project(":lint"))
        implementation(androidx.annotation)
    }
}
