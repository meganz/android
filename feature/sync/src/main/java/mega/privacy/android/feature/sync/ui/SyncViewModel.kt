package mega.privacy.android.feature.sync.ui

import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.FolderPairState
import mega.privacy.android.feature.sync.domain.usecase.GetFolderPairs
import mega.privacy.android.feature.sync.domain.usecase.GetRemoteFolders
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairs
import mega.privacy.android.feature.sync.domain.usecase.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.SyncFolderPair
import javax.inject.Inject

/**
 * ViewModel for Sync feature
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val getRemoteFolders: GetRemoteFolders,
    private val syncFolderPair: SyncFolderPair,
    private val getFolderPairs: GetFolderPairs,
    private val removeFolderPairs: RemoveFolderPairs,
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val setSyncByWiFiUseCase: SetSyncByWiFiUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncState())

    /**
     * screen state
     */
    val state: StateFlow<SyncState> = _state.asStateFlow()

    init {
        fetchAllMegaFolders()
        fetchFirstFolderPair()
        observeSyncStatus()
    }

    private fun fetchAllMegaFolders() {
        viewModelScope.launch {
            runCatching { getRemoteFolders() }
                .onSuccess {
                    _state.value = _state.value.copy(rootMegaRemoteFolders = it)
                }
        }
    }

    private fun fetchFirstFolderPair() {
        viewModelScope.launch {
            runCatching { getFirstFolderPair() }
                .onSuccess { folderPair ->
                    _state.update {
                        it.copy(
                            selectedLocalFolder = folderPair.localFolderPath,
                            selectedMegaFolder = folderPair.remoteFolder,
                            status = folderPair.state
                        )
                    }
                }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            monitorSyncByWiFiUseCase().collectLatest { syncByWiFi ->
                _state.update {
                    it.copy(syncOnlyByWiFi = syncByWiFi)
                }
            }
        }
    }

    /**
     * handles actions/events dispatched from UI
     */
    fun handleAction(syncAction: SyncAction) {
        when (syncAction) {
            is SyncAction.SyncClicked -> {
                _state.value.selectedMegaFolder?.let { remoteFolder ->
                    viewModelScope.launch {
                        removeFolderPairs()
                        syncFolderPair(
                            _state.value.selectedLocalFolder,
                            remoteFolder
                        )
                    }
                }
            }

            is SyncAction.RemoteFolderSelected -> {
                _state.value = _state.value.copy(selectedMegaFolder = syncAction.remoteFolder)
            }

            is SyncAction.LocalFolderSelected -> {
                saveSelectedSyncPath(syncAction.path)
            }

            is SyncAction.RemoveFolderPairClicked -> {
                viewModelScope.launch {
                    removeFolderPairs()
                    _state.update {
                        it.copy(
                            selectedLocalFolder = "",
                            selectedMegaFolder = null,
                            status = FolderPairState.DISABLED
                        )
                    }
                }
            }

            is SyncAction.SyncByWiFiChecked -> {
                viewModelScope.launch {
                    setSyncByWiFiUseCase(syncAction.checked)
                }
            }
        }
    }

    // For POC, we are only syncing one folder
    private suspend fun getFirstFolderPair(): FolderPair =
        getFolderPairs().first()

    /**
     * Saves selected sync path with full path. It also converts relative path to absolute.
     */
    private fun saveSelectedSyncPath(path: Uri) {
        val fullPath = Environment.getExternalStorageDirectory().path +
                "/" + path.lastPathSegment?.split(":")?.last()
        _state.value = _state.value.copy(selectedLocalFolder = fullPath)
    }
}