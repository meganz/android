package mega.privacy.android.core.theme.buildscripts

import mega.privacy.android.core.ui.buildscripts.GenerateTokens

/**
 * Runs the Script to generate Kotlin files with the tokens from json files
 */
fun main(args: Array<String>) {
    GenerateTokens().generate(
        appPrefix = "MegaApp",
        packageName = "mega.privacy.android.core.theme.tokens",
        destinationPath = "core/theme/src/main/java",
        assetsFolder = "designSystemAssets"
    )
}