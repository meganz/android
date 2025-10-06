package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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
import mega.privacy.android.navigation.destination.OfflineNavKey
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
    private val args = savedStateHandle.toRoute<OfflineNavKey>()
    private val _uiState = MutableStateFlow(
        OfflineUiState(
            nodeId = args.nodeId,
            title = args.title,
            path = args.path,
            highlightedFiles = args
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
        // Navigate to path if specified
        navigateToPath()
    }

    /**
     * Navigates to the specified path, if exists
     */
    fun navigateToPath() {
        val path = args.path
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
                    offlineNodes = offlineNodeList.map { file ->
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
     * Update title
     */
    fun updateTitle(title: String?) {
        _uiState.update { it.copy(title = title) }
    }

}
