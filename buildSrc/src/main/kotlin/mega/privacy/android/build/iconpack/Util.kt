package mega.privacy.android.build.iconpack

internal fun String.indentLines(indentLevel: Int = 4): String {
    val indent = " ".repeat(indentLevel)
    return this.lines().joinToString("\n") { line ->
        if (line.isNotBlank()) {
            indent + line
        } else {
            line
        }
    }
}