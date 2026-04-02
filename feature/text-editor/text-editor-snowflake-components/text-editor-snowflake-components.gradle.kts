plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.feature.texteditor.components"
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {

    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))

    //core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    // Compose
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)
    implementation(lib.logging.timber)

    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
