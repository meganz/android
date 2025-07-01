package mega.privacy.android.build.iconpack

import java.io.File

/**
 * Reads the IconPackInterface and extracts the icon properties that need to be implemented.
 * This class parses the interface file to understand what icons should be generated.
 * It reads the file as raw text and uses indention to find the icons and hierarchy as introspection is not possible in gradle tasks.
 * So it's important to correctly format the interface and don't add other levels that could change the indention.
 */
class InterfaceReader(
    private val interfaceFilePath: String,
) {

    /**
     * Reads the interface and extracts all icon properties that need to be implemented.
     *
     * @return List of IconDefinition objects representing the icons to generate
     */
    fun readIconDefinitions(): List<IconDefinition> {
        val interfaceFile = File(interfaceFilePath)
        if (!interfaceFile.exists()) {
            throw IllegalArgumentException("Interface file not found: ${interfaceFile.absolutePath}")
        }

        val interfaceContent = interfaceFile.readText()
        return parseIconProperties(interfaceContent)
    }

    /**
     * Parses the interface content to extract icon properties with their size, weight, and style.
     * This parser looks for the nested interface structure to determine the icon properties.
     */
    private fun parseIconProperties(content: String): List<IconDefinition> {
        val iconDefinitions = mutableListOf<IconDefinition>()

        // Parse the nested structure: interface Size -> interface Weight -> interface Style -> val IconName
        val interfacePattern = Regex("""interface\s+(\w+) \{""")
        val propertyPattern = Regex("""val\s+(\w+):\s*ImageVector""")

        val lines = content.lines()
        var currentSize = ""
        var currentWeight = ""
        var currentStyle = ""
        var currentDepth = 0

        for (line in lines) {
            val trimmedLine = line.trim()
            val indentLevel = (line.length - line.trimStart().length) / indentionChars

            // Check for interface declarations
            val interfaceMatch = interfacePattern.find(trimmedLine)
            if (interfaceMatch != null) {
                val interfaceName = interfaceMatch.groupValues[1]

                // Determine the level based on indentation and context
                when (indentLevel) {
                    1 -> {
                        // Top level interface (Size level)
                        currentSize = interfaceName
                        currentWeight = ""
                        currentStyle = ""
                        currentDepth = 1
                    }

                    2 -> {
                        // Weight level interface
                        currentWeight = interfaceName
                        currentStyle = ""
                        currentDepth = 2
                    }

                    3 -> {
                        // Style level interface
                        currentStyle = interfaceName
                        currentDepth = 3
                    }
                }
            } else {

                // Check for property declarations
                val propertyMatch = propertyPattern.find(trimmedLine)
                if (propertyMatch != null && currentDepth == 3) {
                    val iconName = propertyMatch.groupValues[1]

                    // Only add if we have all the context (size, weight, style)
                    if (currentSize.isNotEmpty() && currentWeight.isNotEmpty() && currentStyle.isNotEmpty()) {
                        val iconDefinition = IconDefinition(
                            name = iconName,
                            size = currentSize,
                            weight = currentWeight,
                            style = currentStyle,
                            sourceName = convertToSnakeCase(iconName)
                        )

                        iconDefinitions.add(iconDefinition)
                        println("Found icon: $currentSize.$currentWeight.$currentStyle.$iconName")
                    }
                }
            }
        }

        return iconDefinitions
    }

    private val indentionChars = 4

    /**
     * Converts a camelCase or PascalCase string to snake_case. Including digits
     * Example: "ArrowUp01" -> "arrow_up_01"
     */
    private fun convertToSnakeCase(input: String): String {
        return input
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("([a-zA-Z])([0-9])"), "$1_$2")
            .lowercase()
    }
} 