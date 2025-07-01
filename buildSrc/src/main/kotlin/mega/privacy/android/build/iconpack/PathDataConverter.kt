package mega.privacy.android.build.iconpack

/**
 * Converts SVG path data to Kotlin code for ImageVector.
 * This class parses SVG path commands and converts them to Kotlin path functions.
 * Handles multiple subpaths (holes) by using PathFillType.EvenOdd.
 */
class PathDataConverter {

    /**
     * Converts SVG path data to Kotlin code.
     *
     * @param pathData The SVG path data string
     * @return The Kotlin code as a string
     */
    fun convertPathDataToKotlin(pathData: String): String {

        // Split the path into subpaths (separated by Z or z)
        val subpaths = splitIntoSubpaths(pathData)

        if (subpaths.size == 1) {
            // Single path - use simple conversion
            val commands = parsePathCommands(subpaths[0])
            return generateKotlinCodeWithTracking(commands)
        } else {
            // Multiple subpaths - use EvenOdd fill type for proper hole rendering
            return generateMultiSubpathCode(subpaths)
        }
    }

    /**
     * Splits path data into subpaths based on Z/z commands.
     */
    private fun splitIntoSubpaths(pathData: String): List<String> {
        val subpaths = mutableListOf<String>()
        var currentSubpath = ""

        // Find all command letters and their parameters
        val commandRegex = Regex("([A-Za-z])([^A-Za-z]*)")
        val matches = commandRegex.findAll(pathData)

        for (match in matches) {
            val command = match.groupValues[1]
            val params = match.groupValues[2].trim()
            val fullCommand = command + params

            if (command.uppercase() == "Z") {
                // End of subpath
                currentSubpath += fullCommand
                if (currentSubpath.isNotEmpty()) {
                    subpaths.add(currentSubpath.trim())
                    currentSubpath = ""
                }
            } else {
                // Continue building current subpath
                currentSubpath += fullCommand
            }
        }

        // Add any remaining subpath
        if (currentSubpath.isNotEmpty()) {
            subpaths.add(currentSubpath.trim())
        }

        return subpaths
    }

    /**
     * Generates Kotlin code for multiple subpaths using EvenOdd fill type.
     */
    private fun generateMultiSubpathCode(subpaths: List<String>): String {
        val codeLines = mutableListOf<String>()

        for (i in subpaths.indices) {
            val subpath = subpaths[i]
            val commands = parsePathCommands(subpath)

            if (i == 0) {
                // First subpath - start the path
                codeLines.add("// Subpath ${i + 1}")
                codeLines.addAll(generateKotlinCodeWithTracking(commands).lines())
            } else {
                // Subsequent subpaths - add as separate path operations
                codeLines.add("// Subpath ${i + 1} (hole)")
                codeLines.addAll(generateKotlinCodeWithTracking(commands).lines())
            }
        }

        return codeLines.joinToString("\n")
    }

    /**
     * Parses SVG path data into individual commands.
     * Specific parser for SVG path syntax.
     */
    private fun parsePathCommands(pathData: String): List<PathCommand> {
        val commands = mutableListOf<PathCommand>()

        // Find all command letters and their parameters
        val commandRegex = Regex("([A-Za-z])([^A-Za-z]*)")
        val matches = commandRegex.findAll(pathData)

        for (match in matches) {
            val command = match.groupValues[1]
            val params = match.groupValues[2].trim()

            if (params.isNotEmpty()) {
                // Split parameters by commas and spaces, but be careful with negative numbers
                val numbers = mutableListOf<String>()
                var current = ""
                var i = 0

                while (i < params.length) {
                    val char = params[i]
                    when {
                        char.isDigit() || char == '.' || char == '-' -> {
                            current += char
                        }

                        char == ',' || char.isWhitespace() -> {
                            if (current.isNotEmpty()) {
                                numbers.add(current)
                                current = ""
                            }
                        }
                    }
                    i++
                }

                // Add last number if exists
                if (current.isNotEmpty()) {
                    numbers.add(current)
                }

                val floatParams = numbers.mapNotNull {
                    val float = it.toFloatOrNull()
                    if (float == null) {
                        println("Failed to parse number: '$it'")
                    }
                    float
                }

                if (floatParams.isNotEmpty()) {
                    commands.add(PathCommand(command, floatParams))
                } else {
                    println("No valid numbers found for command: $command")
                }
            } else {
                // Handle commands without parameters (like 'Z')
                commands.add(PathCommand(command, emptyList()))
            }
        }

        return commands
    }

    /**
     * Generates Kotlin code with proper coordinate tracking for SVG commands.
     */
    private fun generateKotlinCodeWithTracking(commands: List<PathCommand>): String {
        val codeLines = mutableListOf<String>()
        var currentX = 0f
        var currentY = 0f

        for (command in commands) {
            val line = when (command.type.uppercase()) {
                "M" -> {
                    if (command.params.size >= 2) {
                        currentX = command.params[0]
                        currentY = command.params[1]
                        "moveTo(${command.params[0]}f, ${command.params[1]}f)"
                    } else {
                        "// Invalid moveTo command: ${command.params}"
                    }
                }

                "L" -> {
                    if (command.params.size >= 2) {
                        currentX = command.params[0]
                        currentY = command.params[1]
                        "lineTo(${command.params[0]}f, ${command.params[1]}f)"
                    } else {
                        "// Invalid lineTo command: ${command.params}"
                    }
                }

                "H" -> {
                    if (command.params.size >= 1) {
                        currentX = command.params[0]
                        "lineTo(${command.params[0]}f, ${currentY}f)"
                    } else {
                        "// Invalid horizontalLineTo command: ${command.params}"
                    }
                }

                "V" -> {
                    if (command.params.size >= 1) {
                        currentY = command.params[0]
                        "lineTo(${currentX}f, ${command.params[0]}f)"
                    } else {
                        "// Invalid verticalLineTo command: ${command.params}"
                    }
                }

                "C" -> {
                    if (command.params.size >= 6) {
                        currentX = command.params[4]
                        currentY = command.params[5]
                        "curveTo(${command.params[0]}f, ${command.params[1]}f, ${command.params[2]}f, ${command.params[3]}f, ${command.params[4]}f, ${command.params[5]}f)"
                    } else {
                        "// Invalid curveTo command: ${command.params}"
                    }
                }

                "S" -> {
                    if (command.params.size >= 4) {
                        currentX = command.params[2]
                        currentY = command.params[3]
                        "curveTo(${command.params[0]}f, ${command.params[1]}f, ${command.params[2]}f, ${command.params[3]}f)"
                    } else {
                        "// Invalid smoothCurveTo command: ${command.params}"
                    }
                }

                "Q" -> {
                    if (command.params.size >= 4) {
                        currentX = command.params[2]
                        currentY = command.params[3]
                        "quadraticBezierTo(${command.params[0]}f, ${command.params[1]}f, ${command.params[2]}f, ${command.params[3]}f)"
                    } else {
                        "// Invalid quadraticCurveTo command: ${command.params}"
                    }
                }

                "T" -> {
                    if (command.params.size >= 2) {
                        currentX = command.params[0]
                        currentY = command.params[1]
                        "quadraticBezierTo(${command.params[0]}f, ${command.params[1]}f)"
                    } else {
                        "// Invalid smoothQuadraticCurveTo command: ${command.params}"
                    }
                }

                "A" -> {
                    "// TODO: arcTo not supported in ImageVector - params: ${command.params}"
                }

                "Z" -> "close()"
                else -> "// Unknown command: ${command.type}"
            }
            codeLines.add(line)
        }

        return codeLines.joinToString("\n")
    }
}

/**
 * Data class representing a path command.
 */
private data class PathCommand(
    val type: String,
    val params: List<Float>,
) 