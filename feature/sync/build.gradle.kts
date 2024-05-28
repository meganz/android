import mega.privacy.android.build.preBuiltSdkDependency
import mega.privacy.android.build.shouldApplyDefaultConfiguration

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.room)
    alias(convention.plugins.mega.android.test)
    alias(convention.plugins.mega.android.library.jacoco)
    id("kotlin-android")
    id("kotlin-kapt")
    id("de.mannodermaus.android-junit5")
}

android {
    namespace = "mega.privacy.android.feature.sync"

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
}

dependencies {
    implementation(project(":navigation"))
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    lintChecks(project(":lint"))
    preBuiltSdkDependency(rootProject.extra)

    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core:formatter"))
    implementation(project(":shared:original-core-ui"))
    implementation(project(":shared:sync"))
    implementation(project(":shared:resources"))
    implementation(project(":legacy-core-ui"))
    implementation(project(":icon-pack"))
    implementation(project(":analytics"))

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(lib.mega.analytics)

    implementation(google.hilt.android)
    implementation(google.gson)
    implementation(androidx.datastore.preferences)
    implementation(androidx.hilt.navigation)

    if (shouldApplyDefaultConfiguration(project)) {
        apply(plugin = "dagger.hilt.android.plugin")

        kapt(google.hilt.android.compiler)
        kapt(androidx.hilt.compiler)
    }

    implementation(androidx.appcompat)
    implementation(androidx.fragment)
    implementation(google.material)
    implementation(google.accompanist.permissions)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.lifecycle.service)
    implementation(androidx.compose.activity)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
    implementation(lib.compose.state.events)

    testImplementation(testlib.compose.junit)
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
}
