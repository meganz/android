package mega.privacy.android.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        composeOptions {
            kotlinCompilerExtensionVersion =
                androidx.findVersion("compose-compiler").get().toString()
        }

        dependencies {
            add("implementation", platform(androidx.findLibrary("compose-bom").get()))
            add("testImplementation", testlib.findLibrary("compose-junit").get())
        }

        testOptions {
            unitTests {
                // For Robolectric
                isIncludeAndroidResources = true
            }
        }
    }
}