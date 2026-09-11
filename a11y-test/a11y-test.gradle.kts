plugins {
    alias(convention.plugins.mega.android.a11y.test)
}

android {
    namespace = "mega.privacy.android.a11ytest"
}

dependencies {
    // The harness is consumed from other modules' androidTest source sets, so everything that
    // appears in its own public signatures has to be exposed rather than hidden.
    api(platform(androidx.compose.bom))
    api(testlib.androidx.compose.ui.test.junit4)
    api(testlib.androidx.compose.ui.test.junit4.accessibility)
    api(testlib.hamcrest)

    implementation(androidx.compose.foundation)
    implementation(androidx.compose.ui)
    implementation(lib.mega.core.ui)
    implementation(testlib.test.storage)
    // The run mode is read from the instrumentation runner arguments.
    implementation(testlib.test.monitor)
    implementation(google.guava)

    androidTestImplementation(androidx.compose.ui)
    androidTestImplementation(androidx.compose.foundation)
    androidTestImplementation(androidx.material3)
    // The component-family sweeps exercise the prebuilt core-ui artifact, which belongs to no
    // feature module. Screen-level sweeps mount stateless feature-module screens directly, so
    // the modules that own them and the modules declaring their UI-state types are compile-time
    // dependencies until each of those screens is swept by its own module.
    androidTestImplementation(project(":feature:settings"))
    androidTestImplementation(project(":feature:transfers:transfers"))
    androidTestImplementation(project(":feature:payment:payment-snowflake-components"))
    androidTestImplementation(project(":domain"))
    androidTestImplementation(project(":core:analytics:analytics-test"))
    androidTestImplementation(project(":resources:string-resources"))
    androidTestImplementation(lib.mega.core.ui)
    androidTestImplementation(lib.kotlinx.collections.immutable)
    androidTestImplementation(testlib.truth)
}
