package mega.privacy.android.feature.devicecenter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterInfoUiState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] for the Device Center Info View
 *
 * @property getNodeByIdUseCase [GetNodeByIdUseCase]
 * @property getFolderTreeInfo [GetFolderTreeInfo]
 */
@HiltViewModel
internal class DeviceCenterInfoViewModel @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFolderTreeInfo: GetFolderTreeInfo,
) : ViewModel() {

    private var _state = MutableStateFlow(DeviceCenterInfoUiState())
    val state: StateFlow<DeviceCenterInfoUiState> = _state.asStateFlow()

    private var selectedItem: DeviceCenterUINode? = null

    fun setSelectedItem(selectedItem: DeviceCenterUINode) {
        if (selectedItem != this.selectedItem) {
            this.selectedItem = selectedItem
            loadInfo()
        }
    }

    private fun loadInfo() {
        selectedItem?.let { item ->
            with(item) {
                _state.update {
                    it.copy(
                        name = name,
                        icon = icon.iconRes,
                        applySecondaryColorIconTint = item is DeviceUINode
                    )
                }

                when (item) {
                    is DeviceUINode -> {
                        (item as? DeviceUINode)?.let { device ->
                            loadDeviceInfo(device = device)
                        }
                    }

                    is DeviceFolderUINode -> {
                        (item as? DeviceFolderUINode)?.let { folder ->
                            loadFolderInfo(folderHandle = folder.rootHandle)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun loadDeviceInfo(device: DeviceUINode) {
        device.folders.forEach { folder ->
            loadDeviceFolderInfo(folderHandle = folder.rootHandle)
        }
    }

    /**
     * Load device folder info
     *
     * @param folderHandle Handle of the device folder to load the info
     */
    private fun loadDeviceFolderInfo(folderHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getNodeByIdUseCase(NodeId(folderHandle))
            }.onSuccess { node ->
                node?.let { folder ->
                    runCatching {
                        getFolderTreeInfo(folder as TypedFolderNode)
                    }.onSuccess { folderTreeInfo ->
                        with(folderTreeInfo) {
                            _state.update { state ->
                                state.copy(
                                    numberOfFiles = state.numberOfFiles + numberOfFiles,
                                    numberOfFolders = state.numberOfFolders + numberOfFolders,
                                    totalSizeInBytes = state.totalSizeInBytes + totalCurrentSizeInBytes,
                                )
                            }
                        }
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Load folder info
     *
     * @param folderHandle Handle of the folder to load the info
     */
    private fun loadFolderInfo(folderHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getNodeByIdUseCase(NodeId(folderHandle))
            }.onSuccess { node ->
                node?.let { folder ->
                    _state.update { state ->
                        state.copy(creationTime = folder.creationTime)
                    }
                    runCatching {
                        getFolderTreeInfo(folder as TypedFolderNode)
                    }.onSuccess { folderTreeInfo ->
                        with(folderTreeInfo) {
                            _state.update { state ->
                                state.copy(
                                    numberOfFiles = state.numberOfFiles + numberOfFiles,
                                    numberOfFolders = state.numberOfFolders + numberOfFolders - 1,
                                    totalSizeInBytes = state.totalSizeInBytes + totalCurrentSizeInBytes,
                                )
                            }
                        }
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}