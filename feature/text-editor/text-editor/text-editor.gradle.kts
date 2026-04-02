plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    lint {
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.texteditor"
}

dependencies {
    implementation(project(":core:navigation-contract"))
    implementation(project(":domain"))
    implementation(project(":navigation"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))

    implementation(platform(androidx.compose.bom))
    implementation(androidx.compose.material)
    implementation(androidx.compose.icons)
    implementation(androidx.hilt.navigation)
    implementation(androidx.material3)
    implementation(androidx.navigation3.runtime)
    implementation(lib.compose.state.events)
    implementation(lib.kotlin.serialisation)
    implementation(lib.logging.timber)
    implementation(project(":feature:text-editor:text-editor-snowflake-components"))
    implementation(lib.mega.core.ui)

    testImplementation(project(":core-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
}
