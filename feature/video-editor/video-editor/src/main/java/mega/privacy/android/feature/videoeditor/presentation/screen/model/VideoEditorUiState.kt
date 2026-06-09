package mega.privacy.android.feature.videoeditor.presentation.screen.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * UI state for the video editor screen.
 *
 * The video is downloaded to the app cache before it can be edited; the screen shows
 * download progress until [videoFilePath] is available.
 *
 * @param isLoading Whether the video is still being downloaded to the cache
 * @param downloadProgress Download progress in the range [0, 100]
 * @param videoFilePath Absolute path to the downloaded video in the app cache, null until ready
 * @param isError Whether the node could not be resolved or the download failed
 * @param transferEvent Emitted once the exported copy is ready to upload; the host wires it to the
 * app's transfer subsystem, which owns the upload (enqueue, progress, quota, notifications)
 */
data class VideoEditorUiState(
    val isLoading: Boolean = true,
    val downloadProgress: Int = 0,
    val videoFilePath: String? = null,
    val isError: Boolean = false,
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)
