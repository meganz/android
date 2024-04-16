import com.google.devtools.ksp.gradle.KspExtension
import mega.privacy.android.gradle.androidx
import mega.privacy.android.gradle.testlib
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Room related settings
 *
 */
class AndroidRoomConventionPlugin : Plugin<Project> {

    /**
     * Apply this plugin to the given target object.
     *
     * @param target The target object
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")

            extensions.configure<KspExtension> {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.generateKotlin", "true")
            }

            dependencies {
                add("implementation", androidx.findLibrary("room.paging").get())
                add("implementation", androidx.findLibrary("room").get())
                add("ksp", androidx.findLibrary("room.compiler").get())

                add("androidTestImplementation", testlib.findLibrary("room.test").get())
            }
        }
    }
}