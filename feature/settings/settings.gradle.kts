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
    namespace = "mega.privacy.android.feature.settings"
}

dependencies {
    lintChecks(project(":lint"))

    implementation(project(":core:navigation-contract"))
    implementation(project(":domain"))
    implementation(project(":navigation"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))

    implementation(lib.mega.core.ui)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.hilt.navigation)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.navigation3.runtime)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
