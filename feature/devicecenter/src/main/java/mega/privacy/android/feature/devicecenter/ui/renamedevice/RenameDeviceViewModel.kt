package mega.privacy.android.feature.devicecenter.ui.renamedevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.backup.RenameDeviceUseCase
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.renamedevice.model.RenameDeviceState
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] containing all functionalities for [RenameDeviceDialog]
 *
 * @property renameDeviceUseCase [RenameDeviceUseCase]
 */
@HiltViewModel
class RenameDeviceViewModel @Inject constructor(
    private val renameDeviceUseCase: RenameDeviceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RenameDeviceState())

    /**
     * The State of [RenameDeviceDialog]
     */
    val state: StateFlow<RenameDeviceState> = _state.asStateFlow()

    /**
     * Renames a Device
     *
     * @param deviceId The Device ID identifying the Device to be renamed
     * @param newDeviceName The new Device Name
     * @param existingDeviceNames The list of existing Device Names
     */
    fun renameDevice(
        deviceId: String,
        newDeviceName: String,
        existingDeviceNames: List<String>,
    ) = viewModelScope.launch {
        runCatching {
            if (newDeviceName.isBlank()) {
                _state.update { it.copy(errorMessage = R.string.device_center_rename_device_dialog_error_message_empty_device_name) }
            } else if (newDeviceName in existingDeviceNames) {
                _state.update { it.copy(errorMessage = R.string.device_center_rename_device_dialog_error_message_name_already_exists) }
            } else if (INVALID_CHARACTER_REGEX.toRegex().containsMatchIn(newDeviceName)) {
                _state.update { it.copy(errorMessage = R.string.device_center_rename_device_dialog_error_message_invalid_characters) }
            } else {
                renameDeviceUseCase(
                    deviceId = deviceId,
                    deviceName = newDeviceName,
                )
                _state.update {
                    it.copy(
                        errorMessage = null,
                        renameSuccessfulEvent = triggered,
                    )
                }
            }
        }.onFailure { Timber.e(it) }
    }

    /**
     * Clears the Error Message from the Dialog
     */
    fun clearErrorMessage() = _state.update { it.copy(errorMessage = null) }

    /**
     * Notifies [RenameDeviceState.renameSuccessfulEvent] that it has been consumed
     */
    fun resetRenameSuccessfulEvent() = _state.update { it.copy(renameSuccessfulEvent = consumed) }

    companion object {
        private const val INVALID_CHARACTER_REGEX = "[\\\\*/:<>?\"|]"
    }
}