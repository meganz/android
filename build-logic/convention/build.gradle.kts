plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(plugin.build.tools)
    compileOnly(plugin.kotlin.gradle)
    compileOnly(plugin.ksp.gradle.plugin)
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
    plugins {
        register("androidRoom") {
            id = "mega.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
    plugins {
        register("androidTest") {
            id = "mega.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
    }
    plugins {
        register("jvmTest") {
            id = "mega.jvm.test"
            implementationClass = "JvmTestConventionPlugin"
        }
    }
}
