package mega.privacy.android.feature.texteditor.presentation.model

/**
 * Top bar actions whose visibility is conditional (based on node source and mode).
 * Line numbers and More are always visible and are not included here.
 */
enum class TextEditorConditionalTopBarAction {
    Download,
    GetLink,
    SendToChat,
    Share,
}
