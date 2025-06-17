plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.lint)
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

tasks.register("generateIconPackIcons") {
    group = "icon-generation"
    description = "Generates the IconPack object from XML drawables"

    var startTime = 0L

    doFirst {
        startTime = System.currentTimeMillis()
        println("Starting icon generation...")
    }

    doLast {
        mega.privacy.android.build.iconpack.GenerateIconsScript(
            outputDirPath = "icon-pack/src/main/java",
            drawablesPath = "icon-pack/src/main/res/drawable",
            mainObjectPackage = "mega.privacy.android.icon.pack",
            mainObjectName = "IconPack",
        )
        val elapsed = System.currentTimeMillis() - startTime
        println("Icon generation completed in ${elapsed}ms")
    }
}

tasks.named("preBuild").configure {
    dependsOn("generateIconPackIcons")
}