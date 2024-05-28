package mega.privacy.android.shared.original.core.ui.buildscripts

import mega.android.core.ui.tokens.buildscripts.kotlingenerator.GenerateColorTokens

/**
 * Runs the Script to generate Kotlin files with the tokens from json files
 */
fun main(args: Array<String>) {
    GenerateColorTokens().generateThemeForAndroidTemp(
        moduleName = "shared/original-core-ui",
        packageName = "mega.privacy.android.shared.original.core.ui.theme.values",
        themePrefix = "Temp",
        groupsToExpose = listOf("Text"),
        jsonGroupNameForCoreColorTokens = "Core/Android-temp",
    )
}