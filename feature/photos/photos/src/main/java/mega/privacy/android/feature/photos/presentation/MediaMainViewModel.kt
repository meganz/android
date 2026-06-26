package mega.privacy.android.feature.photos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MediaMainViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {
    internal val uiState: StateFlow<MediaMainUiState>
        field: MutableStateFlow<MediaMainUiState> = MutableStateFlow(MediaMainUiState())

    init {
        getTimelineRevampFlag()
    }

    fun showNewAlbumDialog() {
        uiState.update {
            it.copy(newAlbumDialogEvent = triggered)
        }
    }

    fun resetNewAlbumDialog() {
        uiState.update {
            it.copy(newAlbumDialogEvent = consumed)
        }
    }

    private fun getTimelineRevampFlag() {
        viewModelScope.launch {
            runCatching {
                val isEnabled = getFeatureFlagValueUseCase(AppFeatures.TimelineRevamp)
                uiState.update {
                    it.copy(isTimelineRevampEnabled = isEnabled)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}