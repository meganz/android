package mega.privacy.android.feature.devicecenter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.devicecenter.domain.usecase.GetDevicesUseCase
import mega.privacy.android.feature.devicecenter.ui.mapper.DeviceUINodeListMapper
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterState
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] for the Device Center Screen
 *
 * @property getDevicesUseCase [GetDevicesUseCase]
 * @property deviceUINodeListMapper [DeviceUINodeListMapper]
 */
internal class DeviceCenterViewModel @Inject constructor(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val deviceUINodeListMapper: DeviceUINodeListMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceCenterState())

    /**
     * The State of [DeviceCenterScreen]
     */
    val state: StateFlow<DeviceCenterState> = _state.asStateFlow()

    init {
        retrieveBackupInfo()
    }

    /**
     * Retrieves the User's Backup Information
     */
    private fun retrieveBackupInfo() = viewModelScope.launch {
        runCatching {
            val backupDevices = getDevicesUseCase()
            _state.update { it.copy(nodes = deviceUINodeListMapper(backupDevices)) }
        }.onFailure {
            Timber.w(it)
        }
    }
}