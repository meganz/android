package mega.privacy.android.app.presentation.transfers.preview.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.Progress

/**
 * Fake preview state.
 *
 * @param fileName The name of the file.
 * @param fileTypeResId The resource id of the file type icon.
 * @param progress The progress of the transfer.
 * @param previewFilePathToOpen The file path to preview.
 * @param error [Throwable].
 * @param transferEvent [StateEventWithContent] of [TransferTriggerEvent].
 */
data class LoadingPreviewState(
    val fileName: String? = null,
    val fileTypeResId: Int? = null,
    val progress: Progress = Progress(0f),
    val previewFilePathToOpen: String? = null,
    val error: Throwable? = null,
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)
