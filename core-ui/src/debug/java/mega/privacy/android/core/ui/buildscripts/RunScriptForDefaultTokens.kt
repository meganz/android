package mega.privacy.android.core.ui.buildscripts

import mega.privacy.android.core.ui.buildscripts.GenerateTokens.Companion.DEFAULT_PACKAGE

/**
 * Runs the Script to generate Kotlin files with the tokens from json files
 */
fun main(args: Array<String>) {
    //this can be set by arguments using Clikt in the future
    GenerateTokens().generate(
        appPrefix = "Default",
        packageName = DEFAULT_PACKAGE,
        destinationPath = "core-ui/src/main/java",
        generateInterfaces = true,
    )
}