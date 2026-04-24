import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.de.mannodermaus.android.junit5)
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
    namespace = "mega.privacy.android.shared.ads"
    testOptions {
        unitTests {
            targetSdk = 34
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "AD_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField(
                "String",
                "REWARDED_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/5224354917\""
            )
        }
        release {
            buildConfigField("String", "AD_UNIT_ID", "\"ca-app-pub-2135147798858967/9835644604\"")
            buildConfigField(
                "String",
                "REWARDED_AD_UNIT_ID",
                "\"ca-app-pub-2135147798858967/1942333326\""
            )
        }
    }
}

//  Prevent the old play-services-ads from being pulled in transitively by other dependencies
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    implementation(project(":core:feature-flags"))
    implementation(project(":core:analytics:analytics-tracker"))
    lintChecks(project(":lint"))
    preBuiltSdkDependency(rootProject.extra)

    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":navigation"))
    implementation(project(":core:formatter"))
    implementation(project(":resources:string-resources"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":core:ui-components:shared-components"))
    implementation(project(":core:analytics:analytics-tracker"))

    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    implementation(lib.kotlin.ktx)
    implementation(lib.logging.timber)
    implementation(lib.mega.analytics)

    implementation(google.gson)
    implementation(google.ads.mobile.sdk)
    implementation(androidx.datastore.preferences)
    implementation(androidx.hilt.navigation)

    implementation(androidx.appcompat)
    implementation(androidx.fragment)
    implementation(google.material)
    implementation(google.accompanist.permissions)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.bundles.compose.bom)
    implementation(lib.compose.state.events)
    implementation(lib.kotlin.serialisation)
    implementation(lib.kotlinx.collections.immutable)
    implementation(google.guava)
    implementation(androidx.material3)
    implementation(androidx.navigation3.runtime)
    implementation(lib.coil3)
    implementation(lib.coil.compose)

    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(project(":core:analytics:analytics-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(google.hilt.android.test)
    testImplementation(androidx.material3)
    testImplementation(androidx.work.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
