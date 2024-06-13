package mega.privacy.android.app.presentation.offline.offlinecompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUiState
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersFinishedUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.shared.original.core.ui.utils.pop
import mega.privacy.android.shared.original.core.ui.utils.push
import timber.log.Timber
import javax.inject.Inject

/**
 * OfflineComposeViewModel of [OfflineComposeFragment]
 */
@HiltViewModel
class OfflineComposeViewModel @Inject constructor(
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase,
    private val monitorTransfersFinishedUseCase: MonitorTransfersFinishedUseCase,
    private val setOfflineWarningMessageVisibilityUseCase: SetOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineWarningMessageVisibilityUseCase: MonitorOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorViewType: MonitorViewType,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineUiState())
    private val parentStack = ArrayDeque<Pair<Int, String>>()

    /**
     * Flow of [OfflineUiState] UI State
     */
    val uiState = _uiState.asStateFlow()

    init {
        monitorConnectivity()
        monitorOfflineWarningMessage()
        monitorOfflineNodeUpdates()
        monitorViewTypeUpdate()
        viewModelScope.launch {
            monitorTransfersFinishedUseCase()
                .catch {
                    Timber.e(it)
                }
                .conflate()
                .collect {
                    loadOfflineNodes()
                }
        }
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collectLatest { isOnline -> _uiState.update { it.copy(isOnline = isOnline) } }
        }
    }

    private fun monitorViewTypeUpdate() {
        viewModelScope.launch {
            monitorViewType()
                .catch {
                    Timber.e(it)
                }
                .collect { viewType ->
                    _uiState.update {
                        it.copy(
                            currentViewType = viewType
                        )
                    }
                }
        }
    }

    private fun monitorOfflineWarningMessage() {
        viewModelScope.launch {
            monitorOfflineWarningMessageVisibilityUseCase()
                .catch {
                    Timber.e(it)
                }
                .collect {
                    _uiState.update { state -> state.copy(showOfflineWarning = it) }
                }
        }
    }

    private fun monitorOfflineNodeUpdates() {
        viewModelScope.launch {
            monitorOfflineNodeUpdatesUseCase()
                .catch {
                    Timber.e(it)
                }
                .conflate()
                .collect {
                    loadOfflineNodes()
                }
        }
    }


    private fun refreshOfflineNodes() {
        viewModelScope.launch {
            loadOfflineNodes()
        }
    }

    private suspend fun loadOfflineNodes() {
        runCatching {
            getOfflineNodesByParentIdUseCase(uiState.value.parentId)
        }.onSuccess { offlineNodeList ->
            _uiState.update {
                it.copy(
                    offlineNodes = offlineNodeList.map { item -> OfflineNodeUIItem(item) }
                )
            }
        }.onFailure {
            Timber.e(it)
            _uiState.update {
                it.copy(offlineNodes = emptyList())
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
     * Handle on clicked of item
     */
    fun onItemClicked(offlineNodeUIItem: OfflineNodeUIItem) {
        if (uiState.value.selectedNodeHandles.isEmpty()) {
            if (offlineNodeUIItem.offlineNode.isFolder) {
                parentStack.push(
                    Pair(
                        offlineNodeUIItem.offlineNode.parentId,
                        uiState.value.title
                    )
                )
                _uiState.update {
                    it.copy(
                        title = offlineNodeUIItem.offlineNode.name,
                        parentId = offlineNodeUIItem.offlineNode.id,
                    )
                }
                refreshOfflineNodes()
            }
        } else {
            onLongItemClicked(offlineNodeUIItem)
        }
    }

    /**
     * OnBackClicked
     */
    fun onBackClicked(): Int? {
        parentStack.pop()?.let { (parentId, parentTitle) ->
            _uiState.update {
                it.copy(
                    parentId = parentId,
                    title = parentTitle
                )
            }
            refreshOfflineNodes()
            return null
        } ?: run {
            return -1
        }
    }

    /**
     * On Long item clicked
     * @param offlineNodeUIItem [OfflineNodeUIItem]
     */
    fun onLongItemClicked(offlineNodeUIItem: OfflineNodeUIItem) {
        val index =
            _uiState.value.offlineNodes.indexOfFirst { it.offlineNode.handle == offlineNodeUIItem.offlineNode.handle }
        updateNodeInSelectionState(offlineNodeUIItem = offlineNodeUIItem, index = index)
    }

    private fun updateNodeInSelectionState(offlineNodeUIItem: OfflineNodeUIItem, index: Int) {
        offlineNodeUIItem.isSelected = !offlineNodeUIItem.isSelected
        val newNodesList =
            _uiState.value.offlineNodes.updateItemAt(index = index, item = offlineNodeUIItem)
        val selectedHandleList = newNodesList.filter {
            it.isSelected
        }.map {
            it.offlineNode.handle.toLong()
        }
        _uiState.update {
            it.copy(
                offlineNodes = newNodesList,
                selectedNodeHandles = selectedHandleList,
            )
        }
    }

    /**
     * Clear Selected nodes
     */
    fun clearSelection() {
        val clearList = _uiState.value.offlineNodes.map {
            it.copy(isSelected = false)
        }
        _uiState.update {
            it.copy(
                offlineNodes = clearList,
                selectedNodeHandles = emptyList()
            )
        }
    }

    /**
     * Select All nodes
     */
    fun selectAll() {
        viewModelScope.launch {
            val selectedList = _uiState.value.offlineNodes.map {
                it.copy(isSelected = true)
            }
            val selectedListHandles = selectedList.map { it.offlineNode.handle.toLong() }
            _uiState.update {
                it.copy(
                    offlineNodes = selectedList,
                    selectedNodeHandles = selectedListHandles
                )
            }
        }
    }
}
