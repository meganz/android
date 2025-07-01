package mega.privacy.android.build.iconpack

import java.io.File

/**
 * Converts XML drawable files to ImageVector code.
 */
class XmlToImageVectorConverter(
    private val drawablesPath: String,
) {
    private val xmlParser = XmlDrawableParser()
    private val pathConverter = PathDataConverter()

    /**
     * Converts an XML drawable file to ImageVector code.
     *
     * @param drawableFileName The name of the drawable file (without extension)
     * @return The ImageVector code as a string, or null if conversion fails
     */
    fun convertXmlToImageVector(drawableFileName: String) =
        convertXmlToImageVector(File(drawablesPath, "$drawableFileName.xml"), drawableFileName)

    /**
     * Converts an XML drawable file to ImageVector code.
     *
     * @param drawableFile The XML drawable file to convert
     * @return The ImageVector code as a string, or null if conversion fails
     */
    fun convertXmlToImageVector(drawableFile: File, name: String): String? {
        if (!drawableFile.exists()) {
            println("Drawable file not found: ${drawableFile.absolutePath}")
            return null
        }

        try {
            val imageVectorData = xmlParser.parseXmlDrawable(drawableFile)
            if (imageVectorData != null) {
                return generateImageVectorFromData(imageVectorData, name)
            } else {
                println("Failed to parse XML drawable: ${drawableFile.absolutePath}")
                return null
            }
        } catch (e: Exception) {
            println("Error converting XML to ImageVector: ${e.message}")
            return null
        }
    }

    /**
     * Generates ImageVector code from parsed XML data.
     */
    private fun generateImageVectorFromData(
        data: ImageVectorData,
        name: String,
    ): String {
        val pathCode = data.paths.joinToString("\n") { path ->
            generatePathCode(path)
        }

        return """
            ImageVector.Builder(
                name = "${data.name ?: name}",
                defaultWidth = ${data.width}.dp,
                defaultHeight = ${data.height}.dp,
                viewportWidth = ${data.viewportWidth}f,
                viewportHeight = ${data.viewportHeight}f
            ).apply {
${pathCode.indentLines(16)}
            }.build()
        """.trimIndent()
    }

    /**
     * Generates path code for a single path element.
     */
    private fun generatePathCode(path: PathData): String {
        val strokeParam = if (path.strokeColor != "null") {
            "SolidColor(${path.strokeColor})"
        } else {
            "null"
        }

        val pathCommands = pathConverter.convertPathDataToKotlin(path.pathData)

        // Check if the path has multiple subpaths (holes) by looking for multiple Z commands
        val hasMultipleSubpaths = path.pathData.count { it.uppercase() == "Z" } > 1
        val fillType = if (hasMultipleSubpaths) {
            "PathFillType.EvenOdd"
        } else {
            "PathFillType.NonZero"
        }

        return """
            path(
                fill = SolidColor(${path.fillColor}),
                fillAlpha = ${path.fillAlpha}f,
                stroke = $strokeParam,
                strokeAlpha = ${path.strokeAlpha}f,
                strokeLineWidth = ${path.strokeWidth}f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 4.0f,
                pathFillType = $fillType,
            ) {
${pathCommands.indentLines(16)}
            }
        """.trimIndent()
    }
} 