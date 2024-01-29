package mega.privacy.android.app.textEditor

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent

/**
 * Text editor view state
 * @param downloadEvent event to trigger download actions
 */
data class TextEditorViewState(
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)