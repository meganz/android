package mega.privacy.android.feature.texteditor.presentation.model

import mega.privacy.android.feature.texteditor.components.markdown.MarkdownLinkInfo

/**
 * State of the insert/edit link dialog opened from the Markdown formatting toolbar.
 *
 * @property text Initial display text (the selection, or the existing link's text).
 * @property url Initial URL (empty for a new link).
 * @property existingLink The link covering the cursor when the dialog opened, consumed on
 * confirm/remove; null when inserting a new link.
 * @property isRichExistingLink True when the dialog edits an existing link span in rich text
 * mode (which targets the span through the block state rather than [existingLink]).
 */
data class MarkdownLinkDialogUiState(
    val text: String,
    val url: String,
    val existingLink: MarkdownLinkInfo? = null,
    val isRichExistingLink: Boolean = false,
) {
    /** True when the dialog edits an existing link; enables the remove action. */
    val isExistingLink: Boolean get() = existingLink != null || isRichExistingLink
}
