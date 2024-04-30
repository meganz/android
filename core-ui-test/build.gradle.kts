plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.core.ui.test"

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(testlib.bundles.ui.test)
    implementation(testlib.bundles.unit.test)
    implementation(testlib.compose.junit)

    // Compose testing dependencies
    implementation(platform(androidx.compose.bom))
    androidTestImplementation(platform(androidx.compose.bom))
    androidTestImplementation(testlib.androidx.compose.ui.test)
    androidTestImplementation(testlib.androidx.compose.ui.test.junit4)
    debugImplementation(testlib.androidx.compose.ui.testManifest)

}