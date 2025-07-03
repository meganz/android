package mega.privacy.android.build.iconpack

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses XML drawable files and extracts vector path data.
 * This class reads Android Vector Drawable XML files and converts them to ImageVector code.
 */
internal class XmlDrawableParser {

    /**
     * Parses an XML drawable file and returns the ImageVector code.
     *
     * @param xmlFile The XML drawable file to parse
     * @return The ImageVector code as a string, or null if parsing fails
     */
    internal fun parseXmlDrawable(xmlFile: File): ImageVectorData? {
        if (!xmlFile.exists()) {
            println("XML file not found: ${xmlFile.absolutePath}")
            return null
        }

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true // Important for Android XML
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(xmlFile.inputStream())
            document.documentElement.normalize()

            return parseVectorDrawable(document)
        } catch (e: Exception) {
            println("Error parsing XML drawable: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Parses a vector drawable document and extracts the ImageVector data.
     */
    private fun parseVectorDrawable(document: Document): ImageVectorData? {

        val vectorElements = document.getElementsByTagName("vector")

        if (vectorElements.length == 0) {
            // Try with namespace
            val vectorElementsWithNS = document.getElementsByTagNameNS("*", "vector")

            if (vectorElementsWithNS.length == 0) {
                println("No vector element found in document")
                return null
            }
        }

        val vectorElement = vectorElements.item(0) as? Element
            ?: return null

        val width = parseDimension(vectorElement.getAttribute("android:width"), 24f)
        val height = parseDimension(vectorElement.getAttribute("android:height"), 24f)
        val viewportWidth =
            vectorElement.getAttribute("android:viewportWidth").toFloatOrNull() ?: 24f
        val viewportHeight =
            vectorElement.getAttribute("android:viewportHeight").toFloatOrNull() ?: 24f

        val paths = mutableListOf<PathData>()
        val pathElements = vectorElement.getElementsByTagName("path")

        for (i in 0 until pathElements.length) {
            val pathElement = pathElements.item(i) as Element
            val pathData = parsePathElement(pathElement)
            if (pathData != null) {
                paths.add(pathData)
            } else {
                println("Failed to parse path element $i")
            }
        }

        if (paths.isEmpty()) {
            println("No valid paths found")
            return null
        }

        return ImageVectorData(
            name = vectorElement.getAttribute("android:name").takeUnless { it.isEmpty() },
            width = width,
            height = height,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            paths = paths
        )
    }

    /**
     * Parses a dimension value (e.g., "24dp", "24", etc.).
     */
    private fun parseDimension(value: String, default: Float): Float {
        return when {
            value.isEmpty() -> default
            value.endsWith("dp") -> value.removeSuffix("dp").toFloatOrNull() ?: default
            value.endsWith("px") -> value.removeSuffix("px").toFloatOrNull() ?: default
            else -> value.toFloatOrNull() ?: default
        }
    }

    /**
     * Parses a path element and extracts the path data.
     */
    private fun parsePathElement(pathElement: Element): PathData? {
        val pathData = pathElement.getAttribute("android:pathData")
        if (pathData.isEmpty()) {
            println("Path data is empty")
            return null
        }

        val fillColor = parseColor(pathElement.getAttribute("android:fillColor"), "Color.Black")
        val strokeColor = parseColor(pathElement.getAttribute("android:strokeColor"), "null")
        val strokeWidth = pathElement.getAttribute("android:strokeWidth").toFloatOrNull() ?: 0f
        val fillAlpha = pathElement.getAttribute("android:fillAlpha").toFloatOrNull() ?: 1.0f
        val strokeAlpha = pathElement.getAttribute("android:strokeAlpha").toFloatOrNull() ?: 1.0f

        return PathData(
            pathData = pathData,
            fillColor = fillColor,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            fillAlpha = fillAlpha,
            strokeAlpha = strokeAlpha
        )
    }

    /**
     * Parses a color value and converts it to Kotlin code.
     */
    private fun parseColor(color: String, default: String): String {
        return when {
            color.isEmpty() -> default
            color.startsWith("#") -> {
                val hex = color.removePrefix("#")
                when (hex.length) {
                    3 -> "Color(0xFF${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]})"
                    6 -> "Color(0xFF$hex)"
                    8 -> "Color(0x$hex)"
                    else -> "Color.Black"
                }
            }

            else -> "Color.Black"
        }
    }
}

/**
 * Data class representing the parsed ImageVector data.
 */
internal data class ImageVectorData(
    val name: String?,
    val width: Float,
    val height: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val paths: List<PathData>,
)

/**
 * Data class representing a path in the vector drawable.
 */
internal data class PathData(
    val pathData: String,
    val fillColor: String,
    val strokeColor: String,
    val strokeWidth: Float,
    val fillAlpha: Float,
    val strokeAlpha: Float,
) 