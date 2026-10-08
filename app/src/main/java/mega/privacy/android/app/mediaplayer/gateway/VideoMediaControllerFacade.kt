package mega.privacy.android.app.mediaplayer.gateway

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.view.TextureView
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
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
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.feature.mediaplayer.data.gateway.VideoMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.data.model.VideoControllerState
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * [VideoMediaControllerGateway] backed by a Media3 [MediaController] bound to [VideoPlayerService],
 * which owns the player.
 *
 * Turns the player's callbacks into a single [VideoControllerState] stream.
 *
 * **The connection is asynchronous**, so a command can arrive before there is a controller to send
 * it to. Three of them cannot simply be dropped: what to play, whether to play it, and where to
 * draw it. Losing any one leaves a player that never starts or never shows a picture, with nothing
 * to retry it, so they are held and applied once the controller arrives.
 *
 * Not a singleton: each owner gets its own controller and must call [release]. The controllers do
 * share one player, though — it belongs to the service, not to this class. Two screens can
 * therefore overlap while the outgoing one is torn down, which is why the surface is detached by
 * identity rather than with the blanket `clearVideoSurface()`: that one clears whatever the player
 * currently holds, which by then may be the incoming screen's.
 */
@OptIn(UnstableApi::class)
internal class VideoMediaControllerFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : VideoMediaControllerGateway {

    private val gatewayScope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private val _playerState = MutableStateFlow(VideoControllerState())
    override val playerState: Flow<VideoControllerState> = _playerState.asStateFlow()

    @VisibleForTesting
    internal var controller: MediaController? = null

    @VisibleForTesting
    internal var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionPollingJob: Job? = null

    /**
     * Once released there is no connection left to apply anything to, so the commands that would
     * otherwise cache their argument return instead of leaving it to be collected with the facade.
     */
    private var isReleased = false

    private var pendingMediaItem: MediaItem? = null
    private var pendingPlay = false
    private var pendingSurface: TextureView? = null

    /** The view this instance handed to the player, so only that one is ever detached. */
    private var attachedSurface: TextureView? = null

    @VisibleForTesting
    internal val playerListener = object : Player.Listener {

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateState {
                copy(
                    title = mediaMetadata.title?.toString(),
                    artist = mediaMetadata.artist?.toString(),
                    album = mediaMetadata.albumTitle?.toString(),
                )
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updateState {
                copy(
                    playWhenReady = playWhenReady,
                    pausedByUserRequest = !playWhenReady &&
                            reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startPositionPolling()
            } else {
                // Sampled once more on the way out: the poll can be up to its own interval behind,
                // and this is the value that stands until playback resumes.
                samplePosition()
                stopPositionPolling()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateState {
                copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    isEnded = playbackState == Player.STATE_ENDED,
                    isReady = playbackState == Player.STATE_READY,
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateState {
                copy(
                    currentMediaHandle = mediaItem?.mediaId?.toLongOrNull(),
                    transitionUpdatesName = reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT,
                    videoWidth = 0,
                    videoHeight = 0,
                )
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            // A zero size is reported between items and would collapse the video frame to nothing.
            if (videoSize.width == 0 || videoSize.height == 0) return
            updateState { copy(videoWidth = videoSize.width, videoHeight = videoSize.height) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error, "Video player error")
        }
    }

    init {
        connect()
    }

    private fun connect() {
        val sessionToken = runCatching {
            SessionToken(context, ComponentName(context, VideoPlayerService::class.java))
        }.getOrElse {
            Timber.e(it, "Failed to create SessionToken for VideoPlayerService")
            onConnectionLost()
            return
        }
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(result: MediaController) =
                    onControllerConnected(future, result)

                override fun onFailure(t: Throwable) = onControllerConnectionFailed(future, t)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    /**
     * Installs [result] as the live controller, unless [future] is no longer the connection being
     * waited on, which happens when [release] cleared it. A stale result is released on the spot so
     * it cannot leak or hold the service open for a player nobody is watching.
     */
    @MainThread
    @VisibleForTesting
    internal fun onControllerConnected(
        future: ListenableFuture<MediaController>,
        result: MediaController,
    ) {
        if (future !== controllerFuture) {
            runCatching { result.release() }
                .onFailure { Timber.e(it, "Failed to release a superseded MediaController") }
            return
        }
        controller = result
        result.addListener(playerListener)
        applyPending(result)
    }

    /**
     * Records a failed connection for the live [future] only, so a failure belonging to a
     * connection that has already been superseded or released cannot blank out a working one.
     */
    @MainThread
    @VisibleForTesting
    internal fun onControllerConnectionFailed(
        future: ListenableFuture<MediaController>,
        t: Throwable,
    ) {
        if (future !== controllerFuture) return
        Timber.e(t, "Failed to connect MediaController to VideoPlayerService")
        onConnectionLost()
    }

    /**
     * Nothing retries the connection, so the held commands are dropped rather than kept for a
     * controller that is not coming, and the owner is told the screen will stay black.
     */
    private fun onConnectionLost() {
        if (isReleased) return
        pendingMediaItem = null
        pendingPlay = false
        pendingSurface = null
        updateState { copy(isPlayerUnavailable = true) }
    }

    private fun applyPending(ctrl: MediaController) {
        pendingSurface?.let { ctrl.setVideoTextureView(it) }
        pendingSurface = null
        pendingMediaItem?.let {
            ctrl.setMediaItem(it)
            ctrl.prepare()
        }
        pendingMediaItem = null
        if (pendingPlay) ctrl.play()
        pendingPlay = false
    }

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
                samplePosition()
                delay(POSITION_POLLING_INTERVAL)
            }
        }
    }

    private fun samplePosition() {
        val ctrl = controller ?: return
        // Read before the update, so the block stays pure: it re-runs under contention, and a
        // re-run would otherwise re-query the player and pair a position with a duration read at
        // a different instant.
        val positionMs = ctrl.currentPosition.coerceAtLeast(0L)
        val durationMs = ctrl.duration.coerceAtLeast(0L)
        updateState { copy(currentPositionMs = positionMs, durationMs = durationMs) }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    @MainThread
    override fun setPlaySource(uri: Uri, mediaId: String) {
        if (isReleased) return
        val mediaItem = MediaItem.Builder().setUri(uri).setMediaId(mediaId).build()
        val ctrl = controller
        if (ctrl == null) {
            pendingMediaItem = mediaItem
            return
        }
        ctrl.setMediaItem(mediaItem)
        ctrl.prepare()
    }

    @MainThread
    override fun play() {
        if (isReleased) return
        val ctrl = controller
        if (ctrl == null) {
            pendingPlay = true
            return
        }
        ctrl.play()
    }

    @MainThread
    override fun pause() {
        if (isReleased) return
        val ctrl = controller
        if (ctrl == null) {
            // Nothing to pause yet, but a play held for the connection would start the moment it
            // arrives, against the user's last instruction.
            pendingPlay = false
            return
        }
        ctrl.pause()
    }

    @MainThread
    override fun attachSurface(textureView: TextureView) {
        if (isReleased) return
        val ctrl = controller
        if (ctrl == null) {
            pendingSurface = textureView
            return
        }
        attachedSurface = textureView
        ctrl.setVideoTextureView(textureView)
    }

    @MainThread
    override fun clearSurface() {
        if (isReleased) return
        pendingSurface = null
        detachSurface()
    }

    /**
     * Detaches by identity: the player is shared, so the blanket `clearVideoSurface()` would take
     * down whichever surface it currently holds, including a successor screen's.
     */
    private fun detachSurface() {
        val view = attachedSurface ?: return
        attachedSurface = null
        controller?.clearVideoTextureView(view)
    }

    @MainThread
    override fun release() {
        if (isReleased) return
        isReleased = true
        stopPositionPolling()
        pendingMediaItem = null
        pendingPlay = false
        pendingSurface = null
        // Each step is guarded on its own: a throw in any one of them would otherwise skip the
        // rest of the teardown, including the scope cancellation at the end.
        runCatching { controller?.removeListener(playerListener) }
            .onFailure { Timber.e(it, "Failed to remove the player listener") }
        // Detached before the connection is released, so the service cannot be left holding the
        // owner's TextureView.
        runCatching { detachSurface() }
            .onFailure { Timber.e(it, "Failed to detach the video surface") }
        // Releases the connection rather than the player: the service owns the player and ends
        // itself once no controller is left attached.
        runCatching { controllerFuture?.let { MediaController.releaseFuture(it) } }
            .onFailure { Timber.e(it, "Failed to release the video player's MediaController") }
        controller = null
        controllerFuture = null
        gatewayScope.cancel()
    }

    companion object {
        private val POSITION_POLLING_INTERVAL = 500.milliseconds
    }
}
