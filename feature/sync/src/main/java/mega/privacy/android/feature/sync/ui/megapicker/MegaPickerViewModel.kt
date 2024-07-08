package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.shared.resources.R as sharedResR
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.sync.TryNodeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSelectedMegaFolderUseCase
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class MegaPickerViewModel @Inject constructor(
    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getTypedNodesFromFolder: GetTypedNodesFromFolderUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val tryNodeSyncUseCase: TryNodeSyncUseCase,
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper,
    private val getCameraUploadsFolderHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getMediaUploadsFolderHandleUseCase: GetSecondaryFolderNodeUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
    private val createFolderNodeUseCase: CreateFolderNodeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MegaPickerState())
    val state: StateFlow<MegaPickerState> = _state.asStateFlow()

    private var allFilesPermissionShown = false
    private var disableBatteryOptimizationsPermissionShown = false

    private var rootFolder: Node? = null

    private var getNodesFromFolderJob: Job? = null

    init {
        viewModelScope.launch {
            rootFolder = getRootNodeUseCase()
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
                        parentNode?.let(::fetchFolders)
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
                viewModelScope.launch {
                    runCatching {
                        tryNodeSyncUseCase(state.value.currentFolder?.id ?: NodeId(0))
                    }.onSuccess {
                        folderSelected()
                    }.onFailure {
                        val error = (it as MegaSyncException).syncError
                        val errorMessage = deviceFolderUINodeErrorMessageMapper(error)
                            ?: sharedResR.string.general_sync_message_unknown_error

                        _state.update { state ->
                            state.copy(
                                errorMessageId = errorMessage
                            )
                        }
                    }
                }
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

            MegaPickerAction.ErrorMessageShown -> {
                _state.update {
                    it.copy(errorMessageId = null)
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
        getNodesFromFolderJob?.cancel()
        getNodesFromFolderJob = viewModelScope.launch {
            val excludeFolders = if (currentFolder.id == rootFolder?.id) {
                runCatching {
                    val cameraUploadsFolderHandle = getCameraUploadsFolderHandleUseCase()
                    val mediaUploadsFolderHandle = getMediaUploadsFolderHandleUseCase()?.id
                    val myChatsUploadsFolderHandle = getMyChatsFilesFolderIdUseCase()

                    listOfNotNull(
                        NodeId(cameraUploadsFolderHandle),
                        mediaUploadsFolderHandle,
                        myChatsUploadsFolderHandle
                    ).filterNot { it == NodeId(-1L) }
                        .ifEmpty { null }
                }
                    .onFailure {
                        Timber.d(it, "Error getting handles of CU and MyChat files")
                    }
                    .getOrNull()
            } else {
                null
            }

            runCatching {
                getTypedNodesFromFolder(currentFolder.id).collectLatest { childFolders ->
                    _state.update { megaPickerState ->
                        megaPickerState.copy(
                            currentFolder = currentFolder,
                            nodes = if (excludeFolders != null) {
                                childFolders.map {
                                    TypedNodeUiModel(
                                        it,
                                        excludeFolders.contains(it.id)
                                    )
                                }
                            } else {
                                childFolders.map { TypedNodeUiModel(it, false) }
                            }
                        )
                    }
                }
            }.onFailure {
                Timber.d(it, "Error getting child folders of current folder ${currentFolder.name}")
            }
        }
    }

    /**
     * Creates a new folder
     *
     * @param newFolderName The new folder name
     * @param parentNode    Parent node under which the folder should be created
     */
    fun createFolder(
        newFolderName: String,
        parentNode: Node?,
    ) = viewModelScope.launch {
        runCatching {
            createFolderNodeUseCase(
                name = newFolderName,
                parentNodeId = parentNode?.id,
            )
        }.onSuccess { nodeId ->
            runCatching {
                getNodeByHandleUseCase(nodeId.longValue)
            }.onSuccess { node ->
                node?.let(::fetchFolders)
            }.onFailure { Timber.e(it) }
        }.onFailure { Timber.e(it) }
    }
}