package mega.privacy.android.gradle

import org.gradle.api.Project

/**
 * This the build optimisation to avoid run hilt and kapt plugin when running unit test.
 *
 * @return true if no test or unit test in the gradle commands, false otherwise.
 */
fun Project.shouldApplyDefaultConfiguration(): Boolean =
    !gradle.startParameter.taskNames.any { it.contains(":test") && it.contains("UnitTest") }