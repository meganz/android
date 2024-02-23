package mega.privacy.android.app.settings.camerauploads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.settings.camerauploads.model.SettingsCameraUploadsState
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The [ViewModel] for Settings Camera Uploads
 *
 * @property isCameraUploadsEnabledUseCase Checks if Camera Uploads is enabled or not
 */
internal class SettingsCameraUploadsViewModel @Inject constructor(
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsCameraUploadsState())

    /**
     * The State of Settings Camera Uploads
     */
    val state: StateFlow<SettingsCameraUploadsState> = _state.asStateFlow()

    init {
        initializeSettings()
    }

    /**
     * Configures all settings upon opening Settings Camera Uploads
     */
    private fun initializeSettings() {
        viewModelScope.launch {
            runCatching {
                val isCameraUploadsEnabled = isCameraUploadsEnabledUseCase()
                _state.update { it.copy(isCameraUploadsEnabled = isCameraUploadsEnabled) }
            }.onFailure {
                Timber.e("An error occurred when initializing Settings Camera Uploads:\n$it")
            }
        }
    }
}