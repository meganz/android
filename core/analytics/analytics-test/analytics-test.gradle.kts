plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "mega.privacy.android.analytics.test"
}

dependencies {

    implementation(project(":core:analytics:analytics-tracker"))
    implementation(lib.mega.analytics)

    implementation(platform(testlib.junit5.bom))
    implementation(testlib.junit.test.ktx)
    implementation(testlib.junit.jupiter.api)

    testImplementation(testlib.truth)
}