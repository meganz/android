import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Plugin to apply Jacoco configuration to JVM library projects.
 */
class JvmLibraryJacocoConventionPlugin : Plugin<Project> {

    /**
     * Apply the Jacoco configuration to the project.
     *
     * @param target the project to apply the configuration to
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("jacoco")

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
                val excludedFileList = listOf(
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
                classDirectories.setFrom(
                    classDirectories.files.map { dir -> fileTree(dir) { exclude(excludedFileList) } }
                )
            }
        }
    }
}