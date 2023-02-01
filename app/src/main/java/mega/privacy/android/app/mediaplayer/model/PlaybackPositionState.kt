package mega.privacy.android.app.mediaplayer.model

/**
 * The entity regarding playback position state
 *
 * @property showPlaybackDialog true is show dialog, otherwise is false
 * @property mediaItemName the name of media item that contains playback position history
 * @property playbackPosition the playback position history
 * @property isDialogShownBeforeBuildSources true is before build media sources, otherwise is false
 */
data class PlaybackPositionState(
    val showPlaybackDialog: Boolean = false,
    val mediaItemName: String? = null,
    val playbackPosition: Long? = null,
    val isDialogShownBeforeBuildSources: Boolean = true
)
