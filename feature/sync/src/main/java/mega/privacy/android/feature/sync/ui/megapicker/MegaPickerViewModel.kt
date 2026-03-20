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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.backup.RemoveDeviceFolderConnectionUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerNodeInfo
import mega.privacy.android.feature.sync.domain.usecase.megapicker.MonitorMegaPickerFolderNodesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.TryNodeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSelectedMegaFolderUseCase
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import timber.log.Timber

@HiltViewModel(assistedFactory = MegaPickerViewModel.MegaPickerViewModelFactory::class)
internal class MegaPickerViewModel @AssistedInject constructor(
    @Assisted val isStopBackup: Boolean = false,
    @Assisted val folderName: String? = null,
    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val tryNodeSyncUseCase: TryNodeSyncUseCase,
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper,
    private val createFolderNodeUseCase: CreateFolderNodeUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase,
    private val removeDeviceFolderConnectionUseCase: RemoveDeviceFolderConnectionUseCase,
    private val monitorMegaPickerFolderNodesUseCase: MonitorMegaPickerFolderNodesUseCase,
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
                    // Skip sync/backup validation for stop backup flow - user is just picking a move destination
                    if (!isStopBackup) {
                        val folderUsageResult = runCatching {
                            state.value.currentFolder?.let { currentFolder ->
                                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                                    nodeId = currentFolder.id,
                                    shouldCheckCameraUploads = true,
                                    shouldExcludeCurrentDevice = false,
                                    useCache = false,
                                )
                            } ?: FolderUsageResult.NotUsed
                        }.getOrNull() ?: FolderUsageResult.NotUsed

                        val errorMessage = getFolderUsageMessage(folderUsageResult)

                        if (errorMessage != null) {
                            _state.update { it.copy(snackbarMessageId = errorMessage) }
                            return@launch
                        }
                    }

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
                                snackbarMessageId = errorMessage
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

            MegaPickerAction.SnackbarShown -> {
                _state.update {
                    it.copy(snackbarMessageId = null)
                }
            }

            is MegaPickerAction.DisabledFolderClicked -> {
                // Only show remove-connection dialog when folder has a removable backup (other device)
                if (action.node.backupId != null) {
                    _state.update {
                        it.copy(
                            showRemoveConnectionDialog = true,
                            selectedDisabledFolder = action.node
                        )
                    }
                }
            }

            MegaPickerAction.RemoveConnectionConfirmed -> {
                val backupId = state.value.selectedDisabledFolder?.backupId
                if (backupId != null) {
                    removeFolderConnection(backupId)
                } else {
                    Timber.w("RemoveConnectionConfirmed action received but backupId is null")
                    _state.update {
                        it.copy(
                            showRemoveConnectionDialog = false,
                            selectedDisabledFolder = null
                        )
                    }
                }
            }

            MegaPickerAction.RemoveConnectionDialogDismissed -> {
                _state.update {
                    it.copy(
                        showRemoveConnectionDialog = false,
                        selectedDisabledFolder = null
                    )
                }
            }
        }
    }

    private fun getFolderUsageMessage(folderUsageResult: FolderUsageResult): Int? {
        val errorMessage = when (folderUsageResult) {
            FolderUsageResult.NotUsed -> null
            FolderUsageResult.UsedByCameraUpload,
            FolderUsageResult.UsedByCameraUploadParent,
            FolderUsageResult.UsedByCameraUploadChild,
                -> sharedR.string.error_folder_part_of_camera_uploads

            FolderUsageResult.UsedByMediaUpload,
            FolderUsageResult.UsedByMediaUploadParent,
            FolderUsageResult.UsedByMediaUploadChild,
                -> sharedR.string.error_folder_part_of_media_uploads

            is FolderUsageResult.UsedBySyncOrBackup,
            is FolderUsageResult.UsedBySyncOrBackupParent,
            is FolderUsageResult.UsedBySyncOrBackupChild,
                -> sharedR.string.error_folder_part_of_sync_or_backup
        }
        return errorMessage
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

    private fun removeFolderConnection(backupId: Long) {
        viewModelScope.launch {
            runCatching {
                removeDeviceFolderConnectionUseCase(backupId)
            }.onSuccess {
                _state.update {
                    it.copy(
                        showRemoveConnectionDialog = false,
                        selectedDisabledFolder = null,
                        snackbarMessageId = sharedR.string.device_center_snackbar_message_connection_removed
                    )
                }
                state.value.currentFolder?.let(::fetchFolders)
            }.onFailure {
                Timber.e(it, "Failed to remove folder connection")
                _state.update { state ->
                    state.copy(
                        showRemoveConnectionDialog = false,
                        selectedDisabledFolder = null,
                        snackbarMessageId = sharedR.string.general_text_error
                    )
                }
            }
        }
    }

    private fun fetchFolders(currentFolder: Node) {
        getNodesFromFolderJob?.cancel()
        getNodesFromFolderJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            monitorMegaPickerFolderNodesUseCase(
                currentFolder,
                rootFolder?.id,
                isStopBackup,
                folderName,
            ).catch {
                Timber.e(it)
            }.onCompletion {
                _state.update { it.copy(isLoading = false) }
            }.collectLatest { result ->
                _state.update {
                    it.copy(
                        currentFolder = result.currentFolder,
                        nodes = result.nodes.map { node -> node.toTypedNodeUiModel() },
                        isSelectEnabled = result.isSelectEnabled,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun MegaPickerNodeInfo.toTypedNodeUiModel(): TypedNodeUiModel =
        TypedNodeUiModel(
            node = node,
            isDisabled = isDisabled,
            backupId = backupId,
            deviceName = deviceName,
        )

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
