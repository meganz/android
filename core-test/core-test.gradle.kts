plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
}

android {
    namespace = "mega.privacy.android.core.test"

    packaging {
        resources.excludes.add("/META-INF/*")
    }
}

dependencies {
    // Coroutines
    implementation(lib.coroutines.test)

    implementation(project(":core:analytics:analytics-tracker"))
    // JUnit5
    implementation(platform(testlib.junit5.bom))
    implementation(testlib.junit.test.ktx)
    implementation(testlib.junit.jupiter.api)
    implementation(lib.mega.analytics)

    // Weblate screenshot-test helpers (fleeting UI: snackbars, content descriptions)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)
}
