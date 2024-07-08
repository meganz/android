import com.android.build.api.dsl.ManagedVirtualDevice
import mega.privacy.android.build.getTestAccountPassword
import mega.privacy.android.build.getTestAccountUserName

plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    id("androidx.baselineprofile")
}

android {
    namespace = "mega.privacy.android.app.baselineprofile"
    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "TEST_ACCOUNT_USER_NAME",
            getTestAccountUserName(project) ?: "\"\""
        )
        buildConfigField(
            "String",
            "TEST_ACCOUNT_PASSWORD",
            getTestAccountPassword(project) ?: "\"\""
        )
    }

    targetProjectPath = ":app"

    flavorDimensions += listOf("service")
    productFlavors {
        create("gms") { dimension = "service" }
    }

    testOptions.managedDevices.devices {
        create<ManagedVirtualDevice>("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "google"
        }
    }
}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.2.4")
}