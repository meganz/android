package mega.privacy.android.feature.devicecenter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
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
            Timber.d("Selected item set: $selectedItem")
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
                        applySecondaryColorIconTint = item is DeviceUINode,
                        // Reset all numeric fields to 0 when loading new item
                        numberOfFiles = 0,
                        numberOfFolders = 0,
                        totalSizeInBytes = 0L,
                        creationTime = 0L
                    )
                }

                when (item) {
                    is DeviceUINode -> {
                        loadDeviceInfo(device = item)
                    }

                    is DeviceFolderUINode -> {
                        loadFolderInfo(folderHandle = item.rootHandle)
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
        loadFolderTreeInfoForNode(folderHandle)
    }

    /**
     * Load folder info
     *
     * @param folderHandle Handle of the folder to load the info
     */
    private fun loadFolderInfo(folderHandle: Long) {
        loadFolderTreeInfoForNode(
            folderHandle = folderHandle,
            adjustFolderCount = -1,
            onNodeLoaded = { folder ->
                _state.update { state ->
                    state.copy(creationTime = folder.creationTime)
                }
            }
        )
    }

    /**
     * Common function to load folder tree info for a given node handle
     *
     * @param folderHandle Handle of the folder to load the info
     * @param adjustFolderCount Adjustment to folder count (default 0)
     * @param onNodeLoaded Optional callback when node is loaded
     */
    private fun loadFolderTreeInfoForNode(
        folderHandle: Long,
        adjustFolderCount: Int = 0,
        onNodeLoaded: ((TypedNode) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            runCatching {
                getNodeByIdUseCase(NodeId(folderHandle))
            }.onSuccess { node ->
                node?.let { folder ->
                    onNodeLoaded?.invoke(folder)
                    runCatching {
                        getFolderTreeInfo(folder as TypedFolderNode)
                    }.onSuccess { folderTreeInfo ->
                        updateFolderTreeStats(folderTreeInfo, adjustFolderCount)
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
     * Updates folder tree statistics in the state
     *
     * @param folderTreeInfo The folder tree information
     * @param adjustFolderCount Adjustment to folder count (default 0)
     */
    private fun updateFolderTreeStats(
        folderTreeInfo: FolderTreeInfo,
        adjustFolderCount: Int = 0,
    ) {
        with(folderTreeInfo) {
            _state.update { state ->
                state.copy(
                    numberOfFiles = state.numberOfFiles + numberOfFiles,
                    numberOfFolders = state.numberOfFolders + numberOfFolders + adjustFolderCount,
                    totalSizeInBytes = state.totalSizeInBytes + totalCurrentSizeInBytes,
                )
            }
        }
    }
}
