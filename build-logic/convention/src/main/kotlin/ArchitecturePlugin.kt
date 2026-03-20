import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency


class ArchitecturePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.afterEvaluate {
            project.configurations.forEach { configuration ->
                configuration.dependencies.filterIsInstance<ProjectDependency>()
                    .forEach { dependency ->
                        val moduleLayer = project.path.layer()
                        val dependencyLayer = dependency.path.layer()
                        if (
                            project.path != dependency.path
                            && moduleLayer != null
                            && dependencyLayer != null
                        ) {
                            validateDependency(
                                moduleLayer,
                                dependencyLayer,
                            )
                        }
                    }
            }
        }
    }

    private fun validateDependency(
        moduleLayer: ArchitectureLayer,
        dependencyLayer: ArchitectureLayer,
    ) {
        if (dependencyLayer.path in dependencyExceptions) return
        if (moduleLayer.path in moduleExceptions) return
        if ((dependencyLayer as? ArchitectureLayer.SnowFlake)?.validFor(moduleLayer) == false) {
            throw GradleException("Snowflake layer module '${dependencyLayer.path}' cannot be used for not related modules '${moduleLayer.path}'.")
        }
        if (dependencyLayer::class.java !in moduleLayer.allowedDependencies) {
            throw GradleException("Module '${moduleLayer}' cannot use '${dependencyLayer}'")
        }
    }

    private fun ArchitectureLayer.SnowFlake.validFor(layer: ArchitectureLayer) =
        this.path == "${layer.path}-snowflake-components"
                || this.path == "${layer.path}-snowflakes"


    /**
     * This list should be removed once the modules are refactored to follow our architecture
     */
    private val dependencyExceptions = listOf(
        ":feature:sync",
        ":feature:transfers:transfers-snowflake-components",
        ":shared:original-core-ui", //this should be replaced by core-ui library
    )

    private val moduleExceptions = listOf(
        ":core:ui-components:node-components", //this module will be deleted because it's not following the architecture itself
        ":feature:photos:photos-snowflake-components" //this library should be refactored or moved to shared folder, as it's used by multiple modules
    )

    sealed interface ArchitectureLayer {
        val path: String
        val allowedDependencies: List<Class<out ArchitectureLayer>>

        data class Feature(override val path: String) : ArchitectureLayer {
            override val allowedDependencies: List<Class<out ArchitectureLayer>> = listOf(
                Core::class.java,
                Shared::class.java,
                SnowFlake::class.java,
                Resources::class.java,
            )
        }

        data class Shared(override val path: String) : ArchitectureLayer {
            override val allowedDependencies: List<Class<out ArchitectureLayer>> = listOf(
                Core::class.java,
                SnowFlake::class.java,
                Resources::class.java,
            )
        }

        data class SnowFlake(override val path: String) : ArchitectureLayer {
            override val allowedDependencies: List<Class<out ArchitectureLayer>> = listOf(
                Core::class.java,
                Resources::class.java,
            )
        }

        data class Core(override val path: String) : ArchitectureLayer {
            override val allowedDependencies: List<Class<out ArchitectureLayer>> = listOf(
                Resources::class.java,
                Core::class.java //this should be removed once the modules are refactored to avoid circular dependencies
            )
        }

        data class Resources(override val path: String) : ArchitectureLayer {
            override val allowedDependencies: List<Class<out ArchitectureLayer>> = listOf(
            )
        }
    }

    private fun String.layer(): ArchitectureLayer? =
        when {
            this.endsWith("-snowflake-components")
                    || this.endsWith("-snowflakes") -> ArchitectureLayer.SnowFlake(this)

            this.startsWith(":feature:") -> ArchitectureLayer.Feature(this)
            this.startsWith(":shared:") -> ArchitectureLayer.Shared(this)
            this.startsWith(":core:") -> ArchitectureLayer.Core(this)
            this.startsWith(":resources:") -> ArchitectureLayer.Resources(this)
            else -> null
        }
}