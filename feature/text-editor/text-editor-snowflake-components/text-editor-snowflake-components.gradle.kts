plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(plugin.plugins.compose.screenshot)
}

android {
    namespace = "mega.privacy.android.feature.texteditor.components"
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
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
    implementation(androidx.compose.icons.extended)
    implementation(lib.kotlinx.collections.immutable)
    implementation(lib.logging.timber)

    // Markdown parsing for the in-house Compose reader (CommonMark + GFM tables). No UI dep.
    implementation(lib.commonmark.java)
    implementation(lib.commonmark.ext.gfm.tables)
    implementation(lib.commonmark.ext.gfm.strikethrough)
    implementation(lib.commonmark.ext.task.list.items)
    implementation(lib.javax.inject)

    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    // screenshot tests
    screenshotTestImplementation(platform(androidx.compose.bom))
    screenshotTestImplementation(androidx.compose.ui.tooling)
    screenshotTestImplementation(testlib.compose.screenshot)
}
