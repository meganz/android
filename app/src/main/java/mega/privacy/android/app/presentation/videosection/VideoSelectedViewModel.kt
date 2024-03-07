package mega.privacy.android.app.presentation.videosection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.videosection.model.VideoSelectedState
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for video selected
 */
@HiltViewModel
class VideoSelectedViewModel @Inject constructor(
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
) : ViewModel() {

    private val _state = MutableStateFlow(VideoSelectedState())

    /**
     * The state regarding the business logic
     */
    val state: StateFlow<VideoSelectedState> = _state.asStateFlow()

    init {
        refreshNodes()
        checkViewType()
    }

    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _state.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    private fun refreshNodes(
        parentHandle: Long? = null,
        topBarTitle: String? = null,
    ) = viewModelScope.launch {
        val childrenNodes = getFileBrowserNodeChildrenUseCase(parentHandle ?: -1)
        val nodeUIItems = childrenNodes.getNodeUIItems().filterBySearchQuery()
        val sortOrder = getCloudSortOrder()

        _state.update {
            it.copy(
                nodesList = nodeUIItems,
                isLoading = false,
                sortOrder = sortOrder,
                topBarTitle = topBarTitle,
                currentFolderHandle = parentHandle ?: -1,
                openedFolderNodeHandles = if (parentHandle == null)
                    emptySet()
                else
                    it.openedFolderNodeHandles
            )
        }
    }

    private fun List<TypedNode>.getNodeUIItems() = filter {
        if (it is FileNode) {
            it.type is VideoFileTypeInfo
        } else true
    }.map { node ->
        NodeUIItem(
            node = node,
            isSelected = false
        )
    }

    private fun List<NodeUIItem<TypedNode>>.filterBySearchQuery() =
        filter { item ->
            _state.value.query?.let { query ->
                item.name.contains(query, true)
            } ?: true
        }

    internal fun itemClicked(item: NodeUIItem<TypedNode>) {
        when (item.node) {
            is TypedFileNode -> {
                _state.value.nodesList.indexOf(item).let { index ->
                    if (index != -1) {
                        updateVideoItemInSelectionState(item = item, index = index)
                    } else {
                        Timber.e("The index is invalid.")
                    }
                }
            }

            is TypedFolderNode -> {
                val openedFolderHandles =
                    _state.value.openedFolderNodeHandles.toMutableSet().apply {
                        add(_state.value.currentFolderHandle)
                    }
                _state.update {
                    it.copy(
                        isLoading = true,
                        openedFolderNodeHandles = openedFolderHandles
                    )
                }
                refreshNodes(item.id.longValue, item.name)
            }

        }
    }

    private fun updateVideoItemInSelectionState(item: NodeUIItem<TypedNode>, index: Int) {
        val isSelected = !item.isSelected
        val selectedHandles = updateSelectedVideoHandles(item, isSelected)
        val nodes = _state.value.nodesList.updateItemSelectedState(index, isSelected)
        _state.update {
            it.copy(
                nodesList = nodes,
                selectedNodeHandles = selectedHandles
            )
        }
    }

    private fun List<NodeUIItem<TypedNode>>.updateItemSelectedState(
        index: Int,
        isSelected: Boolean,
    ) =
        if (index in indices) {
            toMutableList().also { list ->
                list[index] = list[index].copy(isSelected = isSelected)
            }
        } else this


    private fun updateSelectedVideoHandles(item: NodeUIItem<TypedNode>, isSelected: Boolean) =
        _state.value.selectedNodeHandles.toMutableList().also { selectedHandles ->
            if (isSelected) {
                selectedHandles.add(item.id.longValue)
            } else {
                selectedHandles.remove(item.id.longValue)
            }
        }

    internal fun backToParentFolder() {
        viewModelScope.launch {
            val rootNode = getRootNodeUseCase()
            val parentNode = getParentNodeUseCase(NodeId(_state.value.currentFolderHandle))
            val openedHandles =
                _state.value.openedFolderNodeHandles.toMutableSet().apply {
                    remove(_state.value.currentFolderHandle)
                }

            _state.update {
                it.copy(
                    isLoading = true,
                    openedFolderNodeHandles = openedHandles
                )
            }
            val isRoot = rootNode?.id == parentNode?.id
            val parentHandle = if (isRoot) null else parentNode?.id?.longValue
            val topBarTitle = if (isRoot) null else parentNode?.name
            refreshNodes(parentHandle = parentHandle, topBarTitle = topBarTitle)
        }
    }

    internal fun selectAllVideos() {
        val nodes = _state.value.nodesList.map { item ->
            item.copy(isSelected = item.node is TypedFileNode)
        }
        val selectedHandles = nodes.filter {
            it.isSelected
        }.map { item ->
            item.id.longValue
        }
        _state.update {
            it.copy(
                nodesList = nodes,
                selectedNodeHandles = selectedHandles
            )
        }
    }

    internal fun clearAllSelectedVideos() {
        val nodes = clearVideosSelected()
        _state.update {
            it.copy(
                nodesList = nodes,
                selectedNodeHandles = emptyList()
            )
        }
    }

    private fun clearVideosSelected() = _state.value.nodesList.map {
        it.copy(isSelected = false)
    }

    internal fun refreshWhenOrderChanged() =
        viewModelScope.launch {
            val sortOrder = getCloudSortOrder()
            _state.update {
                it.copy(
                    sortOrder = sortOrder,
                    isLoading = true
                )
            }
            refreshNodesByCurrentFolder()
        }

    private fun refreshNodesByCurrentFolder() {
        val handle = _state.value.currentFolderHandle
        val title = _state.value.topBarTitle
        refreshNodes(parentHandle = handle, topBarTitle = title)
    }

    internal fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_state.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    internal fun searchQuery(queryString: String) {
        _state.update {
            it.copy(
                query = queryString
            )
        }
        viewModelScope.launch {
            refreshNodesByCurrentFolder()
        }
    }

    internal fun searchWidgetStateUpdate() {
        val searchState = when (_state.value.searchState) {
            SearchWidgetState.EXPANDED -> SearchWidgetState.COLLAPSED

            SearchWidgetState.COLLAPSED -> SearchWidgetState.EXPANDED
        }
        _state.update {
            it.copy(
                searchState = searchState
            )
        }
    }

    internal fun closeSearch() {
        _state.update {
            it.copy(
                query = null,
                searchState = SearchWidgetState.COLLAPSED
            )
        }
        refreshNodesByCurrentFolder()
    }
}