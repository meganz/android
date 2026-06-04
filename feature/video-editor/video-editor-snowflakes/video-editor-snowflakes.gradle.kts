plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
}

android {
    namespace = "mega.privacy.android.feature.videoeditor.components"
    lint {
        abortOnError = true
    }
}

dependencies {
    lintChecks(project(":lint"))

    //core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)
    implementation(lib.logging.timber)

    // Compose
    implementation(androidx.material3)
    implementation(androidx.compose.icons.extended)
    implementation(androidx.compose.ui.tooling.preview)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
