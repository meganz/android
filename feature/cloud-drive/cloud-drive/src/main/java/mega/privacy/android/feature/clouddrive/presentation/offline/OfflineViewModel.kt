package mega.privacy.android.feature.clouddrive.presentation.offline

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
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineUiState
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the OfflineScreen
 */
@HiltViewModel
class OfflineViewModel @Inject constructor(
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
    private val parentStack = ArrayDeque<Pair<Int, String?>>()

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

    /**
     * Navigates to the specified path, if exists
     */
    fun navigateToPath(path: String, rootFolderOnly: Boolean, fileNames: Array<String>? = null) {
        if (path.isBlank() || path == File.separator) return
        isLoadingChildFolders(true)
        viewModelScope.launch {
            path.split(File.separator).filterNot { it.isBlank() }.forEach { child ->
                loadOfflineNodes()
                uiState.value.offlineNodes.find { it.offlineFileInformation.name == child }?.let {
                    openFolder(it, rootFolderOnly, false)
                } ?: return@forEach
            }
            loadOfflineNodes()
            highlightFiles(fileNames)
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
            getOfflineNodesByParentIdUseCase(uiState.value.parentId, uiState.value.searchQuery)
        }.onSuccess { offlineNodeList ->
            _uiState.update {
                it.copy(
                    isLoadingCurrentFolder = false,
                    offlineNodes = offlineNodeList.map { offlineFileInformation ->
                        OfflineNodeUiItem(
                            offlineFileInformation = offlineFileInformation,
                            isSelected = uiState.value.selectedNodeHandles.contains(
                                offlineFileInformation.handle.toLongOrNull()
                            ),
                            isHighlighted = false // TODO: Implement highlighting logic
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

    private fun highlightFiles(fileNamesToHighlight: Array<String>?) {
        _uiState.update {
            it.copy(
                offlineNodes = it.offlineNodes.map { item ->
                    item.copy(
                        isHighlighted = fileNamesToHighlight?.contains(item.offlineFileInformation.name) == true
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
    fun onItemClicked(offlineNodeUIItem: OfflineNodeUiItem, rootFolderOnly: Boolean) {
        if (uiState.value.selectedNodeHandles.isEmpty()) {
            if (offlineNodeUIItem.offlineFileInformation.isFolder) {
                openFolder(offlineNodeUIItem, rootFolderOnly, true)
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
        rootFolderOnly: Boolean,
        refreshNodesAsync: Boolean,
    ) {
        if (rootFolderOnly) {
            _uiState.update {
                it.copy(openFolderInPageEvent = triggered(offlineNodeUIItem.offlineFileInformation))
            }
        } else {
            Pair(
                offlineNodeUIItem.offlineFileInformation.parentId,
                uiState.value.title
            ).also {
                parentStack.remove(it)
                parentStack.add(it)
            }

            _uiState.update {
                it.copy(
                    title = offlineNodeUIItem.offlineFileInformation.name,
                    parentId = offlineNodeUIItem.offlineFileInformation.id,
                    closeSearchViewEvent = triggered
                )
            }
            if (refreshNodesAsync) refreshOfflineNodes()
        }
    }

    /**
     * On Close Search View Event Consumed
     */
    fun onCloseSearchViewEventConsumed() {
        _uiState.update { it.copy(closeSearchViewEvent = consumed) }
    }

    /**
     * On Open Offline Node Event Consumed
     */
    fun onOpenOfflineNodeEventConsumed() {
        _uiState.update { it.copy(openOfflineNodeEvent = consumed()) }
    }

    /**
     * OnBackClicked
     * @return [Int] 0 if no node was opened, and offline page should be exited
     */
    fun onBackClicked(): Int? {
        parentStack.removeLastOrNull()?.let { (parentId, parentTitle) ->
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
     * Update title
     */
    fun updateTitle(title: String?) {
        _uiState.update { it.copy(title = title) }
    }

    /**
     * Update default title
     */
    fun updateDefaultTitle(defaultTitle: String) {
        _uiState.update { it.copy(defaultTitle = defaultTitle) }
    }
}
