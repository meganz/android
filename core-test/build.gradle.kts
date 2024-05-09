plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.core.test"

    packaging {
        resources.excludes.add("/META-INF/*")
    }
}

dependencies {
    // Coroutines
    implementation(lib.coroutines.test)

    // JUnit5
    implementation(platform(testlib.junit5.bom))
    implementation(testlib.junit.jupiter.api)
}
