plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.xray"

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

    buildFeatures {
        compose = true
    }

    compileOptions {
        val javaVersion: JavaVersion by rootProject.extra
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
    }

    kotlin {
        val jdk: String by rootProject.extra
        jvmToolchain(jdk.toInt())
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }
}

dependencies {
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
}
