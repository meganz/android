package mega.privacy.android.feature.texteditor.presentation.model

import mega.privacy.android.domain.entity.texteditor.TextEditorMode

/**
 * UI state for the Compose text editor screen.
 */
data class TextEditorComposeUiState(
    val fileName: String = "",
    val isLoading: Boolean = false,
    val mode: TextEditorMode = TextEditorMode.View,
    val isFileEdited: Boolean = false,
    val showLineNumbers: Boolean = false,
)
