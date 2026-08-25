package mega.privacy.android.feature.texteditor.presentation.model

/**
 * State of the insert/edit link dialog opened from the formatting toolbar.
 *
 * @property text Initial display text (the selection, or the existing link's text).
 * @property url Initial URL (empty for a new link).
 * @property isExistingLink True when the dialog edits an existing link span (the cursor sat
 * inside one when it opened); enables the remove action.
 */
data class MarkdownLinkDialogUiState(
    val text: String,
    val url: String,
    val isExistingLink: Boolean = false,
)
