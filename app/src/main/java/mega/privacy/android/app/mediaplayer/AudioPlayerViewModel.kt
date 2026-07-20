package mega.privacy.android.app.mediaplayer

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.mediaplayer.gateway.AudioMediaControllerGateway
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.app.mediaplayer.model.AudioControllerState
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.URL_LOCAL_FILE_PATH
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioShuffleEnabledUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FILE_LINK_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import mega.privacy.mobile.analytics.event.AudioPlayerLoopPlayingItemEnabledEvent
import mega.privacy.mobile.analytics.event.AudioPlayerLoopQueueEnabledEvent
import mega.privacy.mobile.analytics.event.AudioPlayerShuffleEnabledEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber

/**
 * ViewModel for the revamped audio player.
 *
 * Collects raw player state from [AudioMediaControllerGateway] and maps it to [uiState].
 * Shuffle/repeat changes are persisted and analytics events are tracked here, keeping the
 * gateway focused on Media3 interaction only.
 *
 * [isPodcastMode] is resolved as early as possible: when [startPlayback] receives the launch
 * intent, the node's duration is fetched from [GetNodeByHandleUseCase] before Media3 connects,
 * so the correct mode is shown even during the initial [AudioPlayerUiState.Loading] phase.
 * Once Media3 reports the actual duration the value is confirmed (or corrected). The user may
 * override the auto-detected mode at any time via [togglePlayerMode]; the override resets when
 * the playing item changes.
 */
@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    private val gateway: AudioMediaControllerGateway,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val setAudioShuffleEnabledUseCase: SetAudioShuffleEnabledUseCase,
    private val setAudioRepeatModeUseCase: SetAudioRepeatModeUseCase,
    private val repeatToggleModeByExoPlayerMapper: RepeatToggleModeByExoPlayerMapper,
) : ViewModel() {

    private val playerState = MutableStateFlow<AudioPlayerUiState>(AudioPlayerUiState.Loading)

    val uiState: StateFlow<AudioPlayerUiState> =
        playerState.asUiStateFlow(viewModelScope, AudioPlayerUiState.Loading)

    private val _isPodcastMode = MutableStateFlow(true)

    /** Whether the player is currently in podcast mode. Available from the moment the launch
     * intent is processed, before Media3 finishes connecting. */
    val isPodcastMode: StateFlow<Boolean> = _isPodcastMode.asStateFlow()

    private var currentControllerState: AudioControllerState? = null
    private var lastMediaItemId: String? = null
    private var userOverriddenMode: Boolean? = null
    private var prefetchJob: Job? = null

    private data class IntentData(
        val adapterType: Int,
        val nodeSourceType: NodeSourceType,
        val fileLinkUrl: String?,
        val localFilePath: String?,
        val chatId: Long?,
        val msgId: Long?,
    )

    private var intentData: IntentData? = null

    init {
        observePlayerState()
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            var prevState: AudioControllerState? = null
            gateway.playerState
                .catch { Timber.e(it, "Failed to collect player state from gateway") }
                .collect { state ->
                    handleSideEffects(prevState, state)
                    prevState = state
                    currentControllerState = state
                    mapToUiState(state)
                }
        }
    }

    private fun handleSideEffects(prev: AudioControllerState?, current: AudioControllerState) {
        if (prev == null) return

        if (prev.shuffleEnabled != current.shuffleEnabled) {
            if (current.shuffleEnabled) {
                Analytics.tracker.trackEvent(AudioPlayerShuffleEnabledEvent)
            }
            viewModelScope.launch { setAudioShuffleEnabledUseCase(current.shuffleEnabled) }
        }

        if (prev.repeatMode != current.repeatMode) {
            val toggleMode = repeatToggleModeByExoPlayerMapper(current.repeatMode)
            when (toggleMode) {
                RepeatToggleMode.REPEAT_ONE ->
                    Analytics.tracker.trackEvent(AudioPlayerLoopPlayingItemEnabledEvent)

                RepeatToggleMode.REPEAT_ALL ->
                    Analytics.tracker.trackEvent(AudioPlayerLoopQueueEnabledEvent)

                else -> {}
            }
            viewModelScope.launch { setAudioRepeatModeUseCase(toggleMode.ordinal) }
        }

        if (prev.currentMediaItemId != current.currentMediaItemId) {
            current.currentMediaItemHandle?.let { fetchNodeName(it) }
        }
    }

    private fun mapToUiState(state: AudioControllerState) {
        val existing = playerState.value as? AudioPlayerUiState.Data
        val data = intentData
        if (state.currentMediaItemId != lastMediaItemId) {
            lastMediaItemId = state.currentMediaItemId
            userOverriddenMode = null
        }
        if (userOverriddenMode == null && state.durationMs > 0) {
            _isPodcastMode.value = state.durationMs > PODCAST_MODE_DURATION_MS
        }
        playerState.value = AudioPlayerUiState.Data(
            isPlaying = state.isPlaying,
            currentPosition = state.currentPositionMs,
            duration = state.durationMs,
            repeatMode = state.repeatMode,
            shuffleEnabled = state.shuffleEnabled,
            hasPlaylist = state.mediaItemCount > 1,
            isLoading = state.isBuffering,
            title = state.title,
            artist = state.artist,
            artworkUri = state.artworkUri,
            currentPlayingHandle = state.currentMediaItemHandle,
            thumbnailData = state.currentMediaItemHandle?.let { ThumbnailRequest.fromHandle(it) },
            currentPlayingItemName = existing?.currentPlayingItemName,
            currentAdapterType = data?.adapterType ?: INVALID_VALUE,
            nodeSourceType = data?.nodeSourceType ?: NodeSourceType.MEDIA_PLAYER_DEFAULT,
            fileLinkUrl = data?.fileLinkUrl,
            localFilePath = data?.localFilePath,
            chatId = data?.chatId,
            msgId = data?.msgId,
            currentPlaybackSpeed = state.playbackSpeed,
        )
    }

    private fun fetchNodeName(handle: Long) {
        viewModelScope.launch {
            runCatching { getNodeByHandleUseCase(handle)?.name }
                .onSuccess { name ->
                    playerState.update { state ->
                        if (state is AudioPlayerUiState.Data) state.copy(currentPlayingItemName = name)
                        else state
                    }
                }
                .onFailure { Timber.w(it, "Failed to fetch node name for handle=$handle") }
        }
    }

    private fun prefetchPlayerMode(handle: Long) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            if (userOverriddenMode != null) return@launch
            val node = runCatching { getNodeByHandleUseCase(handle) }
                .onFailure { Timber.w(it, "Failed to prefetch node for player mode detection, handle=$handle") }
                .getOrNull() as? TypedAudioNode ?: return@launch
            if (userOverriddenMode != null) return@launch
            _isPodcastMode.value = node.duration.inWholeMilliseconds > PODCAST_MODE_DURATION_MS
        }
    }

    fun startPlayback(intent: Intent) {
        setCurrentIntent(intent)
        val handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_VALUE.toLong())
        if (handle != INVALID_VALUE.toLong()) {
            prefetchPlayerMode(handle)
        }
        val rebuildPlaylist = intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)
        if (rebuildPlaylist) {
            gateway.startService(intent)
        }
    }

    fun togglePlayPause() {
        val state = currentControllerState ?: return
        if (state.isPlaying) gateway.pause() else gateway.play()
    }

    fun seekTo(positionMs: Long) {
        gateway.seekTo(positionMs)
    }

    fun skipToNext() {
        gateway.skipToNext()
    }

    fun skipToPrevious() {
        gateway.skipToPrevious()
    }

    fun toggleShuffle() {
        val state = currentControllerState ?: return
        gateway.setShuffleEnabled(!state.shuffleEnabled)
    }

    fun togglePlayerMode() {
        val newMode = !_isPodcastMode.value
        userOverriddenMode = newMode
        _isPodcastMode.value = newMode
    }

    fun seekForward15() {
        val state = currentControllerState ?: return
        val target = if (state.durationMs > 0) {
            minOf(state.currentPositionMs + SEEK_15_SECONDS_MS, state.durationMs)
        } else {
            state.currentPositionMs + SEEK_15_SECONDS_MS
        }
        gateway.seekTo(target)
    }

    fun seekBackward15() {
        val state = currentControllerState ?: return
        gateway.seekTo(maxOf(0L, state.currentPositionMs - SEEK_15_SECONDS_MS))
    }

    fun cycleRepeatMode() {
        val state = currentControllerState ?: return
        gateway.setRepeatMode(
            when (state.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        )
    }

    private fun setCurrentIntent(intent: Intent) {
        val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        if (adapterType == INVALID_VALUE) {
            Timber.w("Audio player launched without a valid adapter type")
        }
        val rawChatId = intent.getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, INVALID_HANDLE)
        val rawMsgId = intent.getLongExtra(INTENT_EXTRA_KEY_MSG_ID, INVALID_HANDLE)
        intentData = IntentData(
            adapterType = adapterType,
            nodeSourceType = adapterTypeToNodeSourceType(adapterType),
            fileLinkUrl = intent.getStringExtra(URL_FILE_LINK),
            localFilePath = intent.getStringExtra(URL_LOCAL_FILE_PATH),
            chatId = rawChatId.takeIf { it != INVALID_HANDLE },
            msgId = rawMsgId.takeIf { it != INVALID_HANDLE },
        )
        playerState.update { state ->
            if (state is AudioPlayerUiState.Data) {
                val data = intentData ?: return@update state
                state.copy(
                    currentAdapterType = data.adapterType,
                    nodeSourceType = data.nodeSourceType,
                    fileLinkUrl = data.fileLinkUrl,
                    localFilePath = data.localFilePath,
                    chatId = data.chatId,
                    msgId = data.msgId,
                )
            } else state
        }
    }

    private fun adapterTypeToNodeSourceType(adapterType: Int): NodeSourceType =
        when (adapterType) {
            OFFLINE_ADAPTER -> NodeSourceType.OFFLINE
            RUBBISH_BIN_ADAPTER -> NodeSourceType.RUBBISH_BIN
            FOLDER_LINK_ADAPTER, FROM_ALBUM_SHARING -> NodeSourceType.FOLDER_LINK
            FROM_CHAT -> NodeSourceType.CHAT
            FILE_LINK_ADAPTER -> NodeSourceType.FILE_LINK
            FROM_IMAGE_VIEWER -> NodeSourceType.MEDIA_PLAYER_IMAGE_VIEWER
            VERSIONS_ADAPTER -> NodeSourceType.MEDIA_PLAYER_VERSIONS
            ZIP_ADAPTER -> NodeSourceType.MEDIA_PLAYER_ZIP_FILE
            else -> NodeSourceType.MEDIA_PLAYER_DEFAULT
        }

    fun setPlaybackSpeed(speed: Float) {
        gateway.setPlaybackSpeed(speed)
    }

    fun stopPlayer() {
        gateway.stop()
    }

    override fun onCleared() {
        gateway.release()
        super.onCleared()
    }

    companion object {
        // Tracks longer than 12 minutes are automatically detected as podcasts; shorter tracks are classified as music.
        // The 12-minute threshold is defined in the design specification.
        private const val PODCAST_MODE_DURATION_MS = 12 * 60 * 1_000L
        private const val SEEK_15_SECONDS_MS = 15_000L
    }
}
