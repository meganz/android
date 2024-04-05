package mega.privacy.android.build

import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.project

/**
 * Set up prebuilt SDK dependency
 *
 * @param extra
 */
fun DependencyHandlerScope.preBuiltSdkDependency(extra: ExtraPropertiesExtension) {
    if (shouldUsePrebuiltSdk()) {
        println("use remote pre-built SDK")
        val megaSdkVersion = extra.get("megaSdkVersion") as String
        "implementation"("nz.mega.sdk:sdk:$megaSdkVersion")
    } else {
        println("use local SDK")
        "implementation"(project(":sdk"))
    }
}
