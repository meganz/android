package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.toDuration
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CloudDriveViewModel @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    val fileTypeIconMapper: FileTypeIconMapper,
    savedStateHandle: SavedStateHandle,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<CloudDrive>()
    internal val uiState: StateFlow<CloudDriveUiState>
        field = MutableStateFlow(CloudDriveUiState(currentFolderId = NodeId(args.nodeHandle)))

    init {
        monitorViewType()
        loadNodes()
    }

    private fun loadNodes() {
        viewModelScope.launch {
            val folderId = uiState.value.currentFolderId
            runCatching {
                getNodeByIdUseCase(folderId) to getFileBrowserNodeChildrenUseCase(folderId.longValue)
            }.onSuccess { (currentNode, children) ->
                val title = currentNode?.name.takeIf { currentNode is FolderNode }?.let {
                    LocalizedText.Literal(it)
                } ?: LocalizedText.StringRes(sharedR.string.general_section_cloud_drive)
                val nodeUiItems = getNodeUiItems(
                    nodeList = children,
                )
                uiState.update { state ->
                    state.copy(
                        title = title,
                        isLoading = false,
                        items = nodeUiItems,
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Handle item click - navigate to folder if it's a folder
     */
    fun onItemClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        if (uiState.value.isInSelectionMode) {
            toggleItemSelection(nodeUiItem)
            return
        }
        when (nodeUiItem.node) {
            is TypedFolderNode -> {
                uiState.update { state ->
                    state.copy(
                        navigateToFolderEvent = triggered(nodeUiItem.id)
                    )
                }
            }

            is TypedFileNode -> {
                uiState.update { state ->
                    state.copy(
                        openedFileNode = nodeUiItem.node as TypedFileNode
                    )
                }
            }
        }
    }

    /**
     * Consume navigation event
     */
    fun onNavigateToFolderEventConsumed() {
        uiState.update { state ->
            state.copy(navigateToFolderEvent = consumed())
        }
    }

    /**
     * Handle item long click - toggle selection state
     */
    fun onItemLongClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        toggleItemSelection(nodeUiItem)
    }

    private fun toggleItemSelection(nodeUiItem: NodeUiItem<TypedNode>) {
        val updatedItems = uiState.value.items.map { item ->
            if (item.node.id == nodeUiItem.node.id) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        uiState.update { state ->
            state.copy(items = updatedItems)
        }
    }

    /**
     * Deselect all items and reset selection state
     */
    fun deselectAllItems() {
        val updatedItems = uiState.value.items.map { it.copy(isSelected = false) }
        uiState.update { state ->
            state.copy(items = updatedItems)
        }
    }

    /**
     * Select all items
     */
    fun selectAllItems() {
        val updatedItems = uiState.value.items.map { it.copy(isSelected = true) }
        uiState.update { state ->
            state.copy(items = updatedItems)
        }
    }

    /**
     * This method will toggle node view type between list and grid.
     */
    fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            runCatching {
                val toggledViewType = when (uiState.value.currentViewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setViewTypeUseCase(toggledViewType)
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }
        }
    }

    private fun monitorViewType() {
        viewModelScope.launch {
            monitorViewTypeUseCase()
                .catch { Timber.e(it) }
                .collect { viewType ->
                    uiState.update { it.copy(currentViewType = viewType) }
                }
        }
    }

    /**
     * A temporary mapper for sample screen
     */
    private suspend fun getNodeUiItems(
        nodeList: List<TypedNode>,
        highlightedNodeId: NodeId? = null,
        highlightedNames: Set<String>? = null,
    ): List<NodeUiItem<TypedNode>> = withContext(defaultDispatcher) {
        val existingNodeList = uiState.value.items
        val existingHighlightedIds = existingNodeList.asSequence()
            .filter { it.isHighlighted }
            .map { it.node.id }
            .toSet()

        nodeList.mapIndexed { index, node ->
            val fileDuration = if (node is FileNode) {
                node.type.toDuration()?.let { durationInSecondsTextMapper(it) }
            } else null
            val isHighlighted = existingHighlightedIds.contains(node.id) ||
                    node.id == highlightedNodeId ||
                    highlightedNames?.contains(node.name) == true
            val hasCorrespondingIndex = existingNodeList.size > index
            NodeUiItem(
                node = node,
                isSelected = if (hasCorrespondingIndex) existingNodeList[index].isSelected else false,
                isInvisible = if (hasCorrespondingIndex) existingNodeList[index].isInvisible else false,
                fileDuration = fileDuration,
                isHighlighted = isHighlighted,
            )
        }
    }

    /**
     * Handle the event when a file node is opened
     */
    fun onOpenedFileNodeHandled() {
        uiState.update { state ->
            state.copy(openedFileNode = null)
        }
    }
}
