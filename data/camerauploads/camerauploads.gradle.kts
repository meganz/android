plugins {
    // `id` rather than `alias`: :data already applies these, so this child project sees them on
    // the classpath with an unknown version and version-bearing aliases fail to resolve.
    id("mega.android.library")
    id("mega.android.hilt")
}

android {
    lint {
        abortOnError = true
        warningsAsErrors = true
    }
    namespace = "mega.privacy.android.data.camerauploads"
}

dependencies {
    lintChecks(project(":lint"))
    implementation(project(":data"))
    implementation(project(":domain:core:shared"))
    implementation(project(":domain:feature:camerauploads"))

    implementation(lib.coroutines.core)
    implementation(androidx.hilt.work)
    implementation(androidx.work.ktx)
    implementation(androidx.concurrent.futures)
    // WorkManager returns Guava ListenableFutures; the empty listenablefuture stub it pulls in has
    // no classes, so full Guava is needed to compile against them, as in :data.
    implementation(google.guava)
    implementation(lib.logging.timber)

    testImplementation(project(":core-test"))
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.truth.ext)
    testImplementation(testlib.test.core.ktx)
    testImplementation(lib.bundles.unit.test)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.junit.test.ktx)
    testImplementation(androidx.work.test)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
