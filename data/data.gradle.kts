import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.room)
    alias(convention.plugins.mega.android.hilt)
    id("kotlin-android")
    alias(plugin.plugins.kotlin.serialisation)
}

android {
    defaultConfig {
        val appVersion: String by rootProject.extra
        resValue("string", "app_version", "\"${appVersion}\"")
        consumerProguardFiles("consumer-rules.pro")
    }
    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
    configurations {
        implementation {
            exclude(module = "protolite-well-known-types")
            exclude(module = "protobuf-javalite")
        }
    }
    lint {
        abortOnError = true
        warningsAsErrors = true
    }
    namespace = "mega.privacy.android.data"
}

android.testVariants.all {
    compileConfiguration.exclude(group = "com.google.guava", module = "listenablefuture")
    runtimeConfiguration.exclude(group = "com.google.guava", module = "listenablefuture")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":shared:sync"))
    implementation(google.guava)
    api(project(":third-party-lib:pdfiumAndroid"))

    //Test Modules
    testImplementation(project(":core-test"))

    preBuiltSdkDependency(rootProject.extra)

    implementation(lib.coroutines.core)
    implementation(lib.kotlin.serialisation)
    implementation(google.gson)
    implementation(google.zxing)
    implementation(androidx.java.core)
    implementation(androidx.exifinterface)
    implementation(androidx.datastore.preferences)
    implementation(androidx.preferences)
    implementation(androidx.lifecycle.process)
    implementation(androidx.work.ktx)
    implementation(androidx.hilt.work)
    implementation(androidx.concurrent.futures)
    implementation(androidx.paging)
    implementation(androidx.documentfile)

    implementation(lib.billing.client.ktx)

    implementation(platform(google.firebase.bom))
    implementation(google.firebase.perf.ktx)

    // Logging
    implementation(lib.bundles.logging)
    // sqlcipherlib
    implementation(lib.sqlcipher) { artifact { type = "aar" } }

    implementation(androidx.security.crypto)
    implementation(google.tink)

    implementation(google.protobuff)

    // Testing dependencies
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.truth.ext)
    testImplementation(testlib.test.core.ktx)
    testImplementation(lib.bundles.unit.test)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.junit.test.ktx)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    androidTestImplementation(testlib.bundles.unit.test)
    androidTestImplementation(lib.bundles.unit.test)
    androidTestImplementation(testlib.junit.test.ktx)
    androidTestImplementation(testlib.runner)
}
