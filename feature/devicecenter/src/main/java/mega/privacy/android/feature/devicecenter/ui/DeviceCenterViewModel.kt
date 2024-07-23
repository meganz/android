package mega.privacy.android.feature.devicecenter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.devicecenter.domain.usecase.GetDevicesUseCase
import mega.privacy.android.feature.devicecenter.ui.mapper.DeviceUINodeListMapper
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUiState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * [ViewModel] for the Device Center Screen
 *
 * @property getDevicesUseCase [GetDevicesUseCase]
 * @property isCameraUploadsEnabledUseCase [IsCameraUploadsEnabledUseCase]
 * @property deviceUINodeListMapper [DeviceUINodeListMapper]
 * @property monitorConnectivityUseCase [MonitorConnectivityUseCase]
 * @property monitorAccountDetailUseCase [MonitorAccountDetailUseCase]
 * @property getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase]
 */
@HiltViewModel
internal class DeviceCenterViewModel @Inject constructor(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val deviceUINodeListMapper: DeviceUINodeListMapper,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceCenterUiState())

    /**
     * The State of [DeviceCenterScreen]
     */
    val state: StateFlow<DeviceCenterUiState> = _state.asStateFlow()

    private var monitorConnectivityJob: Job? = null

    init {
        monitorNetworkConnectivity()
        monitorAccountDetail()
    }

    private fun monitorNetworkConnectivity() {
        monitorConnectivityJob?.cancel()
        monitorConnectivityJob = viewModelScope.launch {
            monitorConnectivityUseCase()
                .collect { isNetworkConnected ->
                    _state.update {
                        it.copy(isNetworkConnected = isNetworkConnected)
                    }
                }
        }
    }

    private fun monitorAccountDetail() {
        viewModelScope.launch {
            monitorAccountDetailUseCase().collect { accountDetail ->
                _state.update {
                    it.copy(isFreeAccount = accountDetail.levelDetail?.accountType == AccountType.FREE)
                }
            }
        }
    }

    /**
     * A Shared Flow prompting Observers to periodically retrieve the User's Backup Information
     * at a specific interval determined by [GET_DEVICES_REFRESH_INTERVAL]
     */
    val refreshBackupInfoPromptFlow = flow {
        while (true) {
            emit(Unit)
            delay(TimeUnit.SECONDS.toMillis(GET_DEVICES_REFRESH_INTERVAL))
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Gets the User's Backup Information
     */
    fun getBackupInfo() = viewModelScope.launch {
        runCatching {
            val isCameraUploadsEnabled = isCameraUploadsEnabledUseCase()
            val devices = deviceUINodeListMapper(
                deviceNodes = getDevicesUseCase(),
            )
            val selectedDevice = getSelectedDevice(devices)
            _state.update {
                it.copy(
                    devices = devices,
                    selectedDevice = selectedDevice,
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                    isInitialLoadingFinished = true,
                )
            }
        }.onFailure {
            Timber.e(it)
            _state.update { state -> state.copy(isInitialLoadingFinished = true) }
        }
    }

    /**
     * Whenever the User's Backup Information is periodically retrieved, this retrieves the selected
     * Device from the updated Device List, so that in Folder View, the list of Folders are updated
     *
     * The selected Device is automatically null if:
     * 1. The User has not selected any Device, or
     * 2. The User's selected Device no longer exists in the updated Device List
     *
     * @param devices The list of Devices
     * @return the selected Device, or null if any of the conditions above are met
     */
    private fun getSelectedDevice(devices: List<DeviceUINode>): DeviceUINode? =
        devices.firstOrNull { it.id == _state.value.selectedDevice?.id }

    /**
     * Shows the Device Folders of a [DeviceUINode]
     *
     * @param deviceUINode The corresponding Device UI Node
     */
    fun showDeviceFolders(deviceUINode: DeviceUINode) = _state.update {
        it.copy(
            selectedDevice = deviceUINode,
            searchWidgetState = SearchWidgetState.COLLAPSED,
            searchQuery = "",
            filteredUiItems = null
        )
    }

    fun shouldNavigateToSyncs(deviceUINode: DeviceUINode) = deviceUINode is OwnDeviceUINode

    /**
     * Handles specific Back Press behavior
     */
    fun handleBackPress() {
        _state.update {
            it.copy(
                menuClickedDevice = null,
                searchWidgetState = SearchWidgetState.COLLAPSED,
                searchQuery = "",
                filteredUiItems = null
            )
        }
        if (_state.value.deviceToRename != null) {
            // The Rename Device feature is shown. Mark deviceToRename as null to dismiss the feature
            _state.update { it.copy(deviceToRename = null) }
        } else {
            if (_state.value.selectedDevice != null) {
                // The User is in Folder View. Mark selectedDevice as null to go back to Device View
                _state.update { it.copy(selectedDevice = null) }
            } else {
                // The User is in Device View. This will cause the User to leave the Device Center
                _state.update { it.copy(exitFeature = triggered) }
            }
        }
    }

    /**
     * Acknowledges that [DeviceCenterUiState.exitFeature] has been triggered, and therefore resets
     * its value
     */
    fun resetExitFeature() = _state.update { it.copy(exitFeature = consumed) }

    /**
     * Updates the value of [DeviceCenterUiState.menuClickedDevice]
     *
     * @param menuClickedDevice The [DeviceUINode] whose Context Menu is clicked
     */
    fun setMenuClickedDevice(menuClickedDevice: DeviceUINode) =
        _state.update { it.copy(menuClickedDevice = menuClickedDevice) }

    /**
     * Updates the value of [DeviceCenterUiState.deviceToRename]
     *
     * @param deviceToRename The Device to be renamed by the User
     */
    fun setDeviceToRename(deviceToRename: DeviceUINode) =
        _state.update { it.copy(deviceToRename = deviceToRename) }

    /**
     * Resets the value of [DeviceCenterUiState.deviceToRename] back to null
     */
    fun resetDeviceToRename() = _state.update { it.copy(deviceToRename = null) }

    /**
     * Updates several State parameters when the User has successfully renamed a Device
     */
    fun handleRenameDeviceSuccess() {
        _state.update {
            it.copy(
                deviceToRename = null,
                renameDeviceSuccess = triggered
            )
        }
    }

    /**
     * Notifies [DeviceCenterUiState.renameDeviceSuccess] that it has been consumed
     */
    fun resetRenameDeviceSuccessEvent() =
        _state.update { it.copy(renameDeviceSuccess = consumed) }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        query.takeIf(String::isNotBlank)?.let { searchQuery ->
            _state.update {
                it.copy(
                    filteredUiItems = it.itemsToDisplay.filter { item ->
                        item.name.contains(
                            searchQuery,
                            true
                        ) || (item is NonBackupDeviceFolderUINode && item.localFolderPath.contains(
                            searchQuery,
                            true
                        ))
                    }
                )
            }
        } ?: run {
            _state.update {
                it.copy(
                    filteredUiItems = null
                )
            }
        }
    }

    fun onSearchCloseClicked() {
        _state.update {
            it.copy(
                filteredUiItems = null,
                searchWidgetState = SearchWidgetState.COLLAPSED
            )
        }
    }

    fun onSearchClicked() {
        _state.update { it.copy(searchWidgetState = SearchWidgetState.EXPANDED) }
    }

    fun onInfoClicked(selectedItem: DeviceCenterUINode) =
        _state.update { it.copy(infoSelectedItem = selectedItem) }

    fun onInfoBackPressHandle() =
        _state.update { it.copy(infoSelectedItem = null) }

    companion object {
        /**
         * Specifies the refresh interval to update the User's Backup Information
         */
        private const val GET_DEVICES_REFRESH_INTERVAL = 30L
    }
}