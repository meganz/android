package mega.privacy.android.feature.texteditor.presentation.model

import mega.privacy.android.feature.texteditor.components.markdown.MarkdownLinkInfo

/**
 * State of the insert/edit link dialog opened from the Markdown formatting toolbar.
 *
 * @property text Initial display text (the selection, or the existing link's text).
 * @property url Initial URL (empty for a new link).
 * @property existingLink The link covering the cursor when the dialog opened, consumed on
 * confirm/remove; null when inserting a new link.
 */
data class MarkdownLinkDialogUiState(
    val text: String,
    val url: String,
    val existingLink: MarkdownLinkInfo? = null,
) {
    /** True when the dialog edits an existing link; enables the remove action. */
    val isExistingLink: Boolean get() = existingLink != null
}
