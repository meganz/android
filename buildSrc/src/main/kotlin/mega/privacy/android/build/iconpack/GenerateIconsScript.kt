package mega.privacy.android.build.iconpack

import mega.privacy.android.build.iconpack.kotlingenerator.GenerateIconPainters
import java.io.File

/**
 * Script to generate Icons from icon-pack resources.
 * It searches for all the drawable resources that follows this [DRAWABLE_REGEX_PATTERN] pattern in default drawable folder in icon-pack module.
 */
object GenerateIconsScript {

    /**
     * Runnable script
     */
    @JvmStatic
    fun main(args: Array<String>) {
        this(
            outputDirPath = args[0],
            drawablesPath = args[1],
            mainObjectPackage = args[2],
            mainObjectName = args[3],
        )
    }

    /**
     * Invoke
     */
    operator fun invoke(
        outputDirPath: String,
        drawablesPath: String,
        mainObjectPackage: String,
        mainObjectName: String,
    ) {
        GenerateIconPainters(
            outputDir = File(outputDirPath),
            drawablesPath = drawablesPath,
            mainObjectPackage = mainObjectPackage,
            mainObjectName = mainObjectName,
        )
    }
}

