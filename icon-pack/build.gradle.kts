plugins {
    alias(convention.plugins.mega.android.library)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.icon.pack"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
}

dependencies {
    lintChecks(project(":lint"))
    implementation(lib.kotlin.ktx)
}