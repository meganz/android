package mega.privacy.android.app.presentation.offline.offlinecompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUIState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.TransferFinishType
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.usecase.LoadOfflineNodesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersFinishedUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * OfflineComposeViewModel of [OfflineFragmentCompose]
 */
@HiltViewModel
class OfflineComposeViewModel @Inject constructor(
    private val loadOfflineNodesUseCase: LoadOfflineNodesUseCase,
    private val monitorTransfersFinishedUseCase: MonitorTransfersFinishedUseCase,
    private val setOfflineWarningMessageVisibilityUseCase: SetOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineWarningMessageVisibilityUseCase: MonitorOfflineWarningMessageVisibilityUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineUIState())

    /**
     * Flow of [OfflineUIState] UI State
     */
    val uiState = _uiState.asStateFlow()

    private var fetchOfflineNodesJob: Job? = null

    init {
        monitorOfflineWarningMessage()
        loadOfflineNodes()
        viewModelScope.launch {
            monitorTransfersFinishedUseCase().conflate().collect {
                /*
                The flow might not be collected because the transfer services behaviour isn't
                working as expected in the DownloadService. This issue is reported under TRAN-35.
                 */
                fetchOfflineNodesIfNeeded(it)
            }
        }
    }

    /**
     * Check on completed offline transfer and fetch the offline nodes accordingly
     * @param transfersFinishedState the state of the finished transfer
     */
    fun fetchOfflineNodesIfNeeded(transfersFinishedState: TransfersFinishedState) {
        val currentPath = uiState.value.currentPath

        if (transfersFinishedState.type == TransferFinishType.DOWNLOAD_OFFLINE
            && currentPath.contains(transfersFinishedState.nodeLocalPath ?: "")
        ) {
            loadOfflineNodes(currentPath)
        }
    }

    /**
     * Monitor the visibility of the offline warning message
     */
    fun monitorOfflineWarningMessage() {
        viewModelScope.launch {
            monitorOfflineWarningMessageVisibilityUseCase().collect {
                _uiState.update { state -> state.copy(showOfflineWarning = it) }
            }
        }
    }

    /**
     * Dismiss showing the Offline warning message
     */
    fun dismissOfflineWarning() {
        viewModelScope.launch {
            setOfflineWarningMessageVisibilityUseCase(isVisible = false)
        }
    }

    /**
     * map a list of [OfflineNodeInformation] to [OfflineNodeUIItem] to be used in the presentation layer
     */
    private fun getOfflineNodeUiItems(nodeList: List<OfflineNodeInformation>): List<OfflineNodeUIItem<OfflineNodeInformation>> {
        return if (nodeList.isNotEmpty()) {
            val offlineNodeList = _uiState.value.offlineNodes
            nodeList.map {
                val isSelected = _uiState.value.selectedNodeHandles.contains(it.handle)
                OfflineNodeUIItem(
                    offlineNode = it,
                    isSelected = isSelected,
                )
            }
        } else emptyList()
    }

    /**
     * get the offline nodes
     * @param path the provided path to fetch the offline nodes from
     * @param searchQuery the provided search query to load the offline nodes based on from database
     */
    fun loadOfflineNodes(path: String = Constants.OFFLINE_ROOT, searchQuery: String = "") {
        fetchOfflineNodesJob?.cancel()
        fetchOfflineNodesJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true)
            }
            runCatching {
                loadOfflineNodesUseCase(path = path, searchQuery = searchQuery)
            }.onSuccess {
                val offlineNodes = getOfflineNodeUiItems(it)
                _uiState.update { state ->
                    state.copy(offlineNodes = offlineNodes, isLoading = false)
                }
            }.onFailure {
                Timber.e(it, "Exception fetching offline nodes")
                _uiState.update { state ->
                    state.copy(offlineNodes = emptyList(), isLoading = false)
                }
            }
        }
    }
}