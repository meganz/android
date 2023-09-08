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
import mega.privacy.android.feature.devicecenter.ui.renamedevice.model.RenameDeviceState
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
     * @param deviceName The new Device Name
     */
    fun renameDevice(deviceId: String, deviceName: String) = viewModelScope.launch {
        renameDeviceUseCase(
            deviceId = deviceId,
            deviceName = deviceName,
        )
        _state.update { it.copy(renameSuccessfulEvent = triggered) }
    }

    /**
     * Notifies [RenameDeviceState.renameSuccessfulEvent] that it has been consumed
     */
    fun onResetRenameSuccessfulEvent() = _state.update { it.copy(renameSuccessfulEvent = consumed) }
}