import java.util.Properties

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
}

android {
    namespace = "mega.privacy.android.feature.signin.external"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Read the OAuth Web Client ID from local.properties so it never gets
        // committed. Set `googleServerClientId=...apps.googleusercontent.com`
        // in your local.properties to enable Google Sign-In. Falls back to a
        // placeholder so unconfigured builds still compile.
        val googleServerClientId = rootProject.file("local.properties")
            .takeIf { it.exists() }
            ?.let { file ->
                Properties().apply { load(file.inputStream()) }
                    .getProperty("googleServerClientId")
            }
            ?: "placeholder.apps.googleusercontent.com"
        buildConfigField(
            "String",
            "GOOGLE_SERVER_CLIENT_ID",
            "\"$googleServerClientId\""
        )
    }
}

dependencies {
    // Lint checks
    lintChecks(project(":lint"))

    // Project modules
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core:feature-flags"))
    implementation(project(":resources:string-resources"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":core:ui-components:shared-components"))
    implementation(lib.mega.core.ui)

    // Google Sign-In / Credential Manager
    implementation(androidx.credentials)
    implementation(androidx.credentials.play)
    implementation(google.identity.googleid)

    // Compose
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.compose.activity)
    implementation(androidx.material3)

    // Common
    implementation(lib.logging.timber)
    implementation(lib.compose.state.events)
    implementation(lib.kotlin.serialisation)

    // Testing
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
