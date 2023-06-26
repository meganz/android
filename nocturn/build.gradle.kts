plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "mega.privacy.android.nocturn"
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

    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    kotlin {
        val jdk: String by rootProject.extra
        jvmToolchain(jdk.toInt())
    }
}

dependencies {
    implementation(lib.kotlin.ktx)
}
