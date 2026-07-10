package mega.privacy.android.app.mediaplayer.gateway

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.privacy.android.app.mediaplayer.model.AudioControllerState
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.domain.qualifier.MainDispatcher
import timber.log.Timber

/**
 * [AudioMediaControllerGateway] implementation backed by a Media3 [MediaController].
 *
 * Connects to [AudioPlayerService] via a [MediaController], translates all [Player.Listener]
 * callbacks into [AudioControllerState] emissions, and polls position/duration every
 * [POSITION_POLLING_INTERVAL_MS] milliseconds.
 */
internal class AudioMediaControllerFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : AudioMediaControllerGateway {

    private val gatewayScope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private val _playerState = MutableSharedFlow<AudioControllerState>(replay = 1)
    override val playerState: Flow<AudioControllerState> = _playerState

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionPollingJob: Job? = null
    private var currentState = AudioControllerState()

    init {
        connect()
    }

    private fun connect() {
        val sessionToken = runCatching {
            SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        }.getOrElse {
            Timber.e(it, "Failed to create SessionToken for AudioPlayerService")
            return
        }
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().also { future ->
            Futures.addCallback(
                future,
                object : FutureCallback<MediaController> {
                    override fun onSuccess(result: MediaController) {
                        // Guard against delivery after release() was already called.
                        if (controllerFuture == null) {
                            result.release()
                            return
                        }
                        controller = result
                        result.addListener(playerListener)
                        syncAndEmit(result)
                        startPositionPolling()
                    }

                    override fun onFailure(t: Throwable) {
                        Timber.e(t, "Failed to connect MediaController to AudioPlayerService")
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    private fun syncAndEmit(c: MediaController) {
        currentState = AudioControllerState(
            isPlaying = c.isPlaying,
            currentPositionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.coerceAtLeast(0L),
            repeatMode = c.repeatMode,
            shuffleEnabled = c.shuffleModeEnabled,
            mediaItemCount = c.mediaItemCount,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            title = c.mediaMetadata.title?.toString(),
            artist = c.mediaMetadata.artist?.toString(),
            artworkUri = c.mediaMetadata.artworkUri?.toString(),
            currentMediaItemId = c.currentMediaItem?.mediaId,
        )
        _playerState.tryEmit(currentState)
    }

    private fun updateState(update: AudioControllerState.() -> AudioControllerState) {
        currentState = currentState.update()
        _playerState.tryEmit(currentState)
    }

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState { copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionPolling() else stopPositionPolling()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateState {
                copy(
                    title = mediaMetadata.title?.toString(),
                    artist = mediaMetadata.artist?.toString(),
                    artworkUri = mediaMetadata.artworkUri?.toString(),
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateState { copy(currentMediaItemId = mediaItem?.mediaId) }
        }

        override fun onPlaybackStateChanged(state: Int) {
            updateState { copy(isBuffering = state == Player.STATE_BUFFERING) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            updateState { copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updateState { copy(repeatMode = repeatMode) }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val ctrl = controller ?: return
            updateState { copy(mediaItemCount = ctrl.mediaItemCount) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error, "AudioPlayer player error")
        }
    }

    private fun startPositionPolling() {
        if (positionPollingJob?.isActive == true) return
        positionPollingJob = gatewayScope.launch {
            while (isActive) {
                val ctrl = controller
                if (ctrl != null) {
                    updateState {
                        copy(
                            currentPositionMs = ctrl.currentPosition.coerceAtLeast(0L),
                            durationMs = ctrl.duration.coerceAtLeast(0L),
                        )
                    }
                }
                delay(POSITION_POLLING_INTERVAL_MS)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    override fun startService(intent: Intent) {
        val extras = intent.extras ?: return
        val serviceIntent = Intent(context, AudioPlayerService::class.java).putExtras(extras)
        serviceIntent.setDataAndType(intent.data, intent.type)
        runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
            .onFailure { Timber.e(it, "Failed to start AudioPlayerService") }
    }

    override fun play() {
        controller?.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    override fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        controller?.setShuffleModeEnabled(enabled)
    }

    override fun setRepeatMode(mode: Int) {
        controller?.setRepeatMode(mode)
    }

    override fun stop() {
        controller?.stop()
    }

    override fun release() {
        stopPositionPolling()
        controller?.removeListener(playerListener)
        gatewayScope.cancel()
        // Null the fields before releasing so the pending onSuccess callback (if any) can detect
        // post-release delivery via the controllerFuture == null guard and immediately drop the
        // stale controller without touching our already-cleared state.
        val futureToRelease = controllerFuture
        controllerFuture = null
        controller = null
        runCatching { futureToRelease?.let { MediaController.releaseFuture(it) } }
            .onFailure { Timber.e(it, "Failed to release MediaController") }
    }

    companion object {
        private const val POSITION_POLLING_INTERVAL_MS = 500L
    }
}
