package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import android.view.TextureView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStopUseCase
import mega.privacy.android.feature.mediaplayer.data.gateway.VideoMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.videoplayer.navigation.VideoPlayerLaunchSourceHolder
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSource
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber

/**
 * ViewModel of the revamped video player.
 *
 * Resolves what the player was asked to play. Playback, the queue, subtitles and the rest are
 * added one feature at a time against `.ref/video-player-feature-inventory.md`, with the frozen
 * legacy player (`VideoPlayerViewModelV2` in `:app`) as the specification.
 */
@HiltViewModel(assistedFactory = VideoPlayerViewModel.Factory::class)
internal class VideoPlayerViewModel @AssistedInject constructor(
    @Assisted private val launchId: String,
    private val launchSourceHolder: VideoPlayerLaunchSourceHolder,
    private val gateway: VideoMediaControllerGateway,
    private val httpServerStopUseCase: HttpServerStopUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(launchId: String): VideoPlayerViewModel
    }

    private var hasStartedPlayback = false

    /**
     * What the player was asked to play, read from the holder exactly once.
     *
     * Held whole rather than reduced to the handful of fields the UI shows today. The holder
     * hands a source out a single time and drops it, so whatever is not kept here is gone: the
     * rest of these fields — the content URI, the parent and node handles, the sort order, the
     * link and local-file paths — are what the queue is built from, and there is no second read
     * to recover them from.
     *
     * Lazy so the read happens on first use rather than at construction, which keeps it out of
     * the way of a ViewModel created before the route is ready to show anything.
     */
    private val launchSource: VideoPlayerLaunchSource? by lazy(LazyThreadSafetyMode.NONE) {
        launchSourceHolder.consume(launchId)
    }

    /**
     * Whether the launch describes something playable, decided once.
     *
     * Separate from [uiState] because playback has to consult the same decision before the state
     * flow has emitted anything: until then [uiState] still reads as [VideoPlayerUiState.Loading],
     * and gating on that would mean the player only ever starts because the screen happens to
     * render the surface after the first emission.
     */
    private val resolvedState: VideoPlayerUiState by lazy(LazyThreadSafetyMode.NONE) {
        launchSource.toUiState()
    }

    /**
     * The launch is resolved once; everything after it tracks the player.
     *
     * Only a usable launch follows the player state — a rejected one is terminal, so collecting
     * the player would keep a flow alive for a screen that is already closing.
     */
    val uiState: StateFlow<VideoPlayerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        val resolved = resolvedState
        val state = if (resolved is VideoPlayerUiState.Data) {
            gateway.playerState.map { player ->
                resolved.copy(
                    videoWidth = player.videoWidth,
                    videoHeight = player.videoHeight,
                    isLoading = player.isLoading,
                    playWhenReady = player.playWhenReady,
                )
            }
        } else {
            flowOf(resolved)
        }
        state.asUiStateFlow(viewModelScope, VideoPlayerUiState.Loading)
    }

    /**
     * Decides whether the launch describes something playable, and says why when it does not.
     *
     * The conditions are the ones the previous implementation applied before it built playback
     * sources. The response differs: it logged and left the player sitting on an empty screen,
     * whereas an unusable launch now closes the player, which is what the user would expect and
     * what a missing source already did.
     *
     * `rebuildPlaylist` is deliberately not among them. It says whether the caller wants the
     * queue rebuilt, not whether the launch is usable, and belongs with the queue building it
     * governs.
     */
    private fun VideoPlayerLaunchSource?.toUiState(): VideoPlayerUiState {
        val source = this ?: return reject("no launch source reached the route")
        return when {
            source.sourceType == VideoPlayerLaunchSourceType.Invalid ->
                reject("the launch source is invalid")

            source.contentUri == null -> reject("the content URI is missing")

            source.handle == INVALID_HANDLE -> reject("the node handle is invalid")

            source.fileName == null -> reject("the file name is missing")

            else -> VideoPlayerUiState.Data(
                currentPlayingHandle = source.handle,
                currentPlayingItemName = source.fileName,
                launchSourceType = source.sourceType,
                videoWidth = 0,
                videoHeight = 0,
                isLoading = true,
                playWhenReady = false,
            )
        }
    }

    /**
     * Warning rather than debug: the player closes on the spot, so this line is the only record
     * of why, and something upstream built a launch it should not have.
     */
    private fun reject(reason: String): VideoPlayerUiState {
        Timber.w("Video player launch rejected: $reason")
        return VideoPlayerUiState.InvalidLaunchSource
    }

    /**
     * Renders into [textureView], and starts playback the first time one exists.
     *
     * Playback starts from here rather than from construction because there is nothing to watch
     * until the picture has somewhere to go, and because the launch source is read on first use.
     * A surface arriving again — a configuration change recreates it — re-attaches to the player
     * that is already running instead of loading the video a second time.
     */
    fun onSurfaceReady(textureView: TextureView) {
        gateway.attachSurface(textureView)
        if (hasStartedPlayback) return

        // Gated on the resolved decision rather than on the launch source, so that whether a
        // launch is playable is decided in exactly one place. Re-deriving it here would start
        // playback for a launch already rejected — an invalid handle, say, still carries a
        // content URI.
        if (resolvedState !is VideoPlayerUiState.Data) return
        val source = launchSource ?: return
        val uri = source.contentUri ?: return

        // Latched only once the player has accepted the item, so a failure leaves the next
        // surface free to try again rather than stranding the screen on black for good.
        runCatching {
            gateway.setPlaySource(uri, source.handle.toString())
            gateway.play()
        }
            .onSuccess { hasStartedPlayback = true }
            .onFailure { Timber.e(it, "Failed to start video playback") }
    }

    /** Stops rendering into the surface that is going away; playback itself continues. */
    fun onSurfaceReleased() {
        gateway.clearSurface()
    }

    /**
     * Toggles playback.
     *
     * The direction comes from the state the button was drawn from, so a tap does what the icon
     * said it would. Nothing to toggle before the launch resolves: the button is not on screen
     * then, and the player holds no item.
     */
    fun onPlayPauseClick() {
        val state = uiState.value as? VideoPlayerUiState.Data ?: return
        if (state.playWhenReady) gateway.pause() else gateway.play()
    }

    /**
     * The gateway is bound without a scope, so this ViewModel owns one player of its own and is
     * the only thing that can release it.
     *
     * The streaming server goes down here too when the launch handed its ownership over. Whoever
     * resolved the content URI had to start the server to obtain it, and says so with
     * `needStopHttpServer`; nothing else is left to stop it once the player closes.
     */
    override fun onCleared() {
        // Guarded separately so that a failure to release the player cannot also leave the
        // streaming server running for the rest of the process.
        runCatching { gateway.release() }
            .onFailure { Timber.e(it, "Failed to release the video player") }
        stopStreamingServerIfOwned()
        super.onCleared()
    }

    /**
     * Runs on the application scope rather than [viewModelScope], which is already cancelled by
     * the time [onCleared] is called, so the suspending stop would never run.
     */
    private fun stopStreamingServerIfOwned() {
        if (launchSource?.needStopHttpServer != true) return
        applicationScope.launch {
            runCatching { httpServerStopUseCase() }
                .onFailure { Timber.e(it, "Failed to stop the streaming server") }
        }
    }
}
