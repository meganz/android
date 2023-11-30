package mega.privacy.android.shared.theme.buildscripts

import mega.privacy.android.core.ui.buildscripts.GenerateTokens

/**
 * Runs the Script to generate Kotlin files with the tokens from json files
 */
fun main(args: Array<String>) {
    GenerateTokens().generate(
        appPrefix = "MegaApp",
        packageName = "mega.privacy.android.shared.theme.tokens",
        destinationPath = "shared/theme/src/main/java",
        assetsFolder = "designSystemAssets"
    )
}