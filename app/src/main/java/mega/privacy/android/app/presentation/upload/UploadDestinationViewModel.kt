package mega.privacy.android.app.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [UploadDestinationActivity]
 */
@HiltViewModel
class UploadDestinationViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadDestinationUiState())

    /**
     * UI state for [UploadDestinationActivity]
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.NewUploadDestinationActivity)
            }.onSuccess { result ->
                _uiState.update { it.copy(isNewUploadScreenEnabled = result) }
            }.onFailure { error ->
                Timber.e(error)
                _uiState.update { it.copy(isNewUploadScreenEnabled = false) }
            }
        }
    }
}