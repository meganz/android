package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.SetOfflineSortOrder
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodesUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.sortorder.GetSortOrderByNodeSourceTypeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineUiState
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.OfflineNavKey
import timber.log.Timber
import java.io.File

/**
 * ViewModel for the OfflineScreen
 */
@HiltViewModel(assistedFactory = OfflineViewModel.Factory::class)
class OfflineViewModel @AssistedInject constructor(
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase,
    private val setOfflineWarningMessageVisibilityUseCase: SetOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineWarningMessageVisibilityUseCase: MonitorOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorViewType: MonitorViewType,
    private val setViewType: SetViewType,
    @Assisted private val navKey: OfflineNavKey,
    private val removeOfflineNodesUseCase: RemoveOfflineNodesUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val setOfflineSortOrder: SetOfflineSortOrder,
    private val getSortOrderByNodeSourceTypeUseCase: GetSortOrderByNodeSourceTypeUseCase,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        OfflineUiState(
            nodeId = navKey.nodeId,
            title = navKey.title,
            path = navKey.path,
            highlightedFiles = navKey
                .highlightedFiles
                ?.split(",")
                ?.toSet()
                ?: emptySet()
        )
    )

    /**
     * Flow of [OfflineUiState] UI State
     */
    val uiState = _uiState.asStateFlow()

    init {
        monitorConnectivity()
        monitorOfflineWarningMessage()
        monitorOfflineNodeUpdates()
        monitorViewTypeUpdate()
        getSortOrder()
        // Navigate to path if specified
        navigateToPath()
    }

    /**
     * Navigates to the specified path, if exists
     */
    fun navigateToPath() {
        val path = navKey.path
        if (path.isNullOrBlank() || path == File.separator) return
        isLoadingChildFolders(true)
        viewModelScope.launch {
            path
                .split(File.separator)
                .filterNot { it.isBlank() }
                .forEach { child ->
                    loadOfflineNodes()
                    uiState.value
                        .offlineNodes
                        .find { it.offlineFileInformation.name == child }
                        ?.let { openFolder(it, false) }
                        ?: return@forEach
                }
            loadOfflineNodes()
            highlightFiles()
            isLoadingChildFolders(false)
        }
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collectLatest { isOnline ->
                    _uiState.update { it.copy(isOnline = isOnline) }
                }
        }
    }

    private fun monitorViewTypeUpdate() {
        viewModelScope.launch {
            monitorViewType()
                .catch { Timber.e(it) }
                .collect { viewType ->
                    _uiState.update { it.copy(currentViewType = viewType) }
                }
        }
    }

    fun updateViewType() {
        viewModelScope.launch {
            val viewType = if (uiState.value.currentViewType == ViewType.LIST) {
                ViewType.GRID
            } else {
                ViewType.LIST
            }

            runCatching {
                setViewType(viewType)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorOfflineWarningMessage() {
        viewModelScope.launch {
            monitorOfflineWarningMessageVisibilityUseCase()
                .catch { Timber.e(it) }
                .collect { showWarning ->
                    _uiState.update { state -> state.copy(showOfflineWarning = showWarning) }
                }
        }
    }

    private fun monitorOfflineNodeUpdates() {
        viewModelScope.launch {
            monitorOfflineNodeUpdatesUseCase()
                .catch { Timber.e(it) }
                .conflate()
                .collect { loadOfflineNodes() }
        }
    }

    private fun refreshOfflineNodes() {
        viewModelScope.launch { loadOfflineNodes() }
    }

    private suspend fun loadOfflineNodes() {
        runCatching {
            getOfflineNodesByParentIdUseCase(uiState.value.nodeId, uiState.value.searchQuery)
        }.onSuccess { offlineNodeList ->
            _uiState.update {
                it.copy(
                    isLoadingCurrentFolder = false,
                    offlineNodes = offlineNodeList
                        .map { file ->
                            OfflineNodeUiItem(
                                offlineFileInformation = file,
                                isSelected = uiState.value.selectedNodeHandles.contains(
                                    file.handle.toLongOrNull()
                                ),
                                isHighlighted = file.name in uiState.value.highlightedFiles
                            )
                        }
                )
            }
        }.onFailure { throwable ->
            Timber.e(throwable)
            _uiState.update {
                it.copy(
                    isLoadingCurrentFolder = false,
                    offlineNodes = emptyList()
                )
            }
        }
    }

    private fun isLoadingChildFolders(isLoadingChildFolders: Boolean) = _uiState.update {
        it.copy(isLoadingChildFolders = isLoadingChildFolders)
    }

    private fun highlightFiles() {
        val fileNamesToHighlight = uiState.value.highlightedFiles
        _uiState.update {
            it.copy(
                offlineNodes = it.offlineNodes.map { item ->
                    item.copy(
                        isHighlighted = fileNamesToHighlight
                            .contains(item.offlineFileInformation.name)
                    )
                }
            )
        }
    }

    /**
     * Set search query
     */
    fun setSearchQuery(query: String?) {
        _uiState.update { it.copy(searchQuery = query) }
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
     */
    fun onItemClicked(offlineNodeUIItem: OfflineNodeUiItem) {
        if (uiState.value.selectedNodeHandles.isEmpty()) {
            if (offlineNodeUIItem.offlineFileInformation.isFolder) {
                openFolder(offlineNodeUIItem, true)
            } else {
                _uiState.update {
                    it.copy(openOfflineNodeEvent = triggered(offlineNodeUIItem.offlineFileInformation))
                }
            }
        } else {
            onLongItemClicked(offlineNodeUIItem)
        }
    }

    private fun openFolder(
        offlineNodeUIItem: OfflineNodeUiItem,
        refreshNodesAsync: Boolean,
    ) {
        _uiState.update {
            it.copy(openFolderInPageEvent = triggered(offlineNodeUIItem.offlineFileInformation))
        }

        if (refreshNodesAsync) refreshOfflineNodes()
    }

    /**
     * On Open Offline Node Event Consumed
     */
    fun onOpenOfflineNodeEventConsumed() {
        _uiState.update { it.copy(openOfflineNodeEvent = consumed()) }
    }

    /**
     * On Long item clicked
     */
    fun onLongItemClicked(offlineNodeUIItem: OfflineNodeUiItem) {
        val index = _uiState.value.offlineNodes.indexOfFirst {
            it.offlineFileInformation.handle == offlineNodeUIItem.offlineFileInformation.handle
        }
        updateNodeInSelectionState(offlineNodeUIItem = offlineNodeUIItem, index = index)
    }

    private fun updateNodeInSelectionState(
        offlineNodeUIItem: OfflineNodeUiItem,
        index: Int,
    ) {
        val updatedOfflineNodeUIItem =
            offlineNodeUIItem.copy(isSelected = !offlineNodeUIItem.isSelected)
        val newNodesList = _uiState.value.offlineNodes.toMutableList().apply {
            if (index >= 0) set(index, updatedOfflineNodeUIItem)
        }
        val selectedHandleList = newNodesList.filter { it.isSelected }
            .map { it.offlineFileInformation.handle.toLongOrNull() ?: -1L }

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
        val clearList = _uiState.value.offlineNodes.map { it.copy(isSelected = false) }
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
            val selectedList = _uiState.value.offlineNodes.map { it.copy(isSelected = true) }
            val selectedListHandles =
                selectedList.map { it.offlineFileInformation.handle.toLongOrNull() ?: -1L }
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
        _uiState.update { it.copy(openFolderInPageEvent = consumed()) }
    }

    /**
     * Remove offline nodes
     */
    fun removeOfflineNodes(handles: List<Long>) {
        viewModelScope.launch {
            if (handles.isEmpty()) return@launch
            val nodeHandles = handles.map { NodeId(it) }

            runCatching {
                removeOfflineNodesUseCase(nodeHandles)
            }.onFailure {
                Timber.e(it)
            }.onSuccess {
                // Todo waiting for snackbar message from content team
                snackbarEventQueue.queueMessage("${handles.size} items removed")
            }
        }
    }

    fun setSortOrder(sortOrder: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val sortOrder = nodeSortConfigurationUiMapper(sortOrder)
                setOfflineSortOrder(sortOrder)
            }.onFailure {
                Timber.e(it)
            }.onSuccess {
                getSortOrder()
            }
        }
    }

    private fun getSortOrder() {
        viewModelScope.launch {
            runCatching {
                getSortOrderByNodeSourceTypeUseCase(NodeSourceType.OFFLINE, true)
            }.onSuccess { sortOrder ->
                val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)
                _uiState.update {
                    it.copy(
                        selectedSortConfiguration = sortOrderPair,
                        selectedSortOrder = sortOrder
                    )
                }

                loadOfflineNodes()
            }.onFailure {
                Timber.e(it, "Failed to get offline sort order")
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: OfflineNavKey): OfflineViewModel
    }
}
