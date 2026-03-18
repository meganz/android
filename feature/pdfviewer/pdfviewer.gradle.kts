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
        disable += listOf("CoroutineCreationDuringComposition", "ComposeUnstableCollections")
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.pdfviewer"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    // Lint checks
    lintChecks(project(":lint"))
    lintChecks(lib.slack.compose.lints)

    // Project modules
    implementation(project(":core:ui-components:node-components"))
    implementation(project(":core:ui-components:shared-components"))
    implementation(project(":core:feature-flags"))
    implementation(project(":resources:string-resources"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":navigation"))
    implementation(project(":domain"))

    // PDF search engine uses PdfiumCore directly
    implementation(project(":third-party-lib:pdfiumAndroid"))

    // Core UI
    implementation(lib.mega.core.ui)

    // Kotlin
    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)

    // AndroidX
    implementation(androidx.appcompat)
    implementation(google.material)

    // Lifecycle
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)

    // Compose
    implementation(androidx.compose.activity)
    implementation(androidx.compose.viewmodel)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.material3)

    // State events (for EventEffect)
    implementation(lib.compose.state.events)

    // Hilt navigation
    implementation(androidx.hilt.navigation)

    // Serialization
    implementation(lib.kotlin.serialisation)

    // Navigation3
    implementation(androidx.navigation3.runtime)
    implementation(androidx.navigation3.ui)

    // Testing
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(google.hilt.android.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
