import mega.privacy.android.build.preBuiltSdkDependency
import mega.privacy.android.build.shouldApplyDefaultConfiguration

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.room)
    alias(convention.plugins.mega.android.test)
    alias(convention.plugins.mega.android.library.jacoco)
    alias(convention.plugins.mega.lint)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.de.mannodermaus.android.junit5)
    id("kotlin-android")
}

android {
    lint {
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.sync"
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
    implementation(google.guava)

    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(androidx.work.test)
}
