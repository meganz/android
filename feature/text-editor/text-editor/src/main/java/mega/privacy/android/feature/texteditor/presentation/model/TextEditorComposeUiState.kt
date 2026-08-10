package mega.privacy.android.feature.texteditor.presentation.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.extension.isMarkdownFile

/**
 * UI state for the Compose text editor screen.
 *
 * @param totalLineCount Total number of logical lines in the full document.
 * @param contentVersion Monotonically increasing counter; the UI re-reads chunk data when it changes.
 * @param isFullyLoaded True when gradual loading has finished and all content is in memory.
 * @param errorMessage Optional error message when an operation fails; shown in error UI when set, cleared when error is consumed.
 * @param isNoInternetError True when the current error event was caused by no internet connectivity; UI shows the no-internet message instead of [errorMessage].
 * @param showDiscardDialog True when the discard-changes confirmation dialog should be shown (Edit/Create, unsaved changes).
 * @param exitAfterCreateDiscardEvent One-shot event when Create mode user confirms discard; UI should pop without save.
 * @param isRestoringContent True while content is being reverted/updated in background (e.g. discard); show loading overlay.
 * @param nodeEffectEvent One-shot effect for manage link, share, or send to chat; consumed by the app host.
 * @param shareErrorEvent One-shot event fired when the share public-link could not be resolved; the UI shows a snackbar.
 * @param sendToChatErrorEvent One-shot event fired when attaching nodes to chat fails; the UI shows a snackbar.
 */
data class TextEditorComposeUiState(
    val fileName: String = "",
    val isLoading: Boolean = false,
    val errorEvent: StateEvent = consumed,
    val errorMessage: String? = null,
    val isNoInternetError: Boolean = false,
    val mode: TextEditorMode = TextEditorMode.View,
    val showLineNumbers: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val exitAfterCreateDiscardEvent: StateEvent = consumed,
    val isRestoringContent: Boolean = false,
    val bottomBarActions: List<TextEditorBottomBarAction> = emptyList(),
    val nodeEffectEvent: StateEventWithContent<TextEditorNodeEffect> = consumed(),
    val shareErrorEvent: StateEvent = consumed,
    val sendToChatErrorEvent: StateEvent = consumed,
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val isFullyLoaded: Boolean = true,
    val totalLineCount: Int = 0,
    val contentVersion: Int = 0,
    val focusedEditChunk: Int = 0,
    val closeEvent: StateEvent = consumed,
    val restoreScrollIndex: Int? = null,
    val restoreScrollOffset: Int = 0,
    /**
     * 0-based line offset within [restoreScrollIndex]'s chunk to scroll to precisely (used when
     * entering Edit from the Markdown preview). Converted to pixels by the UI. Null when unused.
     */
    val restoreScrollWithinChunkLine: Int? = null,
    val restoreFocusChunkIndex: Int? = null,
    /** True when the Markdown-rendering feature flag is enabled for this session. */
    val isMarkdownEnabled: Boolean = false,
    /**
     * One-shot top logical line (0-based) to restore the Markdown preview to, e.g. when returning
     * from Edit or resuming via Continue-Where-Left-Off. Null when nothing to restore.
     */
    val restorePreviewLine: Int? = null,
) {
    /**
     * True when the current file should be treated as Markdown: the flag is on AND the
     * file name has a Markdown extension. Derived so it tracks rename/chat/link updates.
     * Markdown files open in a rendered preview in View mode; Edit shows the raw source.
     */
    val isMarkdown: Boolean get() = isMarkdownEnabled && fileName.isMarkdownFile()
}
