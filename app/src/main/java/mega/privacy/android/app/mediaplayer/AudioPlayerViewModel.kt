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
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import mega.privacy.android.app.mediaplayer.model.AudioSpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.PlaybackPositionState
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.presentation.videoplayer.model.PlaybackPositionStatus
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetMediaPlaybackInfoUseCase

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    @AudioPlayer private val mediaPlayerGateway: MediaPlayerGateway,
    private val getMediaPlaybackInfoUseCase: GetMediaPlaybackInfoUseCase,
) : ViewModel() {
    val uiState: StateFlow<AudioPlayerUiState>
        field: MutableStateFlow<AudioPlayerUiState> = MutableStateFlow(AudioPlayerUiState())
    private var playbackPositionStatus = PlaybackPositionStatus.Initial
    private var playbackPositionJob: Job? = null

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
    ) {
        playbackPositionJob?.cancel()
        playbackPositionJob = viewModelScope.launch {
            val playbackPosition = getMediaPlaybackInfoUseCase(handle)?.currentPosition
            if (playbackPosition != null && playbackPosition > 0) {
                mediaPlayerGateway.setPlayWhenReady(false)
                when (status) {
                    PlaybackPositionStatus.Initial -> {
                        playbackPositionStatus = PlaybackPositionStatus.DialogShowing
                        uiState.update {
                            it.copy(
                                showPlaybackDialog = true,
                                playbackPosition = playbackPosition,
                                currentPlayingHandle = handle,
                                currentPlayingItemName = name
                            )
                        }
                    }

                    else -> updatePlaybackPositionStatus(status)
                }
            }
        }
    }

    internal fun updatePlaybackPositionStatus(
        status: PlaybackPositionStatus,
        playbackPosition: Long? = uiState.value.playbackPosition,
    ) {
        if (status == PlaybackPositionStatus.Resume && playbackPosition != null) {
            mediaPlayerGateway.playerSeekToPositionInMs(playbackPosition)
        }

        playbackPositionStatus = status
        uiState.update { it.copy(showPlaybackDialog = false) }

        if (!mediaPlayerGateway.getPlayWhenReady()) {
            mediaPlayerGateway.setPlayWhenReady(true)
        }
    }
}