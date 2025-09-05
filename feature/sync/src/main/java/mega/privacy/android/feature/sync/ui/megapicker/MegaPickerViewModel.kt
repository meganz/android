package mega.privacy.android.feature.sync.ui.megapicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.sync.TryNodeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSelectedMegaFolderUseCase
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import timber.log.Timber

@HiltViewModel(assistedFactory = MegaPickerViewModel.MegaPickerViewModelFactory::class)
internal class MegaPickerViewModel @AssistedInject constructor(
    @Assisted val isStopBackup: Boolean = false,
    @Assisted val folderName: String? = null,
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
) : ViewModel() {

    @AssistedFactory
    interface MegaPickerViewModelFactory {
        fun create(
            isStopBackup: Boolean,
            folderName: String? = null,
        ): MegaPickerViewModel
    }

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
                viewModelScope.launch {
                    if (action.allFilesAccessPermissionGranted) {
                        allFilesPermissionShown = true
                    }
                    if (action.disableBatteryOptimizationPermissionGranted || !getFeatureFlagValueUseCase(
                            SyncFeatures.DisableBatteryOptimization
                        )
                    ) {
                        disableBatteryOptimizationsPermissionShown = true
                    }
                    runCatching {
                        if (isStopBackup.not()) {
                            state.value.currentFolder?.let {
                                tryNodeSyncUseCase(it.id)
                            }
                        } else {
                            state.value.currentFolder?.id?.let { currentFolder ->
                                isFolderExists(currentFolder).let {
                                    Timber.d("Folder exists: $it")
                                    if (it) return@launch
                                }
                            }
                        }
                    }.onSuccess {
                        folderSelected()
                    }.onFailure {
                        val error = (it as MegaSyncException).syncError
                        val errorMessage = deviceFolderUINodeErrorMessageMapper(error)
                            ?: deviceFolderUINodeErrorMessageMapper(SyncError.UNKNOWN_ERROR)

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
        Timber.d("Folder selected: ${state.value.currentFolder?.name}, id: ${state.value.currentFolder?.id}")
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
            setSelectedMegaFolderUseCase(RemoteFolder(NodeId(id), name))
        }
    }

    private fun fetchFolders(currentFolder: Node) {
        getNodesFromFolderJob?.cancel()
        getNodesFromFolderJob = viewModelScope.launch {
            _state.update { state -> state.copy(isLoading = true) }
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

            Timber.d("Current folder: ${currentFolder.name}, id: ${currentFolder.id}, RootFolder: ${rootFolder?.name}, id: ${rootFolder?.id}, Exclude folders: $excludeFolders")

            val isSelectEnabled =
                if (isStopBackup) isFolderExists(currentFolder.id).not() else currentFolder.id != rootFolder?.id

            getTypedNodesFromFolder(currentFolder.id).catch {
                Timber.d(it, "Error getting child folders of current folder ${currentFolder.name}")
            }.collectLatest { childFolders ->
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
                        },
                        isSelectEnabled = isSelectEnabled,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private suspend fun isFolderExists(currentFolder: NodeId) =
        folderName?.let {
            nodeExistsInCurrentLocationUseCase(currentFolder, it)
        } ?: true


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
