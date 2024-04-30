package mega.privacy.android.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * common configuration for Android with Kotlin
 *
 * @param commonExtension
 */
internal fun Project.configureKotlinAndroidLibrary(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    configureKotlinAndroidApplication(commonExtension)

    commonExtension.apply {
        defaultConfig {
            val targetSdkVersion: Int by rootProject.extra
            testOptions.targetSdk = targetSdkVersion
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            val jdk: String by rootProject.extra
            jvmTarget = jdk
            val shouldSuppressWarnings: Boolean by rootProject.extra
            suppressWarnings = shouldSuppressWarnings
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }
}


/**
 * common configuration for Android with Kotlin
 *
 * @param commonExtension
 */
internal fun Project.configureKotlinAndroidApplication(
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
            lint.targetSdk = targetSdkVersion

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        configure<KotlinAndroidProjectExtension> {
            val jdk: String by rootProject.extra
            jvmToolchain(jdk.toInt())
        }

        compileOptions {
            val javaVersion: JavaVersion by rootProject.extra
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }
}