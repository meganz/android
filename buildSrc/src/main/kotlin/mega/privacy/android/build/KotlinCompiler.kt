package mega.privacy.android.build

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.debugLocalKotlinCompilerPluginClasspath(
    gradle: Gradle,
    dependency: Provider<MinimalExternalModuleDependency>,
) {
    for (taskName in gradle.startParameter.taskNames) {
        val name = taskName.lowercase()
        if (name.contains("debug") && !name.contains("test") && !isServerBuild()) {
            "kotlinCompilerPluginClasspath"(dependency)
            break
        }
    }
}
