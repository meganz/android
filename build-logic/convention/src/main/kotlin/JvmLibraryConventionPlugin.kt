import mega.privacy.android.gradle.configureKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Conventions for JVM(non-Android) library modules
 *
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    /**
     * Apply this plugin to the given target object.
     *
     * @param target
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("kotlin")
            }
            configureKotlin()
            setJvmToolChainVersion()
        }
    }
}

private fun Project.setJvmToolChainVersion() {
    /**
     * Custom Launcher to set jvmToolchain
     */
    val service = extensions.getByType<JavaToolchainService>()

    /**
     * Custom Launcher to set jvmToolchain
     */
    val customLauncher = service.launcherFor {
        val jdk: String by rootProject.extra
        languageVersion.set(JavaLanguageVersion.of(jdk.toInt()))
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinJavaToolchain.toolchain.use(customLauncher)
    }
}