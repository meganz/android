package mega.privacy.android.app.presentation.fileinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * View Model class for [mega.privacy.android.app.presentation.fileinfo.FileInfoActivity]
 */
@HiltViewModel
class FileInfoViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val monitorConnectivity: MonitorConnectivity,
    private val getFileHistoryNumVersions: GetFileHistoryNumVersions,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileInfoViewState())

    /**
     * the state of the view
     */
    val uiState = _uiState.asStateFlow()

    /**
     * the node whose information are displayed
     */
    lateinit var node: MegaNode
        private set

    /**
     * sets the node and updates its state
     */
    fun updateNode(node: MegaNode) {
        this.node = node
        updateHistoryNumVersions()
    }

    /**
     * Get latest value of [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEvent.getState()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * checks the history num versions an updates the view state accordingly
     */
    fun updateHistoryNumVersions() = viewModelScope.launch {
        _uiState.update {
            it.copy(historyVersions = getFileHistoryNumVersions(node.handle))
        }
    }
}