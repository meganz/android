package mega.privacy.android.feature.texteditor.presentation.model

/**
 * Top bar actions for the Compose text editor.
 * Other node actions (rename, move, etc.) are available via the Node Options Bottom Sheet (More).
 */
sealed interface TextEditorTopBarAction {
    data object Download : TextEditorTopBarAction
    data object LineNumbers : TextEditorTopBarAction
    data object GetLink : TextEditorTopBarAction
    data object Share : TextEditorTopBarAction
}
