plugins {
    id("com.android.library")
}

android {
    namespace = "com.jeremyliao.liveeventbus"
    val compileSdkVersion: Int by rootProject.extra
    compileSdk = compileSdkVersion

    defaultConfig {
        val minSdkVersion: Int by rootProject.extra
        minSdk = minSdkVersion
        val targetSdkVersion: Int by rootProject.extra
        targetSdk = targetSdkVersion
    }
}

dependencies {
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.java.core)
}
