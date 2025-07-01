package mega.privacy.android.build.iconpack

import java.io.File

/**
 * Script to generate IconPack with ImageVector properties from IconPackInterface.
 * This script reads the IconPackInterface to understand what icons should be generated
 * and then creates the implementation.
 */
object GenerateIconVectorsScript {

    /**
     * Runnable script
     */
    @JvmStatic
    fun main(args: Array<String>) {
        this(
            outputDirPath = args[0],
            drawablesPath = args[1],
            svgDirectoryPath = args[2],
            interfaceFilePath = args[3],
            mainObjectPackage = args[4],
            mainObjectName = args[5],
        )
    }

    /**
     * Invoke
     * Generates the IconPack object with ImageVector properties
     * from the provided interface file.
     *
     * @param outputDirPath Path to the output directory where the generated code will be saved
     * @param drawablesPath Path to the directory containing XML drawable files
     * @param svgDirectoryPath Path to the directory containing SVG files
     * @param interfaceFilePath Path to the IconPackInterface file that defines the icons
     * @param mainObjectPackage Package name for the generated IconPack object
     * @param mainObjectName Name of the generated IconPack object
     */
    operator fun invoke(
        outputDirPath: String,
        drawablesPath: String,
        svgDirectoryPath: String,
        interfaceFilePath: String,
        mainObjectPackage: String,
        mainObjectName: String,
    ) {
        // Read the interface to understand what icons should be generated
        val iconDefinitions = InterfaceReader(interfaceFilePath).readIconDefinitions()

        println("Found ${iconDefinitions.size} icons to generate from interface")

        // Create converters
        val xmlConverter = XmlToImageVectorConverter(drawablesPath)
        val svgConverter = SvgToImageVectorConverter(File(svgDirectoryPath))

        // Generate the IconPack
        GenerateIconVectors(
            outputDir = File(outputDirPath),
            mainObjectPackage = mainObjectPackage,
            mainObjectName = mainObjectName,
            iconDefinitions = iconDefinitions,
            xmlConverter = xmlConverter,
            svgConverter = svgConverter
        ).generate()
    }
} 