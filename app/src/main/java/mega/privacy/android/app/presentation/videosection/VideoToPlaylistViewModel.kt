package mega.privacy.android.app.presentation.videosection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.videosection.model.VideoToPlaylistUiState
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistSetsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Video playlist view model
 */
@HiltViewModel
class VideoToPlaylistViewModel @Inject constructor(
    private val getVideoPlaylistSetsUseCase: GetVideoPlaylistSetsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoToPlaylistUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        loadVideoPlaylistSets()
    }

    private fun loadVideoPlaylistSets() {
        viewModelScope.launch {
            runCatching {
                getVideoPlaylistSetsUseCase()
            }.onSuccess { sets ->
                _uiState.update { it.copy(items = sets) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}