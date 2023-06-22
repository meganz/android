plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("de.mannodermaus.android-junit5")
}

android {
    namespace = "mega.privacy.android.analytics"
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
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        // Sets Java compatibility to JavaVersion in Project Root
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

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }

}

dependencies {
    lintChecks(project(":lint"))
    implementation(project(":domain"))

    // DI
    implementation(lib.javax.inject)
    kapt(google.hilt.android.compiler)
    implementation(google.hilt.core)
    implementation(google.hilt.android)

    // Framework
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
    implementation(lib.kotlin.ktx)
    implementation(androidx.appcompat)

    // Logging
    implementation(lib.bundles.logging)

    // Testing
    testImplementation(testlib.junit)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
    testImplementation(testlib.junit.test.ktx)

    testImplementation(testlib.bundles.unit.test)

    testImplementation(testlib.compose.junit)
    testImplementation(testlib.mockito)
    testImplementation(testlib.mockito.kotlin)
    testImplementation(testlib.mockito.android)
}