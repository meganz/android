plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.room)
    alias(convention.plugins.mega.android.test)
    alias(convention.plugins.mega.android.library.jacoco)
    alias(convention.plugins.mega.lint)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.de.mannodermaus.android.junit5)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.feature.example_new_components"
}

dependencies {
    implementation(project(":icon-pack"))
    implementation(lib.mega.core.ui)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)

}