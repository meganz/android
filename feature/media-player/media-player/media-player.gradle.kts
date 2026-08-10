import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
}

android {
    namespace = "mega.privacy.android.feature.mediaplayer"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":feature:media-player:media-player-snowflake-components"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":resources:string-resources"))
    implementation(project(":domain"))

    implementation(lib.mega.core.ui)
    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.hilt.navigation)
    implementation(androidx.navigation3.runtime)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
