package mega.privacy.android.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate

/**
 * common configuration for Android with Kotlin
 *
 * @param commonExtension
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        val compileSdkVersion: Int by rootProject.extra
        compileSdk = compileSdkVersion

        val buildTools: String by rootProject.extra
        buildToolsVersion = buildTools

        defaultConfig {
            val minSdkVersion: Int by rootProject.extra
            minSdk = minSdkVersion

            val targetSdkVersion: Int by rootProject.extra
            testOptions.targetSdk = targetSdkVersion
            lint.targetSdk = targetSdkVersion

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            val javaVersion: JavaVersion by rootProject.extra
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }

}