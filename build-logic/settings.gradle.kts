
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("plugin") {
            from(files("../gradle/catalogs/plugin.versions.toml"))
        }
        create("testlib") {
            from(files("../gradle/catalogs/testlib.versions.toml"))
        }
        create("androidx") {
            from(files("../gradle/catalogs/androidx.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
