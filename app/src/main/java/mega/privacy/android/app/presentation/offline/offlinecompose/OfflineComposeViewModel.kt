package mega.privacy.android.app.presentation.offline.offlinecompose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
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
    savedStateHandle: SavedStateHandle,
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase,
    private val setOfflineWarningMessageVisibilityUseCase: SetOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineWarningMessageVisibilityUseCase: MonitorOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorViewType: MonitorViewType,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OfflineUiState(
            parentId = savedStateHandle["parentId"] ?: -1,
            title = savedStateHandle["title"] ?: ""
        )
    )
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
            getOfflineNodesByParentIdUseCase(uiState.value.parentId, uiState.value.searchQuery)
        }.onSuccess { offlineNodeList ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    offlineNodes = offlineNodeList.map { item -> OfflineNodeUIItem(item) }
                )
            }
        }.onFailure { throwable ->
            Timber.e(throwable)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    offlineNodes = emptyList()
                )
            }
        }
    }

    /**
     * Set search query
     */
    fun setSearchQuery(query: String?) {
        _uiState.update {
            it.copy(searchQuery = query)
        }
        refreshOfflineNodes()
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
     *
     * @param offlineNodeUIItem [OfflineNodeUIItem]
     * @param rootFolderOnly [Boolean]
     */
    fun onItemClicked(offlineNodeUIItem: OfflineNodeUIItem, rootFolderOnly: Boolean) {
        if (uiState.value.selectedNodeHandles.isEmpty()) {
            if (offlineNodeUIItem.offlineNode.isFolder) {
                if (rootFolderOnly) {
                    _uiState.update {
                        it.copy(
                            openFolderInPageEvent = triggered(offlineNodeUIItem.offlineNode),
                        )
                    }
                } else {
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
                            closeSearchViewEvent = triggered
                        )
                    }
                    refreshOfflineNodes()
                }
            } else {
                _uiState.update {
                    it.copy(
                        openOfflineNodeEvent = triggered(offlineNodeUIItem.offlineNode)
                    )
                }
            }
        } else {
            onLongItemClicked(offlineNodeUIItem)
        }
    }

    /**
     * On Close Search View Event Consumed
     */
    fun onCloseSearchViewEventConsumed() {
        _uiState.update {
            it.copy(closeSearchViewEvent = consumed)
        }
    }

    /**
     * On Open Offline Node Event Consumed
     */
    fun onOpenOfflineNodeEventConsumed() {
        _uiState.update {
            it.copy(openOfflineNodeEvent = consumed())
        }
    }

    /**
     * OnBackClicked
     *
     * @return [Int] 0 if no node was opened, and offline page should be exited
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
            return 0
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

    /**
     * On Open Folder In Page Event Consumed
     */
    fun onOpenFolderInPageEventConsumed() {
        _uiState.update {
            it.copy(openFolderInPageEvent = consumed())
        }
    }
}
