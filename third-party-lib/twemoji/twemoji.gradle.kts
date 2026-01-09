plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    lint {
        abortOnError = true
    }
    namespace = "mega.privacy.android.thirdpartylib.twemoji"
    testOptions {
        unitTests {
            targetSdk = 36
        }
    }

    sourceSets {
        getByName("main") {
            java {
                srcDirs("src/main/kotlin")
            }
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":shared:resources"))
    implementation(project(":icon-pack"))

    implementation(androidx.bundles.compose.bom)
    implementation(platform(androidx.compose.bom))
    implementation(lib.kotlin.serialisation)
    implementation(lib.mega.core.ui)
    implementation(lib.logging.timber)
    implementation(lib.namedregexp)
    implementation(testlib.hamcrest)
    implementation(androidx.appcompat)
    implementation(google.gson)
    implementation(androidx.emoji2)

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(testlib.bundles.ui.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}

