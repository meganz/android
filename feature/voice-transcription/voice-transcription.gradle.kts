import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
}

android {
    lint {
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.voicetranscription"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)
    lintChecks(project(":lint"))

    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":core:formatter"))
    implementation(project(":core:coroutine"))
    implementation(project(":navigation"))
    implementation(project(":shared:original-core-ui"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))

    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)
    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(lib.coroutines.core)
    implementation(lib.coroutines.android)

    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.compose.activity)
    implementation(androidx.compose.viewmodel)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.hilt.navigation)
    implementation(androidx.navigation3.runtime)
    implementation(androidx.material3)

    testImplementation(project(":core-test"))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(testlib.test.core.ktx)
}
