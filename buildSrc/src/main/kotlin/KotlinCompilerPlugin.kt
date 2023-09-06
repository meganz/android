import org.gradle.StartParameter
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.debugKotlinCompilerPluginClasspath(
    startParameter: StartParameter,
    dependency: Provider<MinimalExternalModuleDependency>
) {
    startParameter.taskNames.forEach { taskName ->
        val normalizedTaskName = taskName.lowercase()
        if (normalizedTaskName.contains("debug") && !normalizedTaskName.contains("test")) {
            "kotlinCompilerPluginClasspath"(dependency)
        }
    }
}
