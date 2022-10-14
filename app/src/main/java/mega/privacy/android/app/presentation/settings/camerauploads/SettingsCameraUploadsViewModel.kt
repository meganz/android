package mega.privacy.android.app.presentation.settings.camerauploads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsState
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import javax.inject.Inject

/**
 * [ViewModel] class for SettingsCameraUploadsFragment
 *
 * @param checkEnableCameraUploadsStatus Use Case to check the Camera Uploads status before enabling
 */
@HiltViewModel
class SettingsCameraUploadsViewModel @Inject constructor(
    private val checkEnableCameraUploadsStatus: CheckEnableCameraUploadsStatus,
    private val restorePrimaryTimestamps: RestorePrimaryTimestamps,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsCameraUploadsState())

    /**
     * State of Settings Camera Uploads
     */
    val state: StateFlow<SettingsCameraUploadsState> = _state.asStateFlow()

    /**
     * Checks whether Camera Uploads can be enabled and handles the Status accordingly, as
     * determined by the Use Case [checkEnableCameraUploadsStatus]
     */
    fun handleEnableCameraUploads() = viewModelScope.launch {
        when (checkEnableCameraUploadsStatus.invoke()) {
            EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS -> {
                _state.update { it.copy(shouldTriggerCameraUploads = true) }
            }
            EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT -> {
                _state.update { it.copy(shouldShowBusinessAccountPrompt = true) }
            }
            EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT -> {
                _state.update { it.copy(shouldShowBusinessAccountSuspendedPrompt = true) }
            }
        }
    }

    /**
     * Resets the value of [SettingsCameraUploadsState.shouldShowBusinessAccountPrompt] to False
     */
    fun resetBusinessAccountPromptState() =
        _state.update { it.copy(shouldShowBusinessAccountPrompt = false) }

    /**
     * Resets the value of [SettingsCameraUploadsState.shouldShowBusinessAccountSuspendedPrompt] to False
     */
    fun resetBusinessAccountSuspendedPromptState() =
        _state.update { it.copy(shouldShowBusinessAccountSuspendedPrompt = false) }

    /**
     * Sets the value of [SettingsCameraUploadsState.shouldTriggerCameraUploads]
     * @param updatedValue the updated Boolean value of the parameter
     */
    fun setTriggerCameraUploadsState(updatedValue: Boolean) =
        _state.update { it.copy(shouldTriggerCameraUploads = updatedValue) }

    /**
     * If the handle matches the previous primary folder's handle, restore the time stamp from stamps
     * if not clean the sync record from previous primary folder
     */
    fun restorePrimaryTimestampsAndSyncRecordProcess() {
        viewModelScope.launch {
            restorePrimaryTimestamps()
        }
    }
}