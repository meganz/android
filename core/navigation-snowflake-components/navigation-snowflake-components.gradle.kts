plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.test)
    alias(convention.plugins.mega.android.library.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.navigation.snowflake"
}

dependencies {
    implementation(project(":core:navigation-contract"))
    implementation(platform(androidx.compose.bom))
    implementation(androidx.navigation.compose)
    implementation(androidx.material3.adaptive.navigation.suite)
    implementation(lib.kotlinx.collections.immutable)
    implementation(lib.logging.timber)

    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)
}