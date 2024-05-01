plugins {
    alias(convention.plugins.mega.android.library)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.shared.resources"

    kotlin {
        val jdk: String by rootProject.extra
        jvmToolchain(jdk.toInt())
    }

    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
        val shouldSuppressWarnings: Boolean by rootProject.extra
        suppressWarnings = shouldSuppressWarnings
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }

    dependencies {
        lintChecks(project(":lint"))
        implementation(project(":core-ui"))
    }
}
