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
    namespace = "mega.privacy.android.feature.fileinfo"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":core:navigation-contract"))
    implementation(project(":navigation"))
    implementation(project(":domain"))
    implementation(project(":core:formatter"))
    implementation(project(":shared:nodes"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))
    implementation(lib.mega.core.ui)
    implementation(lib.mega.analytics)

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)

    // Navigation3
    implementation(androidx.navigation3.runtime)
    implementation(androidx.navigation3.ui)
    implementation(androidx.hilt.navigation)

    // Compose
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.compose.viewmodel)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
