import com.android.build.gradle.LibraryExtension
import mega.privacy.android.gradle.extension.MegaJacocoPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.fileTree
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Locale

/**
 * Plugin to apply Jacoco configuration to Android library projects.
 */
class AndroidLibraryJacocoConventionPlugin : Plugin<Project> {

    private var userExcludedFiles: Set<String> = emptySet()
    private var userIncludedFiles: Set<String> = emptySet()
    private var excludedFiles: List<String> = emptyList()

    /**
     * Apply the Jacoco configuration to the project.
     *
     * @param target the project to apply the configuration to
     */
    override fun apply(target: Project) {
        with(target) {
            println("AndroidLibraryJacocoConventionPlugin applied to project ${project.name}")
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
            }

            tasks.withType<Test> {
                configure<JacocoTaskExtension> {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }

            extensions.configure<LibraryExtension> {
                val buildTypes = buildTypes.map { it.name }
                val flavors =
                    productFlavors.map { it.name }.takeIf { it.isNotEmpty() } ?: listOf("")

                flavors.forEach { productFlavorName ->
                    buildTypes.forEach { buildTypeName ->
                        val sourceName: String?
                        val sourcePath: String?
                        if (productFlavorName.isEmpty()) {
                            sourcePath = buildTypeName
                            sourceName = sourcePath
                        } else {
                            sourceName =
                                "${productFlavorName}${capitalise(buildTypeName)}"
                        }

                        val testTaskName = "test${capitalise(sourceName)}UnitTest"

                        tasks.register("${testTaskName}Coverage", JacocoReport::class.java) {
                            dependsOn(testTaskName)
                            group = "Reporting"
                            description =
                                "Generate Jacoco coverage reports on the ${capitalise(sourceName)} build."

                            val javaTree = this@with.fileTree(
                                "dir" to "${layout.buildDirectory.get()}/intermediates/javac/$sourceName/classes",
                                "excludes" to excludedFiles,
                            )
                            val kotlinTree = this@with.fileTree(
                                "dir" to "${layout.buildDirectory.get()}/tmp/kotlin-classes/$sourceName",
                                "excludes" to defaultExcludedFiles.toList()
                            )

                            classDirectories.setFrom(
                                files(
                                    listOf(javaTree),
                                    listOf(kotlinTree),
                                )
                            )
                            executionData.setFrom(
                                files("${layout.buildDirectory.get()}/jacoco/${testTaskName}.exec")
                            )
                            val coverageSourceDirs = listOf(
                                "src/main/java",
                                "src/$productFlavorName/java",
                                "src/$buildTypeName/java"
                            )
                            sourceDirectories.setFrom(files(coverageSourceDirs))
                            additionalSourceDirs.setFrom(files(coverageSourceDirs))

                            val reportPath = "${layout.buildDirectory.get()}/coverage-report"
                            reports {
                                csv.required.set(true)
                                csv.outputLocation.set(file("$reportPath/coverage.csv"))
                                xml.required.set(true)
                                xml.outputLocation.set(file("$reportPath/coverage.xml"))
                                html.required.set(true)
                                html.outputLocation.set(file("$reportPath/html"))
                            }
                        }
                    }
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
        // data binding
        "android/databinding/**/*.class",
        "**/android/databinding/*Binding.class",
        "**/android/databinding/*",
        "**/androidx/databinding/*",
        "**/BR.*",
        // android
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "ndroid/**/*.*",
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
        // kotlin
        "**/*MapperImpl*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/BuildConfig.*",
        "**/*Component*.*",
        "**/*BR*.*",
        "**/Manifest*.*",
        "**/*\$Lambda$*.*",
        "**/*Companion*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*",
        "**/*MembersInjector*.*",
        "**/*_MembersInjector.class",
        "**/*_Factory*.*",
        "**/*_Provide*Factory*.*",
        "**/*Extensions*.*",
        // sealed and data classes
        "**/*\$Result.*",
        "**/*\$Result$*.*",
        // adapters generated by moshi
        "**/*JsonAdapter.*",
        //entity in domain layer
        "**/domain/entity/*",
    )


    private fun capitalise(str: String): String {
        return str.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }
}