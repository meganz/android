plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("de.mannodermaus.android-junit5")
    id("dagger.hilt.android.plugin")
}

apply(from = "${project.rootDir}/tools/util.gradle")
apply(from = "${project.rootDir}/tools/sdk.gradle")

android {
    namespace = "mega.privacy.android.feature.sync"

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
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    compileOptions {
        val javaVersion: JavaVersion by rootProject.extra
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk

        val shouldSuppressWarnings: Boolean by rootProject.extra
        suppressWarnings = shouldSuppressWarnings

        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    kotlin {
        val jdk: String by rootProject.extra
        jvmToolchain(jdk.toInt())
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
}

dependencies {
    testImplementation(project(":core-ui-test"))
    lintChecks(project(":lint"))

    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core:formatter"))
    implementation(project(":core-ui"))

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)

    implementation(google.hilt.android)
    implementation(androidx.hilt.navigation)
    kapt(google.hilt.android.compiler)
    kapt(androidx.hilt.compiler)

    implementation(androidx.appcompat)
    implementation(androidx.fragment)
    implementation(google.material)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.lifecycle.service)
    implementation(androidx.compose.activity)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
    implementation(lib.compose.state.events)
    implementation(google.accompanist.navigationanimation)

    testImplementation(testlib.junit)
    testImplementation(testlib.junit.test.ktx)
    testImplementation(testlib.espresso)
    testImplementation(testlib.compose.junit)
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.arch.core.test)
    testImplementation(testlib.test.core.ktx)
    testImplementation(testlib.mockito)
    testImplementation(testlib.mockito.kotlin)
    testImplementation(testlib.mockito.android)

    testRuntimeOnly(testlib.junit.jupiter.engine)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
}
