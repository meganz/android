dependencyResolutionManagement {
    versionCatalogs {
        create("testlib") {
            from(files("../gradle/catalogs/testlib.versions.toml"))
        }
        create("lib") {
            from(files("../gradle/catalogs/lib.versions.toml"))
        }
    }
}