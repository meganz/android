package mega.privacy.android.feature.texteditor.presentation.model

/**
 * Actions for the text editor bottom floating bar (Phase 4).
 * Visibility is conditional based on node source and mode. The top bar shows only Line numbers and More.
 */
enum class TextEditorConditionalTopBarAction {
    Download,
    GetLink,
    SendToChat,
    Share,
}
