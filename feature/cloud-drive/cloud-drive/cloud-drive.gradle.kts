import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    lint {
        disable += "CoroutineCreationDuringComposition"
        abortOnError = true
    }
    namespace = "mega.privacy.android.feature.clouddrive"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":feature:cloud-drive:cloud-drive-snowflake-components"))
    implementation(project(":core:ui-components:node-components"))
    implementation(project(":core:analytics:analytics-tracker"))
    implementation(project(":shared:resources"))
    implementation(project(":icon-pack"))
    implementation(project(":core:navigation-contract"))
    implementation(project(":domain"))
    implementation(lib.mega.core.ui)
    implementation(lib.mega.analytics)
    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(androidx.appcompat)
    implementation(google.material)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.compose.activity)
    implementation(androidx.compose.viewmodel)
    implementation(androidx.bundles.compose.bom)
    implementation(lib.compose.state.events)
    implementation(androidx.hilt.navigation)
    implementation(platform(androidx.compose.bom))
    implementation(androidx.material3.adaptive.navigation.suite)
    implementation(lib.kotlin.serialisation)
}