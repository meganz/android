plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.room)
    alias(convention.plugins.mega.android.test)
    alias(convention.plugins.mega.android.library.jacoco)
    id("kotlin-android")
    id("kotlin-kapt")
    id("de.mannodermaus.android-junit5")
}

android {
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
    namespace = "mega.privacy.android.feature.example_new_components"
}

dependencies {
    implementation(project(":icon-pack"))
    implementation(lib.mega.core.ui)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)

}