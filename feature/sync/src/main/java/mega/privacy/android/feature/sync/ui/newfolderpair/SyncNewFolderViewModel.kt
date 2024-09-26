package mega.privacy.android.feature.sync.ui.newfolderpair

import mega.privacy.android.shared.resources.R as sharedR
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.feature.sync.domain.usecase.GetLocalDCIMFolderPathUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SyncFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.ClearSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSelectedMegaFolderUseCase

@HiltViewModel(assistedFactory = SyncNewFolderViewModel.SyncNewFolderViewModelFactory::class)
internal class SyncNewFolderViewModel @AssistedInject constructor(
    @Assisted val syncType: SyncType,
    @Assisted val deviceName: String,
    private val getExternalPathByContentUriUseCase: GetExternalPathByContentUriUseCase,
    private val monitorSelectedMegaFolderUseCase: MonitorSelectedMegaFolderUseCase,
    private val syncFolderPairUseCase: SyncFolderPairUseCase,
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase,
    private val getLocalDCIMFolderPathUseCase: GetLocalDCIMFolderPathUseCase,
    private val clearSelectedMegaFolderUseCase: ClearSelectedMegaFolderUseCase,
) : ViewModel() {

    @AssistedFactory
    interface SyncNewFolderViewModelFactory {
        fun create(syncType: SyncType, deviceName: String): SyncNewFolderViewModel
    }

    private val _state =
        MutableStateFlow(SyncNewFolderState(syncType = syncType, deviceName = deviceName))
    val state: StateFlow<SyncNewFolderState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            clearSelectedMegaFolderUseCase()
            monitorSelectedMegaFolderUseCase().collectLatest { folder ->
                _state.update { state ->
                    state.copy(selectedMegaFolder = folder)
                }
            }
        }
    }

    fun handleAction(action: SyncNewFolderAction) {
        when (action) {
            is SyncNewFolderAction.LocalFolderSelected -> {
                viewModelScope.launch {
                    getExternalPathByContentUriUseCase(action.path.toString())?.let { path ->
                        val localDCIMFolderPath = getLocalDCIMFolderPathUseCase()
                        if (localDCIMFolderPath.isNotEmpty() && path.contains(localDCIMFolderPath)) {
                            _state.update { state ->
                                state.copy(showSnackbar = triggered(sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message))
                            }
                        } else {
                            _state.update { state ->
                                state.copy(selectedLocalFolder = path)
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
                            state.value.selectedMegaFolder?.let { remoteFolder ->
                                syncFolderPairUseCase(
                                    syncType = state.value.syncType,
                                    name = remoteFolder.name,
                                    localPath = state.value.selectedLocalFolder,
                                    remotePath = remoteFolder,
                                )
                            }
                            _state.update { state ->
                                state.copy(openSyncListScreen = triggered)
                            }
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

    /**
     * Consumes the event of showing snackbar.
     */
    fun onShowSnackbarConsumed() {
        _state.update { state -> state.copy(showSnackbar = consumed()) }
    }
}