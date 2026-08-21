package mega.privacy.android.feature.texteditor.presentation.model

/**
 * How a Markdown document is edited when WYSIWYG editing is available.
 *
 * [Markdown] shows the raw source with live styling (syntax visible, dimmed).
 * [RichText] renders the document as formatted blocks with tap-to-edit (no syntax visible).
 */
enum class MarkdownEditMode {
    Markdown,
    RichText,
}
