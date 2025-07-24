import mega.privacy.android.build.isServerBuild

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    id("kotlin-android")
}

android {
    namespace = "mega.privacy.android.icon.pack"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    lintChecks(project(":lint"))
    implementation(androidx.bundles.compose.bom)
    implementation(lib.kotlin.ktx)
    debugImplementation(lib.kotlinpoet)
}

tasks.register("generateIconPackVectors") {
    group = "icon-generation"
    description = "Generates the IconPack object with ImageVector properties from IconPackInterface"

    var startTime = 0L

    doFirst {
        startTime = System.currentTimeMillis()
        println("Starting ImageVector icon generation...")
    }

    doLast {
        mega.privacy.android.build.iconpack.GenerateIconVectorsScript(
            outputDirPath = "./icon-pack/src/main/java",
            drawablesPath = "./icon-pack/src/main/res/drawable",
            svgDirectoryPath = "./icon-pack/icons_MEGA", // Directory for SVG files with icon_ prefix
            interfaceFilePath = "./icon-pack/src/main/java/mega/privacy/android/icon/pack/IconPackInterface.kt",
            mainObjectPackage = "mega.privacy.android.icon.pack",
            mainObjectName = "IconPack",
        )
        val elapsed = System.currentTimeMillis() - startTime
        println("ImageVector icon generation completed in ${elapsed}ms")
    }
}


// Only run icon generation locally, not in CI/CD, to don't create the icons in the pipeline so missing generated icons are detected
if (!isServerBuild()) {
    tasks.named("preBuild").configure {
        dependsOn("generateIconPackVectors")
    }
}