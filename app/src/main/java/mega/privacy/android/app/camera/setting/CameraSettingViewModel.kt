package mega.privacy.android.app.camera.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.setting.EnableGeoTaggingUseCase
import mega.privacy.android.domain.usecase.setting.MonitorGeoTaggingStatusUseCase
import javax.inject.Inject

@HiltViewModel
internal class CameraSettingViewModel @Inject constructor(
    private val enableGeoTaggingUseCase: EnableGeoTaggingUseCase,
    private val monitorGeoTaggingStatusUseCase: MonitorGeoTaggingStatusUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraSettingUiState())
    val uiState: StateFlow<CameraSettingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            monitorGeoTaggingStatusUseCase().collect { isEnabled ->
                _uiState.value = CameraSettingUiState(isGeoTaggingEnabled = isEnabled)
            }
        }
    }

    fun setSaveLocationToMedia(value: Boolean) {
        viewModelScope.launch {
            enableGeoTaggingUseCase(value)
        }
    }
}