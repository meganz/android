package mega.privacy.android.app.mediaplayer.gateway

import android.net.Uri
import android.view.TextureView
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.videoplayer.MediaPlayerManager
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.feature.mediaplayer.data.gateway.VideoMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.data.model.VideoControllerState
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * [VideoMediaControllerGateway] backed by an ExoPlayer that this facade owns.
 *
 * Wraps [MediaPlayerManager], which is where the player construction and the raw Media3 callback
 * plumbing already live, and turns its callbacks into a single [VideoControllerState] stream.
 *
 * Several of those callbacks are deliberately empty: the gateway exposes nothing that would consume
 * them. [MediaPlayerManager] requires all nine, so they are supplied and ignored rather than its
 * signature narrowed for a class that goes away once the player moves into a service.
 *
 * This is the transitional implementation. A later phase replaces the ExoPlayer with a Media3
 * `MediaController` bound to a `VideoPlayerService`; because `MediaController` also implements
 * [Player], the gateway surface does not change at that point.
 *
 * Not a singleton: each owner gets its own facade, its own player, and must call [release].
 */
@OptIn(UnstableApi::class)
internal class VideoMediaControllerFacade @Inject constructor(
    private val mediaPlayerManager: MediaPlayerManager,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : VideoMediaControllerGateway {

    private val gatewayScope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private val _playerState = MutableStateFlow(VideoControllerState())
    override val playerState: Flow<VideoControllerState> = _playerState.asStateFlow()

    private var positionPollingJob: Job? = null
    private var isReleased = false

    /**
     * The player is built eagerly: the queue is assembled against it as soon as the owner starts,
     * so it has to exist before the first command arrives.
     */
    private val player: ExoPlayer = mediaPlayerManager.createPlayer(
        onMetadataChanged = { title, artist, album ->
            updateState { copy(title = title, artist = artist, album = album) }
        },
        onMediaItemTransition = { handle, isUpdateName ->
            updateState {
                copy(
                    currentMediaHandle = handle?.toLongOrNull(),
                    transitionUpdatesName = isUpdateName,
                    videoWidth = 0,
                    videoHeight = 0,
                )
            }
        },
        // Empty until a repeat control exists to read the mode back.
        onRepeatModeChanged = {},
        onPlayWhenReadyChanged = { state, pausedByUser ->
            val playing = state == MediaPlaybackState.Playing
            updateState { copy(playWhenReady = playing, pausedByUserRequest = pausedByUser) }
            if (playing) startPositionPolling() else stopPositionPolling()
        },
        onPlaybackStateChanged = { state ->
            updateState {
                copy(
                    isBuffering = state == Player.STATE_BUFFERING,
                    isEnded = state == Player.STATE_ENDED,
                    isReady = state == Player.STATE_READY,
                )
            }
        },
        // Nothing consumes playback events yet. Resume-position persistence and retry will take
        // whichever of these they need.
        onPlaybackPositionJumped = {},
        onMediaItemLeft = { _, _ -> },
        onPlayerError = { errorCode -> Timber.e("Video player error, code %d", errorCode) },
        onVideoSizeChanged = { size ->
            updateState { copy(videoWidth = size.width, videoHeight = size.height) }
        },
    )

    private fun updateState(transform: VideoControllerState.() -> VideoControllerState) {
        _playerState.update { it.transform() }
    }

    /**
     * Position and duration are not pushed by the player, so they are sampled while playing. The
     * poll stops on pause so a paused player costs nothing.
     */
    private fun startPositionPolling() {
        if (positionPollingJob?.isActive == true) return
        positionPollingJob = gatewayScope.launch {
            while (isActive) {
                // Sampled before the update, so the block stays pure: it re-runs under contention,
                // and a re-run would otherwise re-query the player and pair a position with a
                // duration read at a different instant.
                val positionMs = mediaPlayerManager.getCurrentPlayingPosition().coerceAtLeast(0L)
                val durationMs = mediaPlayerManager.getCurrentItemDuration().coerceAtLeast(0L)
                updateState { copy(currentPositionMs = positionMs, durationMs = durationMs) }
                delay(POSITION_POLLING_INTERVAL)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    override fun setPlaySource(uri: Uri, mediaId: String) {
        mediaPlayerManager.buildPlaySources(
            MediaPlaySources(
                mediaItems = listOf(
                    MediaItem.Builder().setUri(uri).setMediaId(mediaId).build()
                ),
                newIndexForCurrentItem = INVALID_VALUE,
                nameToDisplay = null,
            )
        )
        mediaPlayerManager.playerPrepare()
    }

    override fun play() = mediaPlayerManager.setPlayWhenReady(true)

    @MainThread
    override fun attachSurface(textureView: TextureView) {
        player.setVideoTextureView(textureView)
    }

    @MainThread
    override fun clearSurface() {
        player.clearVideoSurface()
    }

    @MainThread
    override fun release() {
        if (isReleased) return
        isReleased = true
        stopPositionPolling()
        // Detached first and separately, so a failure here cannot leave the player holding the
        // owner's TextureView by skipping the release below.
        runCatching { player.clearVideoSurface() }
            .onFailure { Timber.e(it, "Failed to detach the video surface") }
        runCatching { mediaPlayerManager.release() }
            .onFailure { Timber.e(it, "Failed to release the video player") }
        gatewayScope.cancel()
    }

    companion object {
        private val POSITION_POLLING_INTERVAL = 500.milliseconds
    }
}
