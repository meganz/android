plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    lint {
        disable += "CoroutineCreationDuringComposition"
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.home"
}

dependencies {
    implementation(project(":core:navigation-contract"))
    implementation(project(":domain"))
    implementation(project(":icon-pack"))
    implementation(project(":shared:resources"))

    implementation(platform(androidx.compose.bom))
    implementation(androidx.hilt.navigation)
    implementation(androidx.material3.adaptive.navigation.suite)
    implementation(androidx.navigation3.runtime)
    implementation(lib.kotlin.serialisation)
    implementation(lib.mega.analytics)
    implementation(lib.mega.core.ui)
    implementation(lib.logging.timber)

    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}