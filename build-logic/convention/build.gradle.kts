plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(plugin.build.tools)
    compileOnly(plugin.kotlin.gradle)
    compileOnly(kotlin("stdlib"))
    compileOnly(gradleApi())
    testImplementation(testlib.truth)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "mega.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
    }
}
