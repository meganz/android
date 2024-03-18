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
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterInfoUiState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
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

    private var selectedItem: DeviceUINode? = null

    fun setSelectedItem(selectedItem: DeviceUINode) {
        if (selectedItem != this.selectedItem) {
            this.selectedItem = selectedItem
            loadInfo()
        }
    }

    private fun loadInfo() {
        selectedItem?.let { item ->
            with(item) {
                _state.update { it.copy(name = name, icon = icon.iconRes) }
                folders.forEach { folder ->
                    when (folder) {
                        is BackupDeviceFolderUINode -> loadFolderInfo(folder.rootHandle)
                        is NonBackupDeviceFolderUINode -> loadFolderInfo(folder.rootHandle)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun loadFolderInfo(folderHandle: Long) {
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
}