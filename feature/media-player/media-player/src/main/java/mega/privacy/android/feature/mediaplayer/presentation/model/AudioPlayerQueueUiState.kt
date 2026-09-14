package mega.privacy.android.feature.mediaplayer.presentation.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
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
     * @property isContinuousPlayback Whether playback moves on to the next queue item automatically.
     * @property playingFromName Name of the folder the queue is playing from; null when the
     * source is a root section (see [playingFromSection]) or unknown.
     * @property playingFromSection Root section the queue is playing from, so the UI can show its
     * localized name instead of the SDK's root-node name; null for an ordinary folder.
     * @property controlBlockedByCall Triggered when starting a queue item was refused because a
     * MEGA call is ongoing.
     */
    data class Data(
        val items: List<AudioQueueItem>,
        val currentQueueIndex: Int,
        val currentTitle: String?,
        val currentArtist: String?,
        val currentArtworkUri: String?,
        val currentThumbnailData: ThumbnailData?,
        val isContinuousPlayback: Boolean,
        val isShuffleEnabled: Boolean = false,
        val playingFromName: String?,
        val playingFromSection: PlayingFromSection? = null,
        val controlBlockedByCall: StateEvent = consumed,
    ) : AudioPlayerQueueUiState
}

/**
 * Root sections whose display name must come from string resources, not the SDK node name.
 * Backups is not listed: its root only holds device folders, so no track can sit directly in it.
 */
enum class PlayingFromSection {
    CloudDrive,
    RubbishBin,
}
