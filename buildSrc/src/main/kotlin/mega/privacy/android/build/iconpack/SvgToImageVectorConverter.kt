package mega.privacy.android.build.iconpack

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Converts SVG files to ImageVector code using a custom SVG parser.
 * This converter first converts SVG to vector drawable XML, then converts that to ImageVector code.
 */
class SvgToImageVectorConverter(
    private val svgDirectory: File?,
) {
    private val xmlConverter = XmlToImageVectorConverter("temp_drawables")

    /**
     * Converts an SVG file to ImageVector code.
     *
     * @param svgFileName The name of the SVG file (without extension)
     * @return The ImageVector code as a string, or null if conversion fails
     */
    fun convertSvgToImageVector(svgFileName: String): String? {
        if (svgDirectory == null || !svgDirectory.exists()) {
            println("SVG directory not found or not configured")
            return null
        }

        val svgFile = File(svgDirectory, "$svgFileName.svg")
        if (!svgFile.exists()) {
            println("SVG file not found: ${svgFile.absolutePath}")
            return null
        }

        try {
            // Convert SVG to vector drawable XML using our custom parser
            val vectorDrawableXml = convertSvgToVectorDrawable(svgFile)
            if (vectorDrawableXml != null) {
                // Convert vector drawable XML to ImageVector code
                return convertVectorDrawableToImageVector(
                    vectorDrawableXml,
                    svgFileName,
                )
            } else {
                println("Failed to convert SVG to vector drawable: ${svgFile.absolutePath}")
                return null
            }
        } catch (e: Exception) {
            println("Error converting SVG to ImageVector: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Converts SVG file to vector drawable XML using our custom parser.
     */
    private fun convertSvgToVectorDrawable(svgFile: File): String? {
        return try {
            // Read SVG content
            val svgContent = svgFile.readText()

            // Use our custom parser to convert SVG to vector drawable
            val vectorDrawable = Svg2ToXmlVectorParser.parseSvgToXml(svgContent, svgFile.name)

            // Convert the result to a proper vector drawable XML
            val vectorDrawableXml = convertSvg2VectorResultToXml(vectorDrawable)

            println("Successfully converted SVG to vector drawable")
            vectorDrawableXml

        } catch (e: Exception) {
            println("Error in SVG conversion: ${e.message}")
            null
        }
    }

    /**
     * Converts Svg2Vector result to proper vector drawable XML.
     */
    private fun convertSvg2VectorResultToXml(svg2VectorResult: Svg2ToXmlVectorParser.Svg2VectorResult): String {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = doc.createElement("vector")

        // Set vector attributes
        root.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android")
        root.setAttribute("android:width", "${svg2VectorResult.width}dp")
        root.setAttribute("android:height", "${svg2VectorResult.height}dp")
        root.setAttribute("android:viewportWidth", svg2VectorResult.viewportWidth.toString())
        root.setAttribute("android:viewportHeight", svg2VectorResult.viewportHeight.toString())

        // Add path elements
        svg2VectorResult.paths.forEach { path ->
            val pathElement = doc.createElement("path")
            pathElement.setAttribute("android:pathData", path.pathData)
            pathElement.setAttribute("android:fillColor", path.fillColor)
            if (path.strokeColor != null) {
                pathElement.setAttribute("android:strokeColor", path.strokeColor)
                pathElement.setAttribute("android:strokeWidth", path.strokeWidth.toString())
            }
            root.appendChild(pathElement)
        }

        doc.appendChild(root)

        // Convert to string
        val transformer = TransformerFactory.newInstance().newTransformer()
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))

        return writer.toString()
    }

    /**
     * Converts vector drawable XML to ImageVector code.
     */
    private fun convertVectorDrawableToImageVector(
        vectorDrawableXml: String,
        iconName: String,
    ): String? {
        // Create a temporary file with the vector drawable XML
        val tempFile = File.createTempFile("temp_vector_", ".xml")
        tempFile.writeText(vectorDrawableXml)

        try {
            // Use our existing XML converter to convert the vector drawable to ImageVector
            return xmlConverter.convertXmlToImageVector(tempFile, iconName)
        } finally {
            tempFile.delete()
        }
    }
}

/**
 * Custom SVG parser that converts SVG content to vector drawable format.
 * This implementation is lightweight and handles the most common SVG elements
 * needed for icon conversion (paths, basic attributes, colors).
 */
object Svg2ToXmlVectorParser {
    data class Svg2VectorResult(
        val width: Int,
        val height: Int,
        val viewportWidth: Float,
        val viewportHeight: Float,
        val paths: List<PathData>,
    )

    data class PathData(
        val pathData: String,
        val fillColor: String,
        val strokeColor: String? = null,
        val strokeWidth: Float = 0f,
    )

    /**
     * Parses SVG content and converts it to vector drawable format.
     *
     * @param svgContent The SVG content as a string
     * @param fileName The name of the SVG file (for logging purposes)
     * @return Svg2VectorResult containing the parsed data
     */
    fun parseSvgToXml(svgContent: String, fileName: String): Svg2VectorResult {
        // Parse SVG to extract basic information
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(svgContent.toByteArray()))

        val svgElement = doc.documentElement
        val width = svgElement.getAttribute("width").removeSuffix("px").toIntOrNull() ?: 24
        val height = svgElement.getAttribute("height").removeSuffix("px").toIntOrNull() ?: 24
        val viewBox = svgElement.getAttribute("viewBox")
        val viewportWidth = viewBox.split(" ").getOrNull(2)?.toFloatOrNull() ?: 24f
        val viewportHeight = viewBox.split(" ").getOrNull(3)?.toFloatOrNull() ?: 24f

        // Extract path elements
        val paths = mutableListOf<PathData>()
        val pathElements = svgElement.getElementsByTagName("path")

        for (i in 0 until pathElements.length) {
            val pathElement = pathElements.item(i) as Element
            val pathData = pathElement.getAttribute("d")
            val fill = pathElement.getAttribute("fill")
            val stroke = pathElement.getAttribute("stroke")
            val strokeWidth = pathElement.getAttribute("stroke-width").toFloatOrNull() ?: 0f

            if (pathData.isNotEmpty()) {
                paths.add(
                    PathData(
                        pathData = pathData,
                        fillColor = if (fill.isNotEmpty() && fill != "none") fill else "#000000",
                        strokeColor = if (stroke.isNotEmpty() && stroke != "none") stroke else null,
                        strokeWidth = strokeWidth
                    )
                )
            }
        }

        return Svg2VectorResult(width, height, viewportWidth, viewportHeight, paths)
    }
} 