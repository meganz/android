package mega.privacy.android.feature.texteditor.presentation.model

/**
 * A single slot in the text editor top bar (conditional action, line numbers, or more).
 * Used to define the ordered list of slots to show.
 */
sealed interface TextEditorTopBarSlot {
    data object LineNumbers : TextEditorTopBarSlot
    data object More : TextEditorTopBarSlot
    data class Conditional(val action: TextEditorConditionalTopBarAction) : TextEditorTopBarSlot
}

/**
 * Ordered list of top bar slots to show. Derived from node source type and mode
 * (see legacy text editor activity menu logic).
 */
typealias TextEditorTopBarSlots = List<TextEditorTopBarSlot>

/** Default slots: Download, Line numbers, GetLink, Send to chat, Share, More in that order. */
val DefaultTextEditorTopBarSlots: TextEditorTopBarSlots = listOf(
    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
    TextEditorTopBarSlot.LineNumbers,
    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.GetLink),
    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.SendToChat),
    TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Share),
    TextEditorTopBarSlot.More,
)
