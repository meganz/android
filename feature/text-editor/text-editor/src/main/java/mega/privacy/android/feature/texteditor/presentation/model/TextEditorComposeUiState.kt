package mega.privacy.android.feature.texteditor.presentation.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * UI state for the Compose text editor screen.
 *
 * @param hasMoreLines True when content was capped for display and more lines are available; user can load more by scrolling to bottom.
 * @param totalLinesLoaded Total lines loaded so far (for gradual load); null when not applicable.
 * @param isFullyLoaded True when gradual loading has finished and all content is in memory.
 * @param loadErrorMessage Optional error message when load fails; shown in error UI when set, cleared when error is consumed.
 */
data class TextEditorComposeUiState(
    val fileName: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val errorEvent: StateEvent = consumed,
    val loadErrorMessage: String? = null,
    val mode: TextEditorMode = TextEditorMode.View,
    val isFileEdited: Boolean = false,
    val showLineNumbers: Boolean = false,
    val bottomBarActions: List<TextEditorBottomBarAction> = emptyList(),
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val hasMoreLines: Boolean = false,
    val totalLinesLoaded: Int? = null,
    val isFullyLoaded: Boolean = true,
)
