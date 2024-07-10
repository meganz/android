plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.test)
    alias(convention.plugins.mega.android.library.jacoco)
    alias(convention.plugins.mega.lint)
    alias(convention.plugins.mega.android.hilt)
    id("kotlin-android")
}

android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        abortOnError = true
    }

    namespace = "mega.privacy.android.legacy.core.ui"
}

dependencies {
    lintChecks(project(":lint"))

    implementation(project(":icon-pack"))
    implementation(project(":shared:original-core-ui"))
    testImplementation(project(":core-ui-test"))

    implementation(androidx.constraintlayout.compose)
    implementation(androidx.bundles.compose.bom)
    implementation(lib.kotlin.ktx)
    implementation(androidx.appcompat)
    implementation(google.material)
    implementation(google.accompanist.systemui)
    implementation(google.accompanist.permissions)
    implementation(androidx.compose.activity)
    implementation(androidx.lifecycle.runtime)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(lib.compose.state.events)
    implementation(lib.coil.compose)
    implementation(lib.balloon)
    implementation(google.accompanist.placeholder)

    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)

    debugImplementation(lib.kotlinpoet)
    debugImplementation(google.gson)
}