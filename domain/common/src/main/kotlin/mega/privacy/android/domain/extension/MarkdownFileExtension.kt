package mega.privacy.android.domain.extension

/** Markdown file extensions recognised by the text editor. */
private val MARKDOWN_EXTENSIONS = setOf("md", "markdown")

/**
 * Returns true when this file name has a Markdown extension (`.md` / `.markdown`),
 * case-insensitive. Single source of truth for Markdown detection.
 */
fun String.isMarkdownFile(): Boolean =
    contains('.') && substringAfterLast('.', "").lowercase() in MARKDOWN_EXTENSIONS
