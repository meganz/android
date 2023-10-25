plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "mega.privacy.android.core.ui.test"
    val compileSdkVersion: Int by rootProject.extra
    compileSdk = compileSdkVersion
    val buildTools: String by rootProject.extra
    buildToolsVersion = buildTools

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    defaultConfig {
        val minSdkVersion: Int by rootProject.extra
        minSdk = minSdkVersion

        val targetSdkVersion: Int by rootProject.extra
        targetSdk = targetSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        val javaVersion: JavaVersion by rootProject.extra
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    kotlin {
        val jdk: String by rootProject.extra
        jvmToolchain(jdk.toInt())
    }

    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
        val shouldSuppressWarnings: Boolean by rootProject.extra
        suppressWarnings = shouldSuppressWarnings
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
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