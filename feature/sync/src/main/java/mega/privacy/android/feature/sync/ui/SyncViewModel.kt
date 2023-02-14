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
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.GetRemoteFolders
import mega.privacy.android.feature.sync.domain.usecase.GetSyncLocalPath
import mega.privacy.android.feature.sync.domain.usecase.SetSyncLocalPath
import javax.inject.Inject

/**
 * ViewModel for Sync feature
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val getSyncLocalPath: GetSyncLocalPath,
    private val getRemoteFolders: GetRemoteFolders,
    private val setSyncLocalPath: SetSyncLocalPath,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncState())

    /**
     * screen state
     */
    val state: StateFlow<SyncState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getSyncLocalPath().collectLatest {
                _state.value = _state.value.copy(selectedLocalFolder = it)
            }
        }

        viewModelScope.launch {
            runCatching { getRemoteFolders() }
                .onSuccess {
                    _state.value = _state.value.copy(rootMegaRemoteFolders = it)
                }
        }
    }

    /**
     * handles actions/events dispatched from UI
     */
    fun handleAction(syncAction: SyncAction) {
        when (syncAction) {
            SyncAction.SyncClicked -> {
                _state.value = _state.value.copy(isSyncing = true)
                // This will contain code that syncs local folder with MEGA
            }

            is SyncAction.RemoteFolderSelected -> {
                _state.value = _state.value.copy(selectedMegaFolder = syncAction.remoteFolder)
            }

            is SyncAction.AutoSyncChecked -> {
                // This will contain code that will auto sync local folder with MEGA on changes
            }

            is SyncAction.LocalFolderSelected -> {
                saveSelectedSyncPath(syncAction.path)
            }
        }
    }

    /**
     * Saves selected sync path with full path. It also converts relative path to absolute.
     */
    private fun saveSelectedSyncPath(path: Uri) {
        val fullPath = Environment.getExternalStorageDirectory().path +
                "/" + path.lastPathSegment?.split(":")?.last()
        setSyncLocalPath(fullPath)
    }
}