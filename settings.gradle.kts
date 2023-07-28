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
val isCiBuild: groovy.lang.Closure<Boolean> by extra

if (!shouldUsePrebuiltSdk() || isCiBuild()) {
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
include(":nocturn")
include("liveeventbus-x")
include(":xray")
include(":analytics")
include(":core-ui-test")

println("isCiBuild = ${isCiBuild()}")

buildCache {
    local {
        isEnabled = !isCiBuild()
        isPush = !isCiBuild()
    }

    remote<HttpBuildCache> {
        url =
            uri("${System.getenv()["ARTIFACTORY_BASE_URL"]}/artifactory/android-mega/gradle-cache/")
        credentials {
            username = System.getenv()["ARTIFACTORY_USER"]
            password = System.getenv()["ARTIFACTORY_ACCESS_TOKEN"]
        }
        isPush = isCiBuild()
        isEnabled = isCiBuild()
    }
}