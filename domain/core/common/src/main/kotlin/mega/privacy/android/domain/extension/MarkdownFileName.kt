package mega.privacy.android.domain.extension

/** Markdown file extensions recognised by the text editor. */
private val MARKDOWN_EXTENSIONS = setOf("md", "markdown")

/**
 * Returns true when [fileName] has a Markdown extension (`.md` / `.markdown`),
 * case-insensitive. Single source of truth for Markdown detection.
 */
fun isMarkdownFileName(fileName: String): Boolean =
    fileName.contains('.') && fileName.substringAfterLast('.', "").lowercase() in MARKDOWN_EXTENSIONS
