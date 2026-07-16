package mega.privacy.android.app.mediaplayer.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData

/**
 * UI state for the revamped audio player.
 */
@Stable
sealed interface AudioPlayerUiState {

    /** Player not yet initialised or connecting to the media service. */
    data object Loading : AudioPlayerUiState

    /**
     * Player is ready and metadata is available.
     *
     * @property isPlaying Whether the player is currently playing.
     * @property currentPosition Current playback position in milliseconds.
     * @property duration Total duration of the current item in milliseconds.
     * @property title Track title from media metadata.
     * @property artist Artist name from media metadata.
     * @property artworkUri Artwork URI string from media metadata.
     * @property repeatMode Current repeat mode.
     * @property shuffleEnabled Whether shuffle mode is enabled.
     * @property isLoading Whether the player is buffering mid-playback.
     * @property currentPlayingHandle Handle of the currently playing node, or `null` if no item is loaded.
     * @property currentPlayingItemName Name of the currently playing item.
     * @property hasPlaylist Whether there is more than one item in the queue.
     * @property currentAdapterType The adapter type from the launch intent.
     * @property thumbnailData Thumbnail data for the currently playing node.
     * @property nodeSourceType The node source type derived from the adapter type.
     * @property fileLinkUrl Public file-link URL; non-null only for file-link launches.
     * @property localFilePath Local file path; non-null only for zip-file launches.
     * @property chatId Chat room ID; non-null only for chat launches.
     * @property msgId Message ID; non-null only for chat launches.
     */
    data class Data(
        val isPlaying: Boolean,
        val currentPosition: Long,
        val duration: Long,
        val title: String?,
        val artist: String?,
        val artworkUri: String?,
        val repeatMode: Int,
        val shuffleEnabled: Boolean,
        val isLoading: Boolean,
        val currentPlayingHandle: Long?,
        val currentPlayingItemName: String?,
        val hasPlaylist: Boolean,
        val currentAdapterType: Int,
        val thumbnailData: ThumbnailData?,
        val nodeSourceType: NodeSourceType = NodeSourceType.VIDEO_PLAYER_DEFAULT,
        val fileLinkUrl: String? = null,
        val localFilePath: String? = null,
        val chatId: Long? = null,
        val msgId: Long? = null,
    ) : AudioPlayerUiState
}
