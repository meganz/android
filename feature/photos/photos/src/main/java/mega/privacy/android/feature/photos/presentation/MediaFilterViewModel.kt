package mega.privacy.android.feature.photos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.feature.photos.mapper.MediaFilterUiStateMapper
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import javax.inject.Inject

@HiltViewModel
class MediaFilterViewModel @Inject constructor(
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val mediaFilterUiStateMapper: MediaFilterUiStateMapper,
) : ViewModel() {

    internal val uiState: StateFlow<MediaFilterUiState> by lazy {
        flow { emit(getTimelineFilterPreferencesUseCase()) }
            .map { preferenceMap ->
                mediaFilterUiStateMapper(preferenceMap = preferenceMap)
            }
            .asUiStateFlow(
                scope = viewModelScope,
                initialValue = MediaFilterUiState()
            )
    }
}
