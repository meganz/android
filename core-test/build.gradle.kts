plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.core.test"

    packaging {
        resources.excludes.add("/META-INF/*")
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
