import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
}

android {
    lint {
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.sharelink"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":core:navigation-contract"))
    implementation(project(":navigation"))
    implementation(project(":domain"))
    implementation(project(":core:coroutine"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))

    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(androidx.navigation3.runtime)

    // Compose
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.compose.viewmodel)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)
    implementation(androidx.hilt.navigation)
    implementation(lib.compose.state.events)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
