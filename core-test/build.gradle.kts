plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "mega.privacy.android.core.test"
    val compileSdkVersion: Int by rootProject.extra
    compileSdk = compileSdkVersion
    val buildTools: String by rootProject.extra
    buildToolsVersion = buildTools

    defaultConfig {
        val minSdkVersion: Int by rootProject.extra
        minSdk = minSdkVersion

        val targetSdkVersion: Int by rootProject.extra
        targetSdk = targetSdkVersion
    }

    compileOptions {
        val javaVersion: JavaVersion by rootProject.extra
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
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
    // Coroutines
    implementation(lib.coroutines.test)

    // JUnit5
    implementation(platform(testlib.junit5.bom))
    implementation(testlib.junit.jupiter.api)
}
