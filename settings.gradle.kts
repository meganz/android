apply(from = "tools/util.gradle")

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

/**
 * Checks whether to use Prebuilt Sdk
 */
val shouldUsePrebuiltSdk: groovy.lang.Closure<Boolean> by extra

/**
 * Checks if it is CI Build
 */
val isServerBuild: groovy.lang.Closure<Boolean> by extra

if (!shouldUsePrebuiltSdk() || isServerBuild()) {
    include(":sdk")
}

include(":app")
include(":domain")
include(":core:formatter")
include("core-ui")
include(":screenshot")
include(":data")
include(":lint")
include(":feature:sync")
include("feature:devicecenter")
include("liveeventbus-x")
include(":analytics")
include(":core-ui-test")
include(":baselineprofile")
include(":navigation")

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