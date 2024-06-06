import mega.privacy.android.gradle.extension.MegaJacocoPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Plugin to apply Jacoco configuration to JVM library projects.
 */
class JvmLibraryJacocoConventionPlugin : Plugin<Project> {

    private var userExcludedFiles: Set<String> = emptySet()
    private var userIncludedFiles: Set<String> = emptySet()
    private var excludedFiles: List<String> = emptyList()

    /**
     * Apply the Jacoco configuration to the project.
     *
     * @param target the project to apply the configuration to
     */
    override fun apply(target: Project) {
        println("JvmLibraryJacocoConventionPlugin applied to project ${target.name}")
        with(target) {
            pluginManager.apply("jacoco")

            extensions.create("mega-jacoco", MegaJacocoPluginExtension::class.java)
            extensions.getByType<MegaJacocoPluginExtension>().defaultExcludedFiles =
                defaultExcludedFiles

            afterEvaluate {
                extensions.getByType<MegaJacocoPluginExtension>().let {
                    userExcludedFiles = it.excludedFiles
                    userIncludedFiles = it.includedFiles
                    excludedFiles = mergeExcludedFiles()
                }

                val reportPath = "${layout.buildDirectory.get()}/coverage-report"
                tasks.withType<JacocoReport> {
                    dependsOn("test")
                    reports {
                        csv.required.set(true)
                        csv.outputLocation.set(file("$reportPath/coverage.csv"))
                        xml.required.set(true)
                        xml.outputLocation.set(file("$reportPath/coverage.xml"))
                        html.required.set(true)
                        html.outputLocation.set(file("$reportPath/html"))
                    }

                    classDirectories.setFrom(
                        classDirectories.files.map { dir ->
                            fileTree(dir) {
                                exclude(excludedFiles.toList())
                            }
                        }
                    )
                }
            }
        }
    }

    private fun mergeExcludedFiles(): List<String> {
        val result: MutableSet<String> = defaultExcludedFiles.toMutableSet()
        result += userExcludedFiles
        result -= userIncludedFiles
        return result.toList()
    }

    private val defaultExcludedFiles = setOf(
        // dagger
        "**/*_MembersInjector.class",
        "**/Dagger*Component.class",
        "**/Dagger*Component\$Builder.class",
        "**/Dagger*Subcomponent*.class",
        "**/*Subcomponent\$Builder.class",
        "**/*Module_*Factory.class",
        "**/di/module/*",
        "**/*_Factory*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*",
        //entity in domain layer
        "**/domain/entity/*",
    )
}