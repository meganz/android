
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
    }
}

rootProject.name = "build-logic"
include(":convention")
