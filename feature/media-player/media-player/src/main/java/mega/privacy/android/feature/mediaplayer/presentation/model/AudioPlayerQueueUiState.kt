package mega.privacy.android.feature.mediaplayer.presentation.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.feature.mediaplayer.data.model.AudioQueueItem

/**
 * UI state for the audio player play-queue screen.
 */
@Stable
sealed interface AudioPlayerQueueUiState {

    /** Queue screen is initializing while the media controller connects. */
    data object Loading : AudioPlayerQueueUiState

    /**
     * Queue is ready and player state is available.
     *
     * @property items All items in the queue in their current play order.
     * @property currentQueueIndex Index of the currently playing item within [items].
     * @property currentTitle Title of the currently playing item.
     * @property currentArtist Artist of the currently playing item.
     * @property currentArtworkUri Artwork URI of the currently playing item.
     * @property currentThumbnailData Thumbnail data for the currently playing node.
     * @property isContinuousPlayback Whether the player loops the queue continuously.
     * @property playingFromName Name of the folder or source the queue is playing from.
     */
    data class Data(
        val items: List<AudioQueueItem>,
        val currentQueueIndex: Int,
        val currentTitle: String?,
        val currentArtist: String?,
        val currentArtworkUri: String?,
        val currentThumbnailData: ThumbnailData?,
        val isContinuousPlayback: Boolean,
        val playingFromName: String?,
    ) : AudioPlayerQueueUiState
}
