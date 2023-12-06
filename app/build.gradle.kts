import groovy.lang.Closure

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.firebase.appdistribution")
    id("jacoco")
    id("com.google.gms.google-services")
    id("de.mannodermaus.android-junit5")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
    id("androidx.baselineprofile")
}

apply(from = "${project.rootDir}/tools/util.gradle")
apply(from = "${project.rootDir}/tools/sdk.gradle")

configurations {
    jacocoAnt
}

jacoco {
    toolVersion = "0.8.8"
}

android {
    val compileSdkVersion: Int by rootProject.extra
    compileSdk = compileSdkVersion
    val buildTools: String by rootProject.extra
    buildToolsVersion = buildTools

    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    defaultConfig {
        applicationId = "mega.privacy.android.app"

        val minSdkVersion: Int by rootProject.extra
        minSdk = minSdkVersion

        val targetSdkVersion: Int by rootProject.extra
        targetSdk = targetSdkVersion

        val appVersion: String by rootProject.extra
        versionName = appVersion

        if (useStaticVersion()) {
            println("Create DEBUG build using static versionCode")
            versionCode = 9999
            versionNameSuffix = "(9999_debug)"
        } else {
            println("Create NORMAL build using dynamic versionCode")
            val readVersionCode: Closure<Int> by extra
            versionCode = readVersionCode()
            val readVersionNameChannel: Closure<String> by extra
            val readVersionNameTag: Closure<String> by extra
            val getAppGitHash: Closure<String> by extra
            versionNameSuffix =
                "${readVersionNameChannel()}(${readVersionCode()}${readVersionNameTag()})(${getAppGitHash()})"
        }

        multiDexEnabled = true
        ndk {
            abiFilters += listOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
        }

        buildConfigField("String", "USER_AGENT", "\"MEGAAndroid/${versionName}_${versionCode}\"")

        val shouldActivateGreeter: Closure<Boolean> by extra
        buildConfigField("boolean", "ACTIVATE_GREETER", "${shouldActivateGreeter()}")

        val shouldActivateNocturn: Closure<Boolean> by extra
        buildConfigField("boolean", "ACTIVATE_NOCTURN", "${shouldActivateNocturn()}")

        val getNocturnTimeout: Closure<Long> by extra
        buildConfigField("long", "NOCTURN_TIMEOUT", "${getNocturnTimeout()}")

        val getKarmaPluginPort: Closure<Int> by extra
        buildConfigField("int", "KARMA_PLUGIN_PORT", "${getKarmaPluginPort()}")

        resValue("string", "app_version", "\"${versionName}${versionNameSuffix}\"")

        val megaSdkVersion: String by rootProject.extra
        val getSdkGitHash: Closure<String> by extra
        resValue("string", "sdk_version", "\"${getSdkGitHash(megaSdkVersion)}\"")

        val getChatGitHash: Closure<String> by extra
        resValue("string", "karere_version", "\"${getChatGitHash(megaSdkVersion)}\"")

        testInstrumentationRunner = "test.mega.privacy.android.app.HiltTestRunner"

        resourceConfigurations += listOf(
            "en",
            "ar",
            "de",
            "es",
            "fr",
            "in",
            "it",
            "ja",
            "ko",
            "nl",
            "pl",
            "pt",
            "ro",
            "ru",
            "th",
            "vi",
            "zh-rCN",
            "zh-rTW"
        )

        withGroovyBuilder {
            "firebaseCrashlytics" {
                // Enable processing and uploading of native symbols to Crashlytics servers.
                // This flag must be enabled to see properly-symbolicated native
                // stack traces in the Crashlytics dashboard.
                val nativeLibsDir: Closure<String> by extra
                "nativeSymbolUploadEnabled"(true)
                "unstrippedNativeLibsDir"(nativeLibsDir())
            }
        }
    }

    sourceSets {
        getByName("debug") {
            res {
                srcDirs("src/main/res")
            }
        }

        register("qa") {
            java {
                srcDirs("src/qa/java")
            }
            res {
                srcDirs("src/qa/res")
            }
        }
    }

    packaging {
        jniLibs.pickFirsts.add("lib/arm64-v8a/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/arm64-v8a/libmega.so")
        jniLibs.pickFirsts.add("lib/arm64-v8a/libjniPdfium.so")
        jniLibs.pickFirsts.add("lib/arm64-v8a/libmodpdfium.so")
        jniLibs.pickFirsts.add("lib/arm64-v8a/libmodft2.so")
        jniLibs.pickFirsts.add("lib/arm64-v8a/libmodpng.so")

        jniLibs.pickFirsts.add("lib/armeabi-v7a/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libmega.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libjniPdfium.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libmodpdfium.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libmodft2.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libmodpng.so")

        jniLibs.pickFirsts.add("lib/x86/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/x86/libmega.so")
        jniLibs.pickFirsts.add("lib/x86/libjniPdfium.so")
        jniLibs.pickFirsts.add("lib/x86/libmodpdfium.so")
        jniLibs.pickFirsts.add("lib/x86/libmodft2.so")
        jniLibs.pickFirsts.add("lib/x86/libmodpng.so")

        jniLibs.pickFirsts.add("lib/x86_64/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/x86_64/libmega.so")
        jniLibs.pickFirsts.add("lib/x86_64/libjniPdfium.so")
        jniLibs.pickFirsts.add("lib/x86_64/libmodpdfium.so")
        jniLibs.pickFirsts.add("lib/x86_64/libmodft2.so")
        jniLibs.pickFirsts.add("lib/x86_64/libmodpng.so")
    }

    val readReleaseNotes: Closure<String> by extra
    val readTesterGroupList: Closure<String> by extra
    val readTesters: Closure<String> by extra

    buildTypes {
        debug {
            isDebuggable = true
            extra["enableCrashlytics"] = false
            extra["alwaysUpdateBuildId"] = false
            buildConfigField("String", "ENVIRONMENT", "\"MEGAEnv/Dev\"")
        }
        release {
            firebaseAppDistribution {
                releaseNotes = readReleaseNotes()
                groups = readTesterGroupList()
                testers = readTesters()
            }
            buildConfigField("String", "ENVIRONMENT", "\"\"")
        }

        register("qa") {
            initWith(getByName("debug"))
            isDebuggable = true
            matchingFallbacks += listOf("debug", "release")
            applicationIdSuffix = ".qa"
            buildConfigField("String", "ENVIRONMENT", "\"MEGAEnv/QA\"")
            firebaseAppDistribution {
                releaseNotes = readReleaseNotes()
                groups = readTesterGroupList()
                testers = readTesters()
            }
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        // Sets Java compatibility to JavaVersion in Project Root
        val javaVersion: JavaVersion by rootProject.extra
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlin {
        val jdk: String by rootProject.extra
        jvmToolchain(jdk.toInt())
    }

    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
        val shouldSuppressWarnings: Boolean by rootProject.extra
        suppressWarnings = shouldSuppressWarnings
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    flavorDimensions += "service"
    productFlavors {
        create("gms") {
            dimension = "service"
        }
    }

    configurations {
        implementation {
            exclude(module = "protolite-well-known-types")
            exclude(module = "protobuf-javalite")
        }
    }
    lint {
        checkReleaseBuilds = false
        val shouldCombineLintReports: Closure<Boolean> by extra
        if (shouldCombineLintReports()) {
            checkDependencies = true
            htmlReport = true
            htmlOutput = file("build/reports/combined.html")
        }
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
    namespace = "mega.privacy.android.app"
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

project.extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    buildTypes.configureEach {
        configure<com.google.firebase.appdistribution.gradle.AppDistributionExtension> {
            if (name == "release") {
                appId = "1:268821755439:android:9b611c50c9f7a503"
            } else if (name == "qa") {
                appId = "1:268821755439:android:1e977881202351664e78da"
            }
        }
    }
}

tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

applyTestLiteForTasks()

dependencies {
    // Modules
    implementation(project(":core:formatter"))
    implementation(project(":domain"))
    implementation(project(":core-ui"))
    implementation(project(":shared:theme"))
    implementation(project(":legacy-core-ui"))
    implementation(project(":data"))
    implementation(project(":navigation"))
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    "baselineProfile"(project(":baselineprofile"))
    implementation(project(":liveeventbus-x"))
    implementation(project(":analytics"))
    implementation(project(":icon-pack"))
    testImplementation(project(":core-ui-test"))

    // Jetbrains
    implementation(lib.anko.commons)
    implementation(lib.coroutines.android)
    implementation(lib.coroutines.core)
    implementation(lib.kotlin.ktx)
    implementation(lib.kotlin.stdlib)
    implementation(lib.kotlin.stdlib.jdk7)

    // Android X
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.bundles.navigation)
    implementation(androidx.appcompat)
    implementation(androidx.biometric)
    implementation(androidx.camera.camera2)
    implementation(androidx.camera.view)
    implementation(androidx.camera.lifecycle)
    implementation(androidx.cardview)
    implementation(androidx.constraintlayout)
    implementation(androidx.constraintlayout.compose)
    implementation(androidx.datastore.preferences)
    implementation(androidx.emoji)
    implementation(androidx.exifinterface)
    implementation(androidx.fragment)
    implementation(androidx.legacy.support)
    implementation(androidx.multidex)
    implementation(androidx.palette)
    implementation(androidx.preferences)
    implementation(androidx.recyclerview)
    implementation(androidx.recyclerview.selection)
    implementation(androidx.viewpager2)
    implementation(androidx.work.ktx)

    // Compose
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.compose.activity)
    implementation(androidx.compose.viewmodel)
    implementation(lib.coil)
    implementation(lib.coil.gif)
    implementation(lib.coil.svg)
    implementation(lib.coil.compose)

    // Google
    implementation(google.gson)
    implementation(google.material)
    implementation(google.exoplayer.core)
    implementation(google.exoplayer.ui)
    implementation(google.flexbox)
    implementation(google.zxing)
    implementation(google.accompanist.pager)
    implementation(google.accompanist.flowlayout)
    implementation(google.accompanist.placeholder)
    implementation(google.accompanist.permissions)
    implementation(google.accompanist.navigationanimation)
    implementation(google.accompanist.systemui)

    // Google GMS
    "gmsImplementation"(lib.billing.client.ktx)
    "gmsImplementation"(google.services.location)
    "gmsImplementation"(google.services.maps)
    "gmsImplementation"(google.maps.utils)
    "gmsImplementation"(google.code.scanner)

    // Firebase
    "gmsImplementation"(platform(google.firebase.bom))
    "gmsImplementation"(google.bundles.firebase.bom)

    // Play Core
    implementation(google.play.core)
    implementation(google.play.core.ktx)

    // protobuf-java for tombstone debug
    implementation(google.protobuff)

    // Hilt
    implementation(google.hilt.android)
    implementation(androidx.hilt.work)
    implementation(androidx.hilt.navigation)

    val shouldApplyDefaultConfiguration: Closure<Boolean> by rootProject.extra
    if (shouldApplyDefaultConfiguration()) {
        apply(plugin = "dagger.hilt.android.plugin")

        kapt(google.hilt.android.compiler)
        kapt(androidx.hilt.compiler)
        kaptTest(google.hilt.android.compiler)
    }

    // RX
    implementation(lib.bundles.rx)

    // Fresco
    implementation(lib.bundles.fresco)
    implementation(lib.facebook.inferannotation)
    implementation(files("src/main/libs/fresco-zoomable.aar"))

    // Retrofit
    implementation(lib.retrofit)
    implementation(lib.retrofit.gson)

    // Logging
    implementation(lib.bundles.logging)

    // Other libs
    implementation(lib.bannerviewpager)
    implementation(lib.parallaxscroll)
    implementation(lib.vdurmont.emoji)
    implementation(lib.code.scanner)
    implementation(lib.stickyheader)
    implementation(lib.shimmerlayout)
    implementation(lib.collapsingtoolbar)
    implementation(lib.namedregexp)
    implementation(lib.blurry)
    implementation(lib.documentscanner)
    implementation(lib.simplestorage)
    implementation(lib.shortcutbadger) {
        artifact {
            type = "aar"
        }
    }
    implementation(lib.compose.state.events)
    implementation(testlib.hamcrest)
    implementation(lib.mega.analytics)
    debugImplementation(lib.nocturn)
    debugImplementation(lib.xray)

    coreLibraryDesugaring(lib.desugar)

    val shouldUsePrebuiltSdk: Closure<Boolean> by rootProject.extra
    if (shouldUsePrebuiltSdk()) {
        // These 2 ExoPlayer libs are created by SDK build. If upgrading ExoPlayer version,
        // remember to upload these 2 files.
        implementation(files("src/main/libs/exoplayer-extension-ffmpeg-2.18.1.aar"))
        implementation(files("src/main/libs/exoplayer-extension-flac-2.18.1.aar"))
    } else {
        implementation(
            fileTree(
                mapOf(
                    "dir" to "${rootProject.projectDir}/sdk/src/main/jni/ExoPlayer/",
                    "include" to listOf("*.aar")
                )
            )
        )

        implementation(files("../sdk/src/main/jni/megachat/webrtc/libwebrtc.jar"))
    }

    // Kotlin + coroutines
    // Java Code Coverage
    jacocoAnt("org.jacoco:org.jacoco.ant:0.8.8:nodeps")

    // Testing dependencies
    testImplementation(testlib.bundles.unit.test)
    testImplementation(lib.bundles.unit.test)
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.truth.ext)
    testImplementation(testlib.arch.core.test)
    testImplementation(testlib.test.core.ktx)
    testImplementation(testlib.junit.test.ktx)
    testImplementation(testlib.espresso.contrib) {
        exclude(group = "org.checkerframework", module = "checker")
        exclude(module = "protobuf-lite")
    }
    testImplementation(google.hilt.android.test)
    testImplementation(androidx.work.test)
    testImplementation(testlib.room.test)
    testImplementation(testlib.compose.junit)
    testImplementation(androidx.navigation.testing)

    //jUnit 5
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    androidTestImplementation(platform(androidx.compose.bom))
    androidTestImplementation(testlib.junit.test.ktx)
    androidTestImplementation(testlib.truth)
    androidTestImplementation(testlib.espresso)
    androidTestImplementation(google.hilt.android.test)
    androidTestImplementation(testlib.mockito)
    androidTestImplementation(testlib.mockito.kotlin)
    androidTestImplementation(testlib.mockito.android)
    androidTestImplementation(testlib.espresso.contrib) {
        exclude(group = "org.checkerframework", module = "checker")
        exclude(module = "protobuf-lite")
    }
    androidTestImplementation(testlib.espresso.intents)
    androidTestImplementation(testlib.compose.junit)

    kaptAndroidTest(google.hilt.android.compiler)
    debugImplementation(androidx.fragment.test)
    debugImplementation(testlib.compose.manifest)
    debugImplementation(testlib.test.monitor)

    // Live Data testing
    testImplementation(testlib.jraska.livedata.test)

    //QA
    "qaImplementation"(google.firebase.app.distribution)
    "qaImplementation"(testlib.compose.manifest)

    lintChecks(project(":lint"))

    implementation(project(":feature:sync"))
    implementation(project(":feature:devicecenter"))

    implementation(androidx.sqlite.ktx)
}

tasks.register("instrumentClasses") {
    dependsOn("compileGmsDebugSources")
    val outputDir = "${buildDir.path}/intermediates/classes-instrumented/gms/debug/"
    doLast {
        println("Instrumenting classes")

        val fileFilter = listOf(
            // data binding
            "android/databinding/**/*.class",
            "**/android/databinding/*Binding.class",
            "**/android/databinding/*",
            "**/androidx/databinding/*",
            "**/BR.*",
            // android
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            // dagger
            "**/*_MembersInjector.class",
            "**/Dagger*Component.class",
            "**/Dagger*Component\$Builder.class",
            "**/Dagger*Subcomponent*.class",
            "**/*Subcomponent\$Builder.class",
            "**/*Module_*Factory.class",
            "**/di/module/*",
            "**/*_Factory*.*",
            "**/*Module*.*",
            "**/*Dagger*.*",
            "**/*Hilt*.*",
            // kotlin
            "**/*MapperImpl*.*",
            "**/*\$ViewInjector*.*",
            "**/*\$ViewBinder*.*",
            "**/BuildConfig.*",
            "**/*Component*.*",
            "**/*BR*.*",
            "**/Manifest*.*",
            "**/*\$Lambda$*.*",
//                "**/*Companion*.*",
            "**/*Module*.*",
            "**/*Dagger*.*",
            "**/*Hilt*.*",
            "**/*MembersInjector*.*",
            "**/*_MembersInjector.class",
            "**/*_Factory*.*",
            "**/*_Provide*Factory*.*",
//                "**/*Extensions*.*",
            // sealed and data classes
            "**/*$Result.*",
            "**/*$Result$*.*",
            // adapters generated by moshi
            "**/*JsonAdapter.*",
            //entity in domain layer
            "**/domain/entity/*",
            // model in data layer
            "**/data/model/*",
        )
        val excludesPattern = fileFilter.joinToString()
        val jacocoAntConfig by configurations.jacocoAnt
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "instrument",
                "classname" to "org.jacoco.ant.InstrumentTask",
                "classpath" to jacocoAntConfig.asPath
            )
            "instrument"("destdir" to outputDir) {
                "fileset"(
                    "dir" to "${buildDir.path}/intermediates/javac/gmsDebug/classes",
                    "excludes" to excludesPattern
                )
                "fileset"(
                    "dir" to "${buildDir.path}/tmp/kotlin-classes/gmsDebug",
                    "excludes" to excludesPattern
                )
            }
        }
        /* Add the instrumented classes to the beginning of classpath */
        tasks.named("testGmsDebugUnitTest") {
            if (hasProperty("classpath")) {
                setProperty("classpath", files(outputDir) + property("classpath") as FileCollection)
            }
        }
    }
}

tasks.register("createUnitTestCoverageReport") {
    dependsOn("instrumentClasses", "testGmsDebugUnitTest")
    val jacocoAntConfig by configurations.jacocoAnt
    doLast {
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "report",
                "classname" to "org.jacoco.ant.ReportTask",
                "classpath" to jacocoAntConfig.asPath
            )
            "report"() {
                "executiondata" {
                    ant.withGroovyBuilder {
                        "file"("file" to "${buildDir.path}/jacoco/testGmsDebugUnitTest.exec")
                    }
                }
                "structure"("name" to "Coverage") {
                    "classfiles" {
                        "fileset"("dir" to "${buildDir.path}/intermediates/javac/gmsDebug/classes")
                        "fileset"("dir" to "${buildDir.path}/tmp/kotlin-classes/gmsDebug")
                    }
                    "sourcefiles" {
                        "fileset"("dir" to "src/main/java")
                        "fileset"("dir" to "src/test/java")
                    }
                }
                "html"("destdir" to "${buildDir.path}/reports/jacoco/html")
                "csv"("destfile" to "${buildDir.path}/reports/jacoco/gmsDebugUnitTestCoverage.csv")
            }
        }
    }
}

/**
 * Gradle task for getting the app git hash
 * Run ./gradlew -q printAppGitHash
 */
tasks.register("printAppGitHash") {
    doLast {
        val getAppGitHash: Closure<String> by ext
        println(getAppGitHash())
    }
}

/**
 * Gradle task for getting the app version name
 * Run ./gradlew -q printAppVersionName
 */
tasks.register("printAppVersionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}

/**
 * Gradle task for getting the pre-build SDK version
 * Run ./gradlew -q printPrebuildSdkVersion
 */
tasks.register("printPrebuildSdkVersion") {
    doLast {
        val megaSdkVersion: String by rootProject.extra
        println(megaSdkVersion)
    }
}

/**
 * Gradle task for getting the app version name channel
 * Run ./gradlew -q printAppVersionNameChannel
 */
tasks.register("printAppVersionNameChannel") {
    doLast {
        val readVersionNameChannel: Closure<String> by ext
        println(readVersionNameChannel())
    }
}

/**
 * Decide whether to use static version code
 */
fun useStaticVersion(): Boolean {
    val isCiBuild: Closure<Boolean> by extra
    val isServerBuild: Closure<Boolean> by extra
    val buildTypeMatches: Closure<Boolean> by extra
    val taskNames = gradle.startParameter.taskNames
    return buildTypeMatches("debug", taskNames) ||
            buildTypeMatches("lint", taskNames) ||
            buildTypeMatches("test", taskNames) ||
            (buildTypeMatches("qa", taskNames) && !isServerBuild()) ||
            (buildTypeMatches("qa", taskNames) && isCiBuild())
}

/**
 * Apply unit test lite mode if constraints met
 */
fun applyTestLiteForTasks() {
    val shouldActivateTestLite: Closure<Boolean> by extra
    val excludedTasks = listOf<(Task) -> Boolean>(
        { it.name.startsWith("injectCrashlytics") },
        { it.name.startsWith("kapt") && it.name.endsWith("TestKotlin") },
    )

    gradle.taskGraph.whenReady {
        for (task in allTasks) {
            if (task.name.lowercase().startsWith("test")) {
                tasks.matching { activeTask ->
                    excludedTasks.any { it(activeTask) } && shouldActivateTestLite()
                }.configureEach {
                    enabled = false
                }
            }
        }
    }
}