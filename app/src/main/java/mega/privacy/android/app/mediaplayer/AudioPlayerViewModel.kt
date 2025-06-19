package mega.privacy.android.app.mediaplayer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.di.mediaplayer.AudioPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import mega.privacy.android.app.mediaplayer.model.AudioSpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    @AudioPlayer private val mediaPlayerGateway: MediaPlayerGateway,
) : ViewModel() {
    val uiState: StateFlow<AudioPlayerUiState>
        field: MutableStateFlow<AudioPlayerUiState> = MutableStateFlow(AudioPlayerUiState())

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
}