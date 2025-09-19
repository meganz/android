package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.exception.ResourceAlreadyExistsMegaException
import mega.privacy.android.domain.usecase.GetCompleteFolderInfoUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNodePathByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadsBackupUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetMediaUploadsBackupUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsSettingsActionsUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.environment.GetBatteryInfoUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.MoveDeconfiguredBackupNodesUseCase
import mega.privacy.android.domain.usecase.node.RemoveDeconfiguredBackupNodesUseCase
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.ChangeSyncLocalRootUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RefreshSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.ClearSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase.Companion.LOW_BATTERY_LEVEL
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.model.StopBackupOption
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncFoldersViewModel @Inject constructor(
    private val syncUiItemMapper: SyncUiItemMapper,
    private val removeFolderPairUseCase: RemoveFolderPairUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
    private val monitorStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val setUserPausedSyncsUseCase: SetUserPausedSyncUseCase,
    private val refreshSyncUseCase: RefreshSyncUseCase,
    private val getBatteryInfoUseCase: GetBatteryInfoUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getNodePathByIdUseCase: GetNodePathByIdUseCase,
    private val getCompleteFolderInfoUseCase: GetCompleteFolderInfoUseCase,
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val moveDeconfiguredBackupNodesUseCase: MoveDeconfiguredBackupNodesUseCase,
    private val removeDeconfiguredBackupNodesUseCase: RemoveDeconfiguredBackupNodesUseCase,
    private val getCameraUploadsBackupUseCase: GetCameraUploadsBackupUseCase,
    private val getMediaUploadsBackupUseCase: GetMediaUploadsBackupUseCase,
    private val monitorCameraUploadsSettingsActionsUseCase: MonitorCameraUploadsSettingsActionsUseCase,
    private val monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase,
    private val getPrimaryFolderNodeUseCase: GetPrimaryFolderNodeUseCase,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getSecondaryFolderNodeUseCase: GetSecondaryFolderNodeUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val changeSyncLocalRootUseCase: ChangeSyncLocalRootUseCase,
    private val monitorSelectedMegaFolderUseCase: MonitorSelectedMegaFolderUseCase,
    private val clearSelectedMegaFolderUseCase: ClearSelectedMegaFolderUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncFoldersUiState(emptyList()))
    val uiState: StateFlow<SyncFoldersUiState> = _uiState.asStateFlow()

    private var loadSyncsJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching {
                _uiState.update { state -> state.copy(isLoading = true) }
                refreshSyncUseCase()
            }.onSuccess {
                loadSyncs()
            }.onFailure {
                _uiState.update { state -> state.copy(isLoading = false) }
                Timber.e(it)
            }
        }

        getAndMonitorBatteryInfo()

        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.e(it) }
                .collect {
                    checkOverQuotaStatus()
                    onSyncRefresh()
                }
        }

        viewModelScope.launch {
            monitorCameraUploadsSettingsActionsUseCase()
                .catch { Timber.e(it) }
                .collect { loadSyncs() }
        }

        viewModelScope.launch {
            monitorNodeUpdatesUseCase()
                .catch { Timber.e(it) }
                .collect { updatedNodes ->
                    val handles =
                        setOf(getPrimarySyncHandleUseCase(), getSecondarySyncHandleUseCase())
                    updatedNodes.changes.keys.find {
                        handles.contains(it.id.longValue)
                    }?.run { loadSyncs() }
                }
        }

        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collect {
                    onSyncRefresh()
                }
        }

        viewModelScope.launch {
            runCatching {
                val value = getFeatureFlagValueUseCase(SyncFeatures.DisableBatteryOptimization)
                _uiState.update { state ->
                    state.copy(
                        isDisableBatteryOptimizationEnabled = value
                    )
                }
            }
        }

        viewModelScope.launch {
            monitorSelectedMegaFolderUseCase().catch {
                Timber.e(it)
            }.distinctUntilChanged().collect {
                if (it != null) {
                    Timber.d("Selected mega folder: $it")
                    handleAction(
                        SyncFoldersAction.OnRemoveBackupFolderDialogConfirmed(
                            StopBackupOption.MOVE,
                            it
                        )
                    )
                }
            }
        }
    }

    private fun loadSyncs() {
        loadSyncsJob?.cancel()
        loadSyncsJob = combine(
            flow = processSyncsFlow(),
            flow2 = processCameraUploadsFlow()
        ) { syncs, cameraUploadsBackup ->
            _uiState.update {
                it.copy(
                    syncUiItems = cameraUploadsBackup + syncs.first,
                    isRefreshing = false,
                    isLoading = false,
                    stalledIssueCount = syncs.second
                )
            }
        }.catch { Timber.e(it) }.launchIn(viewModelScope)
    }

    private fun processSyncsFlow() = combine(
        monitorSyncsUseCase()
            .map(syncUiItemMapper::invoke)
            .distinctUntilChanged(),
        monitorStalledIssuesUseCase().distinctUntilChanged()
    ) { syncs, stalledIssues ->
        val syncUiItems = syncs.map { sync ->
            val folderInfo = getCompleteFolderInfoUseCase(sync.megaStorageNodeId)
            val hasStalledIssue = stalledIssues.any { it.syncId == sync.id }
            sync.copy(
                hasStalledIssues = hasStalledIssue,
                expanded = _uiState.value.syncUiItems.firstOrNull { it.id == sync.id }?.expanded == true,
                numberOfFiles = folderInfo?.numOfFiles ?: 0,
                numberOfFolders = folderInfo?.numOfFolders ?: 0,
                totalSizeInBytes = folderInfo?.totalSizeInBytes ?: 0L,
                creationTime = folderInfo?.creationTime ?: 0L,
            )
        }
        syncUiItems to stalledIssues.size
    }.catch {
        Timber.e(it)
    }


    private fun processCameraUploadsFlow() = monitorCameraUploadsStatusInfoUseCase()
        .onStart { emit(CameraUploadsStatusInfo.Unknown) }
        .distinctUntilChanged()
        .map { cuStatusInfo ->
            buildList {
                getCameraUploadsOrMediaUploadsSyncUiItem(
                    cameraUploadsOrMediaUploadsBackup = getCameraUploadsBackupUseCase(),
                    cuStatusInfo = cuStatusInfo,
                )?.let { add(it) }
                getCameraUploadsOrMediaUploadsSyncUiItem(
                    cameraUploadsOrMediaUploadsBackup = getMediaUploadsBackupUseCase(),
                    cuStatusInfo = cuStatusInfo,
                )?.let { add(it) }
            }
        }.catch { Timber.e(it) }

    private suspend fun getCameraUploadsOrMediaUploadsSyncUiItem(
        cameraUploadsOrMediaUploadsBackup: Backup?,
        cuStatusInfo: CameraUploadsStatusInfo,
    ): SyncUiItem? {
        return cameraUploadsOrMediaUploadsBackup?.let { backup ->
            runCatching {
                val node = when (backup.backupInfoType) {
                    BackupInfoType.CAMERA_UPLOADS -> getPrimaryFolderNodeUseCase()
                    BackupInfoType.MEDIA_UPLOADS -> getSecondaryFolderNodeUseCase()
                    else -> getNodeByIdUseCase(backup.targetNode)
                } ?: return null
                val localPath = when (backup.backupInfoType) {
                    BackupInfoType.CAMERA_UPLOADS -> getPrimaryFolderPathUseCase()
                    BackupInfoType.MEDIA_UPLOADS -> getSecondaryFolderPathUseCase()
                    else -> backup.localFolder
                }

                val megaStoragePath = getNodePathByIdUseCase(node.id)
                val folderInfo = getCompleteFolderInfoUseCase(node.id)
                val expanded =
                    _uiState.value.syncUiItems.firstOrNull { it.id == backup.backupId }?.expanded == true

                syncUiItemMapper(cuBackup = backup, cuStatusInfo = cuStatusInfo).copy(
                    hasStalledIssues = false,
                    expanded = expanded,
                    numberOfFiles = folderInfo?.numOfFiles ?: 0,
                    numberOfFolders = folderInfo?.numOfFolders ?: 0,
                    totalSizeInBytes = folderInfo?.totalSizeInBytes ?: 0L,
                    creationTime = folderInfo?.creationTime ?: 0L,
                    deviceStoragePath = localPath,
                    megaStoragePath = megaStoragePath,
                    megaStorageNodeId = node.id
                )
            }.getOrNull()
        }
    }

    private fun checkOverQuotaStatus() {
        viewModelScope.launch {
            runCatching {
                isStorageOverQuotaUseCase()
            }.onSuccess { isStorageOverQuota ->
                _uiState.update {
                    it.copy(
                        isStorageOverQuota = isStorageOverQuota,
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getAndMonitorBatteryInfo() {
        viewModelScope.launch {
            monitorBatteryInfoUseCase()
                .onStart { emit(getBatteryInfoUseCase()) }
                .catch { Timber.e(it) }
                .collect { batteryInfo ->
                    _uiState.update { state ->
                        state.copy(isLowBatteryLevel = batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging)
                    }
                }
        }
    }

    fun handleAction(action: SyncFoldersAction) {
        when (action) {
            is SyncFoldersAction.CardExpanded -> {
                val syncUiItem = action.syncUiItem
                val expanded = action.expanded

                _uiState.update { state ->
                    state.copy(syncUiItems = _uiState.value.syncUiItems.map {
                        if (it.id == syncUiItem.id) {
                            it.copy(expanded = expanded)
                        } else {
                            it
                        }
                    })
                }
            }

            is SyncFoldersAction.RemoveFolderClicked -> {
                Timber.d("Remove folder clicked: ${action.syncUiItem}")
                _uiState.update { state ->
                    state.copy(
                        showConfirmRemoveSyncFolderDialog = true,
                        syncUiItemToRemove = action.syncUiItem
                    )
                }
            }

            is SyncFoldersAction.OnRemoveSyncFolderDialogConfirmed -> {
                viewModelScope.launch {
                    uiState.value.syncUiItemToRemove?.let { syncUiItemToRemove ->
                        runCatching {
                            removeFolderPairUseCase(syncUiItemToRemove.id)
                        }.onSuccess {
                            _uiState.update { state -> state.copy(snackbarMessage = sharedResR.string.sync_snackbar_message_confirm_sync_stopped) }
                        }.onFailure {
                            Timber.e(it)
                        }
                    }
                }
                dismissConfirmRemoveSyncFolderDialog()
            }

            is SyncFoldersAction.OnRemoveBackupFolderDialogConfirmed -> {
                viewModelScope.launch {
                    Timber.d("Sync Ui Item to remove: ${uiState.value.syncUiItemToRemove}")
                    uiState.value.syncUiItemToRemove?.let { syncUiItemToRemove ->
                        syncUiItemToRemove.apply {
                            runCatching {
                                when (action.stopBackupOption) {
                                    StopBackupOption.MOVE -> {
                                        val destinationFolderId =
                                            action.selectedFolder?.id ?: getRootNodeUseCase()?.id
                                        val destinationFolderName = action.selectedFolder?.name
                                            ?: getRootNodeUseCase()?.name
                                        destinationFolderId?.let { folderId ->
                                            Timber.d("Moving deconfigured backup nodes to folder with id: $folderId")
                                            moveDeconfiguredBackupNodesUseCase(
                                                deconfiguredBackupRoot = megaStorageNodeId,
                                                backupDestination = folderId,
                                            )
                                            removeFolderPairUseCase(syncUiItemToRemove.id)
                                            clearSelectedMegaFolderUseCase()
                                            _uiState.update { state ->
                                                state.copy(
                                                    snackbarMessage = sharedResR.string.sync_snackbar_message_confirm_backup_moved,
                                                    showConfirmRemoveSyncFolderDialog = false,
                                                    syncUiItemToRemove = null,
                                                    movedFolderName = destinationFolderName ?: ""
                                                )
                                            }
                                        }
                                    }

                                    StopBackupOption.DELETE -> {
                                        removeDeconfiguredBackupNodesUseCase(
                                            deconfiguredBackupRoot = megaStorageNodeId,
                                        )
                                        removeFolderPairUseCase(syncUiItemToRemove.id)
                                        clearSelectedMegaFolderUseCase()
                                        _uiState.update { state ->
                                            state.copy(
                                                snackbarMessage = sharedResR.string.sync_snackbar_message_confirm_backup_deleted,
                                                showConfirmRemoveSyncFolderDialog = false,
                                                syncUiItemToRemove = null
                                            )
                                        }
                                    }
                                }
                            }.onFailure {
                                if (it is ResourceAlreadyExistsMegaException) {
                                    _uiState.update { state ->
                                        state.copy(
                                            snackbarMessage = sharedResR.string.create_new_folder_dialog_error_existing_folder,
                                            showConfirmRemoveSyncFolderDialog = false,
                                            syncUiItemToRemove = null
                                        )
                                    }
                                } else {
                                    removeFolderPairUseCase(syncUiItemToRemove.id)
                                    clearSelectedMegaFolderUseCase()
                                    _uiState.update { state ->
                                        state.copy(
                                            snackbarMessage = sharedResR.string.sync_snackbar_message_confirm_backup_stopped,
                                            showConfirmRemoveSyncFolderDialog = false,
                                            syncUiItemToRemove = null
                                        )
                                    }
                                    Timber.e(it)
                                }
                            }
                        }
                    }
                }
            }

            is SyncFoldersAction.OnRemoveFolderDialogDismissed -> {
                dismissConfirmRemoveSyncFolderDialog()
            }

            is SyncFoldersAction.PauseRunClicked -> {
                viewModelScope.launch {
                    runCatching {
                        if (action.syncUiItem.status != SyncStatus.PAUSED) {
                            pauseSyncUseCase(action.syncUiItem.id)
                            setUserPausedSyncsUseCase(action.syncUiItem.id, true)
                        } else {
                            resumeSyncUseCase(action.syncUiItem.id)
                            setUserPausedSyncsUseCase(action.syncUiItem.id, false)
                        }
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }

            is SyncFoldersAction.SnackBarShown -> {
                _uiState.update { state ->
                    state.copy(snackbarMessage = null, movedFolderName = null)
                }
            }

            is SyncFoldersAction.LocalFolderSelected -> {
                viewModelScope.launch {
                    runCatching {
                        changeSyncLocalRootUseCase(
                            folderPairId = action.syncUiItem.id,
                            newLocalPath = action.uri.toString(),
                        )
                        resumeSyncUseCase(action.syncUiItem.id)
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }

        }
    }

    private fun dismissConfirmRemoveSyncFolderDialog() {
        _uiState.update {
            it.copy(
                showConfirmRemoveSyncFolderDialog = false,
                syncUiItemToRemove = null
            )
        }
    }

    fun onSyncRefresh() {
        viewModelScope.launch {
            refreshSyncUseCase()
        }
    }
}
