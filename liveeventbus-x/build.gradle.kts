plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "com.jeremyliao.liveeventbus"
}

dependencies {
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.java.core)
}
