package mega.privacy.android.domain.entity.texteditor

enum class TextEditorMode(val value: String) {
    Create("CREATE_MODE"),
    View("VIEW_MODE"),
    Edit("EDIT_MODE");
}

/**
 * Maps a string value (e.g. from intents or nav keys) to [TextEditorMode].
 * Returns [TextEditorMode.View] when the value is not recognized.
 */
fun textEditorModeFromValue(value: String): TextEditorMode =
    TextEditorMode.entries.find { it.value == value } ?: TextEditorMode.View
