package mega.privacy.android.app.presentation.search.model

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.usecase.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.search.IncomingSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.LinkSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.OutgoingSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.SearchInNodesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * SearchActivity View Model
 * @property monitorNodeUpdates [MonitorNodeUpdates]
 * @property incomingSharesTabSearchUseCase [IncomingSharesTabSearchUseCase]
 * @property outgoingSharesTabSearchUseCase [OutgoingSharesTabSearchUseCase]
 * @property searchInNodesUseCase [SearchInNodesUseCase]
 * @property getRootNodeUseCase [GetRootNodeUseCase]
 * @property getNodeByHandleUseCase [GetNodeByHandleUseCase]
 * @property getRubbishNodeUseCase [GetRubbishNodeUseCase]
 * @property getBackupsNodeUseCase [GetBackupsNodeUseCase]
 * @property getParentNodeHandle [GetParentNodeHandle]
 * @property setViewType [SetViewType]
 */
@HiltViewModel
class SearchActivityViewModel @Inject constructor(
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val incomingSharesTabSearchUseCase: IncomingSharesTabSearchUseCase,
    private val outgoingSharesTabSearchUseCase: OutgoingSharesTabSearchUseCase,
    private val linkSharesTabSearchUseCase: LinkSharesTabSearchUseCase,
    private val searchInNodesUseCase: SearchInNodesUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val fileDurationMapper: FileDurationMapper,
    stateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * private UI state
     */
    private val _state = MutableStateFlow(SearchActivityState())

    /**
     * public UI State
     */
    val state: StateFlow<SearchActivityState> = _state

    private val isFirstLevel = stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL) ?: false
    private val searchType =
        stateHandle.get<SearchType>(SearchActivity.SEARCH_TYPE) ?: SearchType.OTHER
    private val parentHandle =
        stateHandle.get<Long>(SearchActivity.PARENT_HANDLE) ?: MegaApiJava.INVALID_HANDLE

    init {
        monitorNodeUpdatesForSearch()
        initializeSearch()
        checkViewType()
    }

    private fun initializeSearch() {
        performSearch(
            isFirstLevel = isFirstLevel,
            query = state.value.searchQuery,
            searchType = searchType,
            parentHandle = parentHandle
        )
    }

    private fun monitorNodeUpdatesForSearch() {
        viewModelScope.launch {
            monitorNodeUpdates().collect { nodeUpdate ->
                if (nodeUpdate.changes.keys.find { state.value.parentHandle == it.parentId.longValue } != null)
                    performSearch(
                        isFirstLevel = isFirstLevel,
                        query = state.value.searchQuery,
                        searchType = searchType,
                        parentHandle = parentHandle
                    )
            }
        }
    }

    /**
     * Perform search by entering query or change in search type
     */
    private fun performSearch(
        query: String,
        isFirstLevel: Boolean = false,
        searchType: SearchType,
        parentHandle: Long,
    ) {
        viewModelScope.launch {
            runCatching {
                _state.update {
                    it.copy(isInProgress = true)
                }
                val node = getSearchParentNode(searchType = searchType, parentHandle = parentHandle)

                val searchList = getSearchResults(
                    query = query,
                    isFirstLevel = isFirstLevel,
                    node = node,
                    searchType = searchType
                )
                _state.update {
                    it.copy(
                        searchItemList = getNodeUiItems(searchList),
                        isInProgress = false,
                        sortOrder = getCloudSortOrder()
                    )
                }
            }.onFailure { ex ->
                Timber.e(ex)
                _state.update {
                    it.copy(searchItemList = emptyList(), isInProgress = false)
                }
            }
        }
    }

    /**
     * This method Returns [Node] for respective selected [SearchType]
     * @param parentHandle parent handle
     * @return [Node]
     */
    private suspend fun getSearchParentNode(searchType: SearchType, parentHandle: Long?): Node? =
        if (parentHandle == null || parentHandle == -1L) {
            when (searchType) {
                SearchType.CLOUD_DRIVE -> getRootNodeUseCase()
                SearchType.RUBBISH_BIN -> getRubbishNodeUseCase()
                SearchType.BACKUPS -> getBackupsNodeUseCase()
                else -> null
            }
        } else {
            getNodeByHandleUseCase(parentHandle)
        }

    /**
     * This method returns list of search items
     * @param searchType current tab
     * @param query query to be searched
     * @param isFirstLevel is first level
     * @param node Node
     * @return list of TypedNode
     */
    private suspend fun getSearchResults(
        searchType: SearchType,
        query: String,
        isFirstLevel: Boolean,
        node: Node?,
    ) =
        when (searchType) {
            SearchType.INCOMING_SHARES -> incomingSharesTabSearchUseCase(query = query)
            SearchType.OUTGOING_SHARES -> outgoingSharesTabSearchUseCase(query = query)
            SearchType.LINKS -> linkSharesTabSearchUseCase(
                query = query,
                isFirstLevel = isFirstLevel
            )

            else -> searchInNodesUseCase(
                nodeId = node?.id,
                query = query,
                searchCategory = state.value.searchType
            )
        }

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private suspend fun getNodeUiItems(nodeList: List<TypedNode>): List<NodeUIItem<TypedNode>> {
        return nodeList.map {
            val fileDuration = if (it is FileNode) {
                fileDurationMapper(it.type)?.let { TimeUtils.getVideoDuration(it) } ?: run { null }
            } else null
            NodeUIItem(
                node = it,
                isSelected = false,
                isInvisible = false,
                isAvailableOffline = isAvailableOfflineUseCase(it),
                fileDuration = fileDuration
            )
        }
    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count else get MegaNode
     * and navigate to appropriate activity
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem<TypedNode>) {
        val index =
            _state.value.searchItemList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        if (_state.value.isInSelection) {
            updateNodeSelection(nodeUIItem = nodeUIItem, index = index)
        } else {
            if (nodeUIItem.node is FileNode) {
                _state.update {
                    it.copy(
                        itemIndex = index,
                        currentFileNode = nodeUIItem.node
                    )
                }

            } else {
                _state.update {
                    it.copy(
                        currentFolderClickedHandle = nodeUIItem.id.longValue
                    )
                }
            }
        }
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeSelection(nodeUIItem: NodeUIItem<TypedNode>, index: Int) {
        nodeUIItem.isSelected = !nodeUIItem.isSelected
        val selectedNodeHandleList = state.value.selectedNodeHandles.toMutableList()
        selectedNodeHandleList.apply {
            if (nodeUIItem.isSelected) add(nodeUIItem.node.id.longValue) else remove(nodeUIItem.node.id.longValue)
        }
        val pair = selectUnSelectNode(nodeUIItem = nodeUIItem, nodeSelected = nodeUIItem.isSelected)
        selectUnSelectNode(nodeUIItem = nodeUIItem, nodeSelected = nodeUIItem.isSelected)
        val newNodesList =
            _state.value.searchItemList.updateItemAt(index = index, item = nodeUIItem)
        _state.update {
            it.copy(
                selectedFileNodes = pair.first,
                selectedFolderNodes = pair.second,
                searchItemList = newNodesList,
                isInSelection = pair.first > 0 || pair.second > 0,
                selectedNodeHandles = selectedNodeHandleList,
                optionsItemInfo = null
            )
        }
    }

    /**
     * select a node
     * @param nodeUIItem
     * @param nodeSelected if node is selected or removed selection
     * @return Pair of count of Selected File Node and Selected Folder Node
     */
    private fun selectUnSelectNode(
        nodeUIItem: NodeUIItem<TypedNode>,
        nodeSelected: Boolean,
    ): Pair<Int, Int> {
        var totalSelectedFileNode = state.value.selectedFileNodes
        var totalSelectedFolderNode = state.value.selectedFolderNodes
        if (nodeUIItem.node is FolderNode) {
            totalSelectedFolderNode =
                if (nodeSelected) _state.value.selectedFolderNodes + 1 else _state.value.selectedFolderNodes - 1
        } else if (nodeUIItem.node is FileNode) {
            totalSelectedFileNode = if (nodeSelected) _state.value.selectedFileNodes + 1 else
                _state.value.selectedFileNodes - 1
        }
        return Pair(totalSelectedFileNode, totalSelectedFolderNode)
    }

    /**
     * This method will handle Long click on a NodesView and check the selected item
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onLongItemClicked(nodeUIItem: NodeUIItem<TypedNode>) {
        val index =
            _state.value.searchItemList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        updateNodeSelection(nodeUIItem = nodeUIItem, index = index)
    }

    /**
     * This method will toggle view type
     */
    fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_state.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    /**
     * When item is clicked on activity
     */
    fun onItemPerformedClicked() {
        _state.update {
            it.copy(
                currentFileNode = null,
                itemIndex = -1,
            )
        }
    }

    /**
     * This method will monitor view type and update it on state
     */
    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _state.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    /**
     * When we change sort order from UI
     */
    fun onSortOrderChanged() {
        performSearch(
            isFirstLevel = isFirstLevel,
            query = state.value.searchQuery,
            searchType = searchType,
            parentHandle = parentHandle
        )
    }

    /**
     * Show error message on UI
     */
    fun showShowErrorMessage(@StringRes errorMessageResId: Int) {
        _state.update {
            it.copy(
                errorMessageId = errorMessageResId
            )
        }
    }

    /**
     * Remove error message
     */
    fun errorMessageShown() {
        _state.update {
            it.copy(
                errorMessageId = null
            )
        }
    }
}