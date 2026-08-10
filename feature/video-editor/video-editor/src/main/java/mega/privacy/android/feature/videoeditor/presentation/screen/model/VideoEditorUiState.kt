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
 * @param isDownloading Whether a network download is actually in progress; stays false when a local
 * or cached copy is reused, so the "Preparing video" dialog is not flashed for an instant ready
 * @param fileName Name of the source video, shown while it is being downloaded
 * @param exportFileName Name the exported copy is uploaded under (before collision renaming):
 * the source name with an .mp4 extension, since the encoder always outputs MP4. Shown while encoding
 * @param fileSizeBytes Size of the source video in bytes, used for the download size readout
 * @param previewImagePath Absolute path to the source's preview (or thumbnail fallback), shown in
 * the preview area while preparing
 * @param videoFilePath Absolute path to the downloaded video in the app cache, null until ready
 * @param isError Whether the node could not be resolved or the download failed
 * @param transferEvent Emitted once the exported copy is ready to upload; the host wires it to the
 * app's transfer subsystem, which owns the upload (enqueue, progress, quota, notifications)
 */
data class VideoEditorUiState(
    val isLoading: Boolean = true,
    val downloadProgress: Int = 0,
    val isDownloading: Boolean = false,
    val fileName: String = "",
    val exportFileName: String = "",
    val fileSizeBytes: Long = 0L,
    val previewImagePath: String? = null,
    val videoFilePath: String? = null,
    val isError: Boolean = false,
    val transferEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)
