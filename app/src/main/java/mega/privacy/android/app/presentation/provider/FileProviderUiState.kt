package mega.privacy.android.app.presentation.provider

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

/**
 * File Provider UI State
 *
 * @property startDownloadEvent event to start the needed downloads
 * @property cloudRootHandle the cloud drive root node handle, or [INVALID_HANDLE] if not yet
 * initialised.
 */
data class FileProviderUiState(
    val startDownloadEvent: StateEventWithContent<TransferTriggerEvent.StartDownloadForAttach> = consumed(),
    val cloudRootHandle: Long = INVALID_HANDLE,
)