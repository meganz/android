import groovy.lang.Closure
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate

fun DependencyHandlerScope.debugLocalKotlinCompilerPluginClasspath(
    gradle: Gradle,
    extra: ExtraPropertiesExtension,
    dependency: Provider<MinimalExternalModuleDependency>,
) {
    val isServerBuild: Closure<Boolean> by extra

    for (taskName in gradle.startParameter.taskNames) {
        val name = taskName.lowercase()
        if (name.contains("debug") && !name.contains("test") && !isServerBuild()) {
            "kotlinCompilerPluginClasspath"(dependency)
            break
        }
    }
}
