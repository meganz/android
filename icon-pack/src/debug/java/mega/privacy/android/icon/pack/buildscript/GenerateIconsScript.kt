package mega.privacy.android.icon.pack.buildscript

import mega.privacy.android.icon.pack.buildscript.kotlingenerator.APP_ICONS_OBJECT_DESTINATION_PATH
import mega.privacy.android.icon.pack.buildscript.kotlingenerator.GenerateIconPainters
import java.io.File

/**
 * Script to generate Icons from icon-pack resources.
 * It searches for all the drawable resources that follows this [DRAWABLE_REGEX_PATTERN] pattern in default drawable folder in icon-pack module.
 */
fun main(args: Array<String>) {
    val outputDir = File(APP_ICONS_OBJECT_DESTINATION_PATH)
    GenerateIconPainters(outputDir)
}

