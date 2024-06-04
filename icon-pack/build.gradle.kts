plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.lint)
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
}

dependencies {
    lintChecks(project(":lint"))
    implementation(lib.kotlin.ktx)
}