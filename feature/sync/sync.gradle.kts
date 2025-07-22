import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.room)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.de.mannodermaus.android.junit5)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    lint {
        abortOnError = true
    }
    defaultConfig {
        testInstrumentationRunner = "mega.privacy.android.app.HiltTestRunner"
    }
    namespace = "mega.privacy.android.feature.sync"
}

dependencies {
    implementation(project(":navigation"))
    implementation(project(":core:ui-components:node-components"))
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
    implementation(project(":core:analytics:analytics-tracker"))

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(lib.mega.analytics)

    implementation(google.gson)
    implementation(androidx.datastore.preferences)
    implementation(androidx.hilt.navigation)

    implementation(androidx.appcompat)
    implementation(androidx.fragment)
    implementation(google.material)
    implementation(google.accompanist.permissions)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.lifecycle.service)
    implementation(androidx.compose.activity)
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.work.ktx)
    implementation(androidx.hilt.work)
    implementation(lib.compose.state.events)
    implementation(lib.kotlin.serialisation)
    implementation(google.guava)

    testImplementation(project(":core:analytics:analytics-test"))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(google.hilt.android.test)
    testImplementation(androidx.material3)
    testImplementation(androidx.work.test)
}
