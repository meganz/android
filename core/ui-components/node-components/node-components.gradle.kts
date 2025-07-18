plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.core.nodecomponents"
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":icon-pack"))
    implementation(project(":shared:resources"))

    //core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    // Compose
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)
    implementation(androidx.material3.adaptive)
    implementation(androidx.material3.adaptive.layout)
    implementation(androidx.material3.adaptive.navigation)
    implementation(androidx.material3.window)
    implementation(lib.coil3)
    implementation(lib.coil.compose)

    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}