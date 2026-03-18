package mega.privacy.android.feature.texteditor.presentation.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * UI state for the Compose text editor screen.
 *
 * @param totalLineCount Total number of logical lines in the full document.
 * @param contentVersion Monotonically increasing counter; the UI re-reads chunk data when it changes.
 * @param isFullyLoaded True when gradual loading has finished and all content is in memory.
 * @param errorMessage Optional error message when an operation fails; shown in error UI when set, cleared when error is consumed.
 * @param showDiscardDialog True when the discard-changes confirmation dialog should be shown (Edit mode, unsaved changes).
 * @param saveSuccessEvent One-shot event to show "Changes saved" snackbar when save completes successfully.
 * @param isRestoringContent True while content is being reverted/updated in background (e.g. discard); show loading overlay.
 */
data class TextEditorComposeUiState(
    val fileName: String = "",
    val isLoading: Boolean = false,
    val errorEvent: StateEvent = consumed,
    val errorMessage: String? = null,
    val mode: TextEditorMode = TextEditorMode.View,
    val showLineNumbers: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val saveSuccessEvent: StateEvent = consumed,
    val isRestoringContent: Boolean = false,
    val bottomBarActions: List<TextEditorBottomBarAction> = emptyList(),
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val isFullyLoaded: Boolean = true,
    val totalLineCount: Int = 0,
    val contentVersion: Int = 0,
    val focusedEditChunk: Int = 0,
)
