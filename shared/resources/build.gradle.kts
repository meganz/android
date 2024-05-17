plugins {
    alias(convention.plugins.mega.android.library)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.shared.resources"

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }

    dependencies {
        lintChecks(project(":lint"))
        implementation(project(":shared:original-core-ui"))
    }
}
