import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    lint {
        abortOnError = true
    }
    namespace = "mega.privacy.android.core.transfers"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":icon-pack"))
    implementation(project(":shared:resources"))
    implementation(project(":feature:transfers:transfers-snowflake-components"))
    implementation(project(":core:formatter"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":domain"))
    implementation(project(":navigation"))
    implementation(lib.mega.core.ui)
    implementation(lib.mega.analytics)
    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(androidx.appcompat)

    implementation(google.material)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.compose.activity)
    implementation(androidx.compose.viewmodel)
    implementation(androidx.bundles.compose.bom)
    implementation(lib.compose.state.events)
    implementation(androidx.hilt.navigation)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.material3.adaptive.navigation.suite)
    implementation(lib.kotlin.serialisation)
    implementation(androidx.navigation3.runtime)

    // test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(androidx.navigation.testing)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(google.hilt.android.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}