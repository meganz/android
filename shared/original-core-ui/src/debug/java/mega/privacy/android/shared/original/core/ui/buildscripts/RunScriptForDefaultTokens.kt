package mega.privacy.android.shared.original.core.ui.buildscripts

import mega.privacy.android.shared.original.core.ui.buildscripts.GenerateTokens.Companion.DEFAULT_PACKAGE


/**
 * Runs the Script to generate Kotlin files with the tokens from json files
 */
fun main(args: Array<String>) {
    //this can be set by arguments using Clikt in the future

    //generate the tokens with TEMP core tokens
    GenerateTokens().generate(
        appPrefix = "AndroidTemp",
        packageName = DEFAULT_PACKAGE,
        destinationPath = "shared/original-core-ui/src/main/java",
        generateInterfaces = true,
    )

    //generate the tokens with NEW core tokens
    GenerateTokens().generate(
        appPrefix = "AndroidNew",
        packageName = "$DEFAULT_PACKAGE.new",
        destinationPath = "shared/original-core-ui/src/main/java",
        generateInterfaces = false,
        coreColorsTokensGroupName = "Core/Main"
    )
}