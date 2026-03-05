package mega.privacy.android.feature.texteditor.presentation.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * UI state for the Compose text editor screen.
 */
data class TextEditorComposeUiState(
    val fileName: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val errorEvent: StateEvent = consumed,
    val mode: TextEditorMode = TextEditorMode.View,
    val isFileEdited: Boolean = false,
    val showLineNumbers: Boolean = false,
    val topBarSlots: TextEditorTopBarSlots = DefaultTextEditorTopBarSlots,
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)
