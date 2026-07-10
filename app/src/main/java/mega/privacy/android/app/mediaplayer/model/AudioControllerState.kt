package mega.privacy.android.app.mediaplayer.model

import androidx.media3.common.Player
import timber.log.Timber

/**
 * Raw player state emitted by [mega.privacy.android.app.mediaplayer.gateway.AudioMediaControllerGateway].
 *
 * Represents the current snapshot of the Media3 MediaController state. The ViewModel maps this
 * into [AudioPlayerUiState] and applies domain-level side effects (analytics, persistence,
 * node name fetching) on top.
 */
data class AudioControllerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val mediaItemCount: Int = 0,
    val isBuffering: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    /** Raw [androidx.media3.session.MediaController.getCurrentMediaItem] ID, always a Long-as-String in this app. */
    val currentMediaItemId: String? = null,
) {
    /** Convenience accessor that converts [currentMediaItemId] to [Long], or `null` if absent. */
    val currentMediaItemHandle: Long?
        get() = currentMediaItemId?.toLongOrNull().also { result ->
            if (result == null && currentMediaItemId != null) {
                Timber.w("Non-numeric media item ID: $currentMediaItemId")
            }
        }
}
