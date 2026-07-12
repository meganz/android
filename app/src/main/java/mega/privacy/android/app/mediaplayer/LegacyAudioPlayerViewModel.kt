package mega.privacy.android.app.mediaplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.mediaplayer.AudioPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.AudioSpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.LegacyAudioPlayerUiState
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.presentation.videoplayer.model.PlaybackPositionStatus
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.DeleteAudioPlaybackInfoUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetMediaPlaybackInfoUseCase

@HiltViewModel
class LegacyAudioPlayerViewModel @Inject constructor(
    @AudioPlayer private val mediaPlayerGateway: MediaPlayerGateway,
    private val getMediaPlaybackInfoUseCase: GetMediaPlaybackInfoUseCase,
    private val deleteAudioPlaybackInfoUseCase: DeleteAudioPlaybackInfoUseCase,
) : ViewModel() {
    val uiState: StateFlow<LegacyAudioPlayerUiState>
        field: MutableStateFlow<LegacyAudioPlayerUiState> = MutableStateFlow(LegacyAudioPlayerUiState())
    private var playbackPositionStatus = PlaybackPositionStatus.Initial
    private var playbackPositionJob: Job? = null

    /**
     * Tracks the last media item handle for which playback-position logic was triggered.
     * Kept in the ViewModel (not the Fragment) so it survives configuration changes (e.g. rotation)
     * and prevents the StateFlow replay from re-triggering the dialog or re-seeking.
     */
    private var lastProcessedMediaItemHandle: Long? = null

    /**
     * Returns `true` and records [handle] if it has not been processed before, or `false` if it
     * was already processed. Used by the Fragment to deduplicate StateFlow replays across
     * configuration changes without exposing mutable state.
     */
    internal fun shouldProcessMediaItem(handle: Long?): Boolean {
        if (handle == lastProcessedMediaItemHandle) return false
        lastProcessedMediaItemHandle = handle
        return true
    }

    init {
        val defaultSpeedItem = AudioSpeedPlaybackItem.entries.find {
            it.speed == mediaPlayerGateway.getCurrentPlaybackSpeed()
        } ?: AudioSpeedPlaybackItem.PlaybackSpeed_1X

        updateCurrentSpeedPlaybackItem(defaultSpeedItem)
    }

    internal fun updateIsSpeedPopupShown(value: Boolean) {
        uiState.update { it.copy(isSpeedPopupShown = value) }
    }

    internal fun updateCurrentSpeedPlaybackItem(item: SpeedPlaybackItem) {
        mediaPlayerGateway.updatePlaybackSpeed(item)
        uiState.update { it.copy(currentSpeedPlayback = item) }
    }

    internal fun checkPlaybackPositionOfPlayingItem(
        handle: Long,
        name: String,
        status: PlaybackPositionStatus = playbackPositionStatus,
        isResume: Boolean = true,
        playbackPositionStatusCallback: (PlaybackPositionStatus) -> Unit,
    ) {
        playbackPositionJob?.cancel()
        playbackPositionJob = viewModelScope.launch {
            val playbackPosition = getMediaPlaybackInfoUseCase(handle)?.currentPosition
            if (playbackPosition != null && playbackPosition > 0) {
                mediaPlayerGateway.setPlayWhenReady(false)
                when (status) {
                    PlaybackPositionStatus.Initial -> {
                        playbackPositionStatus = PlaybackPositionStatus.DialogShowing
                        showPlaybackPositionDialog(handle, name, playbackPosition)
                        playbackPositionStatusCallback(playbackPositionStatus)
                    }

                    PlaybackPositionStatus.DialogShowing -> {
                        // Defense-in-depth: handles any edge case where this function is called
                        // while the dialog is already showing (e.g. a code path that bypasses the
                        // shouldProcessMediaItem filter). Normally unreachable on screen rotation.
                        showPlaybackPositionDialog(handle, name, playbackPosition)
                        playbackPositionStatusCallback(PlaybackPositionStatus.DialogShowing)
                    }

                    else -> updatePlaybackPositionStatus(
                        handle = handle,
                        status = status,
                        playbackPosition = playbackPosition,
                        isResume = isResume,
                    )
                }
            }
        }
    }

    private fun showPlaybackPositionDialog(handle: Long, name: String, position: Long) {
        uiState.update {
            it.copy(
                showPlaybackDialog = true,
                playbackPosition = position,
                currentPlayingHandle = handle,
                currentPlayingItemName = name
            )
        }
    }

    internal fun updatePlaybackPositionStatus(
        handle: Long,
        status: PlaybackPositionStatus,
        playbackPosition: Long? = uiState.value.playbackPosition,
        isResume: Boolean = true,
        isClearPosition: Boolean = false,
    ) {
        viewModelScope.launch {
            when {
                status == PlaybackPositionStatus.Resume && playbackPosition != null ->
                    mediaPlayerGateway.playerSeekToPositionInMs(playbackPosition)

                status == PlaybackPositionStatus.Restart && playbackPosition != null && isClearPosition ->
                    deleteAudioPlaybackInfoUseCase(handle)
            }

            playbackPositionStatus = status
            uiState.update { it.copy(showPlaybackDialog = false) }

            if (!mediaPlayerGateway.getPlayWhenReady() && isResume) {
                mediaPlayerGateway.setPlayWhenReady(true)
            }
        }
    }
}
