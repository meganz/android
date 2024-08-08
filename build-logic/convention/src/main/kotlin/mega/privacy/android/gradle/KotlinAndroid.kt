package mega.privacy.android.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


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
        }

        compileOptions {
            val javaVersion: JavaVersion by rootProject.extra
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }

    configure<KotlinAndroidProjectExtension> {
        val jdk: String by rootProject.extra
        jvmToolchain(jdk.toInt())
    }

    configureKotlin()
}

/**
 * Configure base Kotlin options
 */
fun Project.configureKotlin() {
    // Use withType to workaround https://youtrack.jetbrains.com/issue/KT-55947
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            val shouldSuppressWarnings: Boolean by rootProject.extra
            suppressWarnings = shouldSuppressWarnings
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}



