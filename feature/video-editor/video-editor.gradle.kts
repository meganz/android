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

    implementation(project(":core:navigation-contract"))
    implementation(project(":navigation"))
    implementation(project(":core:ui-components:shared-components"))
    implementation(project(":domain"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))

    implementation(lib.mega.core.ui)

    implementation(androidx.appcompat)
    implementation(androidx.compose.activity)
    implementation(androidx.hilt.navigation)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.navigation3.ui)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
