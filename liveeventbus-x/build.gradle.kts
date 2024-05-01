plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "com.jeremyliao.liveeventbus"
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
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.java.core)
}
