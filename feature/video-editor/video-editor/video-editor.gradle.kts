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
    namespace = "mega.privacy.android.feature.videoeditor"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":feature:video-editor:video-editor-snowflakes"))
    implementation(project(":core:formatter"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":navigation"))
    implementation(project(":core:ui-components:shared-components"))
    implementation(project(":domain"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))

    // core-ui brings the Compose runtime/foundation; this module uses no
    // DSTokens/core-ui components directly — those live in the snowflakes module.
    implementation(lib.mega.core.ui)
    implementation(lib.logging.timber)
    implementation(lib.compose.state.events)
    implementation(lib.coil3)
    implementation(lib.coil.compose)

    // Full Media3 stack — the editor engine + preview (ExoPlayer/Transformer/
    // PlayerSurface) live in this module.
    implementation(google.bundles.media3)

    implementation(androidx.material3)
    implementation(androidx.compose.icons.extended)

    implementation(androidx.appcompat)
    implementation(androidx.compose.activity)
    implementation(androidx.hilt.navigation)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.navigation3.ui)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(testlib.roboelectric)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
