package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.core.coroutine.asUiStateFlow
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
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(launchId: String): VideoPlayerViewModel
    }

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

    val uiState: StateFlow<VideoPlayerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flowOf(launchSource.toUiState())
            .asUiStateFlow(viewModelScope, VideoPlayerUiState.Loading)
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
}
