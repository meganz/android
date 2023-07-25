plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets.getByName("main") {
        java.srcDirs(
            "src/main/jni/mega/sdk/bindings/java",
            "src/main/jni/megachat/sdk/bindings/java"
        )
        java.exclude("**/MegaApiSwing.java")
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

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
    namespace = "nz.mega.sdk"
}

dependencies {
    implementation(androidx.exifinterface)
    implementation(files("src/main/jni/megachat/webrtc/libwebrtc.jar"))

    testImplementation(testlib.junit)
    androidTestImplementation(testlib.junit.test.ktx)
    androidTestImplementation(testlib.espresso)
}
