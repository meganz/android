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
                "mega.android.release" -> useModule("mega.privacy:megagradle:${requested.version}")
                "mega.android.cicd" -> useModule("mega.privacy:megagradle:${requested.version}")
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

include(":app")
include(":domain")
include(":core:formatter")
include(":shared:original-core-ui")
include(":data")
include(":lint")
include(":feature:sync")
include("feature:devicecenter")
include("liveeventbus-x")
include(":analytics")
include(":core-test")
include(":core-ui-test")
include(":baselineprofile")
include(":navigation")
include(":legacy-core-ui")
include(":icon-pack")
include(":shared:resources")
include(":shared:sync")

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