package mega.privacy.android.feature.sync.ui.megapicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolderUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSelectedMegaFolderUseCase
import javax.inject.Inject

@HiltViewModel
internal class MegaPickerViewModel @Inject constructor(
    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getTypedNodesFromFolder: GetTypedNodesFromFolderUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MegaPickerState())
    val state: StateFlow<MegaPickerState> = _state.asStateFlow()

    private var allFilesPermissionShown = false
    private var disableBatteryOptimizationsPermissionShown = false

    init {
        viewModelScope.launch {
            val rootFolder = getRootNodeUseCase()
            rootFolder?.let(::fetchFolders)
        }
    }

    fun handleAction(action: MegaPickerAction) {
        when (action) {
            is MegaPickerAction.FolderClicked -> {
                fetchFolders(action.folder)
            }

            is MegaPickerAction.BackClicked -> {
                state.value.currentFolder?.let { currentFolder ->
                    viewModelScope.launch {
                        val parentNode = getNodeByHandleUseCase(currentFolder.parentId.longValue)
                        parentNode
                            ?.let(::fetchFolders)
                    }
                }
            }

            is MegaPickerAction.CurrentFolderSelected -> {
                if (action.allFilesAccessPermissionGranted) {
                    allFilesPermissionShown = true
                }
                if (action.disableBatteryOptimizationPermissionGranted) {
                    disableBatteryOptimizationsPermissionShown = true
                }
                folderSelected()
            }

            MegaPickerAction.AllFilesAccessPermissionDialogShown -> {
                _state.update {
                    it.copy(
                        showAllFilesAccessDialog = false
                    )
                }

                allFilesPermissionShown = true
            }

            MegaPickerAction.DisableBatteryOptimizationsDialogShown -> {
                _state.update {
                    it.copy(
                        showDisableBatteryOptimizationsDialog = false
                    )
                }

                disableBatteryOptimizationsPermissionShown = true
            }

            MegaPickerAction.NextScreenOpened -> {
                _state.update {
                    it.copy(
                        navigateNextEvent = consumed
                    )
                }
            }
        }
    }

    private fun folderSelected() {
        when {
            allFilesPermissionShown && disableBatteryOptimizationsPermissionShown -> {
                saveSelectedFolder()
                _state.update {
                    it.copy(
                        navigateNextEvent = triggered
                    )
                }
            }

            !allFilesPermissionShown -> {
                _state.update {
                    it.copy(
                        showAllFilesAccessDialog = true
                    )
                }
            }

            else -> {
                _state.update {
                    it.copy(
                        showDisableBatteryOptimizationsDialog = true
                    )
                }
            }
        }
    }

    private fun saveSelectedFolder() {
        val id = state.value.currentFolder?.id?.longValue
        val name = state.value.currentFolder?.name
        if (id != null && name != null) {
            setSelectedMegaFolderUseCase(RemoteFolder(id, name))
        }
    }

    private fun fetchFolders(currentFolder: Node) {
        viewModelScope.launch {
            getTypedNodesFromFolder(currentFolder.id).collectLatest { childFolders ->
                _state.update {
                    it.copy(
                        currentFolder = currentFolder, nodes = childFolders
                    )
                }
            }
        }
    }
}