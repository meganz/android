package mega.privacy.android.app.presentation.provider

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent

/**
 * File Provider UI State
 * @property startDownloadEvent event to start the needed downloads
 */
data class FileProviderUiState(
    val startDownloadEvent: StateEventWithContent<TransferTriggerEvent.StartDownloadNode> = consumed(),
)