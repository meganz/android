package mega.privacy.android.app.presentation.search.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.search.IncomingSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.LinkSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.OutgoingSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.SearchInNodesUseCase
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
        viewModelScope.launch {
            monitorNodeUpdates().collect {
                _state.update {
                    it.copy(searchItemList = it.searchItemList)
                }
            }
        }
        initializeSearch()
    }

    private fun initializeSearch() {
        performSearch(
            isFirstLevel = isFirstLevel,
            query = state.value.searchQuery,
            searchType = searchType,
            parentHandle = parentHandle
        )
    }

    /**
     * Perform search by entering query or change in search type
     */
    fun performSearch(
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
                    it.copy(searchItemList = getNodeUiItems(searchList), isInProgress = false)
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
     * Update the search handle
     */
    fun updateSearchHandle(handle: Long) {
        viewModelScope.launch {
            _state.update {
                it.copy(parentHandle = handle)
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
        val existingNodeList = state.value.searchItemList
        return nodeList.mapIndexed { index, node ->
            NodeUIItem(
                node = node,
                isSelected = false,
                isInvisible = if (existingNodeList.size > index) existingNodeList[index].isInvisible else false,
                isAvailableOffline = isAvailableOfflineUseCase(node)
            )
        }
    }
}