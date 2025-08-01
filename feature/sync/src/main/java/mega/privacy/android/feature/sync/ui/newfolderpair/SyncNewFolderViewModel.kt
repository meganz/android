package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.repository.BackupRepository.Companion.BACKUPS_FOLDER_DEFAULT_NAME
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceNameUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.exception.BackupAlreadyExistsException
import mega.privacy.android.feature.sync.domain.usecase.backup.MyBackupsFolderExistsUseCase
import mega.privacy.android.feature.sync.domain.usecase.backup.SetMyBackupsFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SyncFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.ClearSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUriValidityMapper
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUriValidityResult
import timber.log.Timber

@HiltViewModel(assistedFactory = SyncNewFolderViewModel.SyncNewFolderViewModelFactory::class)
internal class SyncNewFolderViewModel @AssistedInject constructor(
    @Assisted val syncType: SyncType,
    @Assisted val remoteFolderHandle: Long?,
    @Assisted val remoteFolderName: String?,
    private val monitorSelectedMegaFolderUseCase: MonitorSelectedMegaFolderUseCase,
    private val syncFolderPairUseCase: SyncFolderPairUseCase,
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase,
    private val clearSelectedMegaFolderUseCase: ClearSelectedMegaFolderUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
    private val getDeviceNameUseCase: GetDeviceNameUseCase,
    private val myBackupsFolderExistsUseCase: MyBackupsFolderExistsUseCase,
    private val setMyBackupsFolderUseCase: SetMyBackupsFolderUseCase,
    private val syncUriValidityMapper: SyncUriValidityMapper,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) : ViewModel() {

    @AssistedFactory
    interface SyncNewFolderViewModelFactory {
        fun create(
            syncType: SyncType,
            remoteFolderHandle: Long?,
            remoteFolderName: String?,
        ): SyncNewFolderViewModel
    }

    private val _state =
        MutableStateFlow(SyncNewFolderState(syncType = syncType))
    val state: StateFlow<SyncNewFolderState> = _state.asStateFlow()

    init {
        if (syncType == SyncType.TYPE_BACKUP) {
            getDeviceName()
        }
        viewModelScope.launch {
            clearSelectedMegaFolderUseCase()
            monitorSelectedMegaFolderUseCase().collectLatest { folder ->
                _state.update { state ->
                    state.copy(selectedMegaFolder = folder)
                }
            }
        }
        if (remoteFolderHandle != null && remoteFolderName != null) {
            _state.update { state ->
                state.copy(
                    selectedMegaFolder = RemoteFolder(
                        NodeId(remoteFolderHandle),
                        remoteFolderName
                    )
                )
            }
        }

        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.e(it) }
                .collect {
                    checkOverQuotaStatus()
                }
        }
    }

    private fun getDeviceName() {
        viewModelScope.launch {
            runCatching {
                getDeviceIdUseCase()?.let { deviceId ->
                    val deviceName = getDeviceNameUseCase(deviceId).orEmpty()
                    _state.update { it.copy(deviceName = deviceName) }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun checkOverQuotaStatus() {
        viewModelScope.launch {
            runCatching {
                isStorageOverQuotaUseCase()
            }.onSuccess { isStorageOverQuota ->
                _state.update {
                    it.copy(
                        isStorageOverQuota = isStorageOverQuota,
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun handleAction(action: SyncNewFolderAction) {
        when (action) {
            is SyncNewFolderAction.LocalFolderSelected -> {
                viewModelScope.launch {
                    val documentFile = action.documentFile
                    val validityResult = syncUriValidityMapper(documentFile.uri.toString())
                    when (validityResult) {
                        is SyncUriValidityResult.ShowSnackbar -> {
                            _state.update { state ->
                                state.copy(showSnackbar = triggered(validityResult.messageResId))
                            }
                        }

                        is SyncUriValidityResult.ValidFolderSelected -> {
                            _state.update { state ->
                                state.copy(
                                    selectedLocalFolder = validityResult.localFolderUri.value,
                                    selectedFolderName = validityResult.folderName
                                )
                            }
                        }

                        SyncUriValidityResult.Invalid -> {
                            Timber.d("Invalid folder selected")
                            _state.update { state ->
                                state.copy(
                                    selectedLocalFolder = "",
                                    selectedFolderName = ""
                                )
                            }
                        }
                    }
                }
            }

            is SyncNewFolderAction.NextClicked -> {
                viewModelScope.launch {
                    when {
                        isStorageOverQuotaUseCase() -> {
                            _state.update { state ->
                                state.copy(showStorageOverQuota = true)
                            }
                        }

                        else -> {
                            when (state.value.syncType) {
                                SyncType.TYPE_BACKUP -> {
                                    if (myBackupsFolderExistsUseCase().not()) {
                                        runCatching {
                                            setMyBackupsFolderUseCase(BACKUPS_FOLDER_DEFAULT_NAME)
                                        }.onSuccess {
                                            if (createBackup().not()) return@launch
                                        }.onFailure {
                                            Timber.e(it)
                                        }
                                    } else {
                                        if (createBackup().not()) return@launch
                                    }
                                }

                                else -> {
                                    state.value.selectedMegaFolder?.let { remoteFolder ->
                                        syncFolderPairUseCase(
                                            syncType = state.value.syncType,
                                            name = remoteFolder.name,
                                            localPath = state.value.selectedLocalFolder,
                                            remotePath = remoteFolder,
                                        )
                                    }
                                }
                            }
                            openSyncListScreen()
                        }
                    }
                }
            }

            is SyncNewFolderAction.StorageOverquotaShown -> {
                _state.update { state ->
                    state.copy(showStorageOverQuota = false)
                }
            }

            SyncNewFolderAction.SyncListScreenOpened -> {
                _state.update { state ->
                    state.copy(openSyncListScreen = consumed)
                }
            }
        }
    }

    private suspend fun createBackup(): Boolean {
        runCatching {
            syncFolderPairUseCase(
                syncType = state.value.syncType,
                name = null,
                localPath = state.value.selectedLocalFolder,
                remotePath = RemoteFolder(NodeId(-1L), ""),
            )
        }.onSuccess { result ->
            onShowRenameAndCreateBackupDialogConsumed()
            return result != false
        }.onFailure { exception ->
            if (exception is BackupAlreadyExistsException) {
                _state.update { state ->
                    state.copy(
                        showRenameAndCreateBackupDialog = _state.value.selectedFolderName
                    )
                }
            } else {
                Timber.e(exception)
            }
        }
        return false
    }

    /**
     * Consumes the event of showing snackbar.
     */
    fun onShowSnackbarConsumed() {
        _state.update { state -> state.copy(showSnackbar = consumed()) }
    }

    /**
     * Consumes the event of showing rename and create backup dialog.
     */
    fun onShowRenameAndCreateBackupDialogConsumed() {
        _state.update { state -> state.copy(showRenameAndCreateBackupDialog = null) }
    }

    /**
     * Triggers the event to open sync list screen
     */
    fun openSyncListScreen() {
        _state.update { state -> state.copy(openSyncListScreen = triggered) }
    }
}
