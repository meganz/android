import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-parcelize")
}

android {
    namespace = "mega.privacy.android.feature.mediaplayer"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":feature:media-player:media-player-snowflake-components"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:coroutine"))
    implementation(project(":core:ui-components:node-components"))
    implementation(project(":core:ui-components:shared-components"))
    implementation(project(":core:analytics:analytics-tracker"))
    implementation(project(":resources:string-resources"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":shared:nodes"))
    implementation(project(":shared:original-core-ui"))
    implementation(project(":domain"))

    implementation(lib.mega.core.ui)
    implementation(lib.mega.analytics)
    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(lib.coil3)
    implementation(lib.coil.compose)
    implementation(google.media3.ui)
    implementation(google.media3.common)
    implementation(androidx.material3)
    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.hilt.navigation)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.navigation3.ui)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
