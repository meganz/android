plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("app.cash.paparazzi")
}

android {
    namespace = "mega.privacy.android.screenshot"

    val compileSdkVersion: Int by rootProject.extra
    compileSdk = compileSdkVersion
    val buildTools: String by rootProject.extra
    buildToolsVersion = buildTools

    defaultConfig {
        val minSdkVersion: Int by rootProject.extra
        minSdk = minSdkVersion
        val targetSdkVersion: Int by rootProject.extra
        targetSdk = targetSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
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
    implementation(project(":core-ui"))

    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)

    implementation(lib.compose.state.events)

    testImplementation(testlib.junit)
    testImplementation(testlib.test.parameter.injector)

    debugImplementation(androidx.compose.ui.tooling)
    debugImplementation(androidx.compose.ui.tooling.preview)
}
