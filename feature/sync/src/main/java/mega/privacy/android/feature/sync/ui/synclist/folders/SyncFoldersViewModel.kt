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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
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
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase.Companion.LOW_BATTERY_LEVEL
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RefreshSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
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
    private val getFolderTreeInfo: GetFolderTreeInfo,
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val moveDeconfiguredBackupNodesUseCase: MoveDeconfiguredBackupNodesUseCase,
    private val removeDeconfiguredBackupNodesUseCase: RemoveDeconfiguredBackupNodesUseCase,
    private val getCameraUploadsBackupUseCase: GetCameraUploadsBackupUseCase,
    private val getMediaUploadsBackupUseCase: GetMediaUploadsBackupUseCase,
    private val monitorCameraUploadsSettingsActionsUseCase: MonitorCameraUploadsSettingsActionsUseCase,
    private val monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase,
    private val backupInfoTypeIntMapper: BackupInfoTypeIntMapper,
    private val getPrimaryFolderNodeUseCase: GetPrimaryFolderNodeUseCase,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getSecondaryFolderNodeUseCase: GetSecondaryFolderNodeUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
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

        checkOverQuotaStatus()
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
    }

    private fun loadSyncs() {
        loadSyncsJob?.cancel()
        loadSyncsJob = viewModelScope.launch {
            combine(
                monitorSyncsUseCase().map(syncUiItemMapper::invoke),
                monitorCameraUploadsStatusInfoUseCase.invoke()
                    .onStart { emit(CameraUploadsStatusInfo.Unknown) },
            ) { syncs: List<SyncUiItem>, cuStatusInfo: CameraUploadsStatusInfo ->
                Pair(syncs, cuStatusInfo)
            }.catch { Timber.e(it) }
                .distinctUntilChanged()
                .collect { (syncs, cuStatusInfo) ->
                    val stalledIssues = monitorStalledIssuesUseCase().first()
                    var numOfFiles = 0
                    var numOfFolders = 0
                    var totalSizeInBytes = 0L
                    var creationTime = 0L
                    val syncsWithUpdatedDetails = syncs.map { sync ->

                        runCatching {
                            getNodeByIdUseCase(sync.megaStorageNodeId)
                        }.onSuccess { node ->
                            node?.let { folder ->
                                creationTime = folder.creationTime
                                runCatching {
                                    getFolderTreeInfo(folder as TypedFolderNode)
                                }.onSuccess { folderTreeInfo ->
                                    with(folderTreeInfo) {
                                        numOfFiles = numberOfFiles
                                        numOfFolders = numberOfFolders
                                        totalSizeInBytes = totalCurrentSizeInBytes
                                    }
                                }.onFailure {
                                    Timber.e(it)
                                }
                            }
                        }.onFailure {
                            Timber.e(it)
                        }

                        sync.copy(
                            hasStalledIssues = stalledIssues.any {
                                it.localPaths.firstOrNull()?.contains(sync.deviceStoragePath)
                                    ?: (it.nodeNames.first().contains(sync.megaStoragePath))
                            },
                            expanded = _uiState.value.syncUiItems.firstOrNull { it.id == sync.id }?.expanded == true,
                            numberOfFiles = numOfFiles,
                            numberOfFolders = numOfFolders,
                            totalSizeInBytes = totalSizeInBytes,
                            creationTime = creationTime,
                        )
                    }

                    val syncsList: MutableList<SyncUiItem> = emptyList<SyncUiItem>().toMutableList()
                    getCameraUploadsOrMediaUploadsSyncUiItem(
                        cameraUploadsOrMediaUploadsBackup = getCameraUploadsBackupUseCase(),
                        cuStatusInfo = cuStatusInfo,
                    )?.let { syncsList.add(it) }
                    getCameraUploadsOrMediaUploadsSyncUiItem(
                        cameraUploadsOrMediaUploadsBackup = getMediaUploadsBackupUseCase(),
                        cuStatusInfo = cuStatusInfo,
                    )?.let { syncsList.add(it) }
                    syncsList.addAll(syncsWithUpdatedDetails)
                    _uiState.update {
                        it.copy(
                            syncUiItems = syncsList,
                            isRefreshing = false,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private suspend fun getCameraUploadsOrMediaUploadsSyncUiItem(
        cameraUploadsOrMediaUploadsBackup: Backup?,
        cuStatusInfo: CameraUploadsStatusInfo,
    ): SyncUiItem? {
        var localPath = ""
        var megaStoragePath = ""
        var megaStorageNodeId = NodeId(-1L)
        var numOfFiles = 0
        var numOfFolders = 0
        var totalSizeInBytes = 0L
        var creationTime = 0L

        cameraUploadsOrMediaUploadsBackup?.let { backup ->
            runCatching {
                when (backupInfoTypeIntMapper(backup.backupType)) {
                    BackupInfoType.CAMERA_UPLOADS -> getPrimaryFolderNodeUseCase()
                    BackupInfoType.MEDIA_UPLOADS -> getSecondaryFolderNodeUseCase()
                    else -> getNodeByIdUseCase(backup.targetNode)
                }
            }.onSuccess { node ->
                node?.let { folder ->
                    localPath = when (backupInfoTypeIntMapper(backup.backupType)) {
                        BackupInfoType.CAMERA_UPLOADS -> getPrimaryFolderPathUseCase()
                        BackupInfoType.MEDIA_UPLOADS -> getSecondaryFolderPathUseCase()
                        else -> backup.localFolder
                    }
                    megaStorageNodeId = folder.id
                    megaStoragePath = getNodePathByIdUseCase(folder.id)
                    creationTime = folder.creationTime
                    runCatching {
                        getFolderTreeInfo(folder as TypedFolderNode)
                    }.onSuccess { folderTreeInfo ->
                        with(folderTreeInfo) {
                            numOfFiles = numberOfFiles
                            numOfFolders = numberOfFolders
                            totalSizeInBytes = totalCurrentSizeInBytes
                        }
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }.onFailure {
                Timber.e(it)
            }

            return syncUiItemMapper(cuBackup = backup, cuStatusInfo = cuStatusInfo).copy(
                hasStalledIssues = false,
                expanded = _uiState.value.syncUiItems.firstOrNull { it.id == backup.backupId }?.expanded == true,
                numberOfFiles = numOfFiles,
                numberOfFolders = numOfFolders,
                totalSizeInBytes = totalSizeInBytes,
                creationTime = creationTime,
                deviceStoragePath = localPath,
                megaStoragePath = megaStoragePath,
                megaStorageNodeId = megaStorageNodeId
            )
        }
        return null
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
                    uiState.value.syncUiItemToRemove?.let { syncUiItemToRemove ->
                        runCatching {
                            removeFolderPairUseCase(syncUiItemToRemove.id)
                        }.onSuccess {
                            syncUiItemToRemove.apply {
                                when (action.stopBackupOption) {
                                    StopBackupOption.MOVE -> {
                                        action.selectedFolder?.let { selectedFolder ->
                                            runCatching {
                                                moveDeconfiguredBackupNodesUseCase(
                                                    deconfiguredBackupRoot = megaStorageNodeId,
                                                    backupDestination = selectedFolder.id,
                                                )
                                            }.onFailure {
                                                Timber.e(it)
                                            }
                                        } ?: run {
                                            getRootNodeUseCase()?.let { rootNode ->
                                                runCatching {
                                                    moveDeconfiguredBackupNodesUseCase(
                                                        deconfiguredBackupRoot = megaStorageNodeId,
                                                        backupDestination = rootNode.id,
                                                    )
                                                }.onFailure {
                                                    Timber.e(it)
                                                }
                                            }
                                        }
                                    }

                                    StopBackupOption.DELETE -> {
                                        runCatching {
                                            removeDeconfiguredBackupNodesUseCase(
                                                deconfiguredBackupRoot = megaStorageNodeId,
                                            )
                                        }.onFailure {
                                            Timber.e(it)
                                        }
                                    }
                                }
                            }
                            _uiState.update { state -> state.copy(snackbarMessage = sharedResR.string.sync_snackbar_message_confirm_backup_stopped) }
                        }.onFailure {
                            Timber.e(it)
                        }
                    }
                }
                dismissConfirmRemoveSyncFolderDialog()
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
                    state.copy(snackbarMessage = null)
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
