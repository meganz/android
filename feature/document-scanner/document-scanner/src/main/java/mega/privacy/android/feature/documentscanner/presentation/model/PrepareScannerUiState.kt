package mega.privacy.android.feature.documentscanner.presentation.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState

/**
 * UI state for the prepare/loading screen that drives the scanner-model download.
 *
 * @property downloadState the latest observed state of the background download.
 * @property modelReadyEvent one-shot event fired when the download completes — the
 *   screen should navigate to the camera on receipt and call [onModelReadyConsumed].
 */
@Stable
data class PrepareScannerUiState(
    val downloadState: ScannerModelDownloadState = ScannerModelDownloadState.Pending,
    val modelReadyEvent: StateEvent = consumed,
)
