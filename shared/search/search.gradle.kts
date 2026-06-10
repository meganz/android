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
    namespace = "mega.privacy.android.shared.search"
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }
}

dependencies {
    lintChecks(project(":lint"))
    preBuiltSdkDependency(rootProject.extra)

    implementation(project(":resources:string-resources"))
    implementation(project(":resources:icon-pack"))

    //core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)

    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)

    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
