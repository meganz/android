plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
}

android {
    namespace = "mega.privacy.android.feature.mediaplayer.components"
}

dependencies {
    implementation(project(":resources:string-resources"))
    implementation(project(":shared:original-core-ui"))

    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.material3)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
