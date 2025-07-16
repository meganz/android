pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url =
                uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/megagradle")
        }
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "mega.android.release",
                "mega.android.cicd",
                "mega.artifactory.publish.convention",
                    -> useModule("mega.privacy:megagradle:${requested.version}")

                else -> {}
            }
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        for (file in fileTree("./gradle/catalogs").matching { include("**/*.toml") }) {
            val name = file.name.split(".")[0]
            create(name) {
                from(files(file))
            }
        }
    }
}


if (!shouldUsePrebuiltSdk() || isServerBuild()) {
    include(":sdk")
}

include(":core:analytics:analytics-tracker")
include(":android-database-sqlcipher")
include(":app")
include(":baselineprofile")
include(":core-test")
include(":core-ui-test")
include(":core:analytics:analytics-test")
include(":core:formatter")
include(":core:navigation-contract")
include(":core:navigation-snowflake-components")
include(":data")
include(":domain")
include(":feature:chat")
include(":feature:devicecenter")
include(":feature:example")
include(":feature:payment:payment-snowflake-components")
include(":feature:shared:shared-snowflake-components")
include(":feature:sync")
include(":feature:transfers:transfers-snowflake-components")
include(":icon-pack")
include(":legacy-core-ui")
include(":lint")
include(":navigation")
include(":shared:original-core-ui")
include(":shared:resources")
include(":shared:sync")
include(":feature:cloud-drive:cloud-drive")
include(":feature:cloud-drive:cloud-drive-snowflake-components")
include(":core:ui-components:node-components")

// Configure modules to use their own name as the build file name
// app/build.gradle.kts -> app/app.gradle.kts
// features/home/build.gradle.kts -> features/home/home.gradle.kts
rootProject.children.forEach { project ->
    fun configureProject(project: ProjectDescriptor) {
        project.buildFileName = "${project.name}.gradle.kts"
        project.children.forEach { child ->
            configureProject(child)
        }
    }
    configureProject(project)
}

println("isServerBuild = ${isServerBuild()}")

buildCache {
    local {
        isEnabled = !isServerBuild()
        isPush = !isServerBuild()
    }

    remote<HttpBuildCache> {
        url =
            uri("${System.getenv()["ARTIFACTORY_BASE_URL"]}/artifactory/android-mega/gradle-cache/")
        credentials {
            username = System.getenv()["ARTIFACTORY_USER"]
            password = System.getenv()["ARTIFACTORY_ACCESS_TOKEN"]
        }
        isPush = isServerBuild()
        isEnabled = isServerBuild()
    }
}

fun shouldUsePrebuiltSdk(): Boolean =
    System.getenv("USE_PREBUILT_SDK")?.let { it != "false" } ?: true

fun isServerBuild(): Boolean = System.getenv("BUILD_NUMBER") != null

