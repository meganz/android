package mega.privacy.android.app.presentation.search.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetInboxNodeUseCase
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
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
 * @property getInboxNodeUseCase [GetInboxNodeUseCase]
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
    private val getInboxNodeUseCase: GetInboxNodeUseCase,
    private val getParentNodeHandle: GetParentNodeHandle,
) : ViewModel() {
    /**
     * private UI state
     */
    private val _state = MutableStateFlow(SearchActivityState())

    /**
     * public UI State
     */
    val state: StateFlow<SearchActivityState> = _state

    init {
        viewModelScope.launch {
            monitorNodeUpdates().collect {
                _state.update {
                    it.copy(searchItemList = it.searchItemList)
                }
            }
        }
    }

    /**
     * Perform search by entering query or change in search type
     */
    fun performSearch(
        query: String,
        isFirstLevel: Boolean = false,
        currentTab: DrawerItem?,
        sharesTab: Int,
        currentHandle: Long,
    ) {
        viewModelScope.launch {
            runCatching {
                val parentHandle = getParentNodeHandle(currentHandle)
                if (query.isBlank().not()) {
                    _state.update {
                        it.copy(isInProgress = true)
                    }
                    val node = getNodeHandle(currentTab = currentTab, parentHandle = parentHandle)

                    val searchList = getSearchResults(
                        currentTab = currentTab,
                        query = query,
                        isFirstLevel = isFirstLevel,
                        node = node,
                        sharesTab = sharesTab
                    )
                    _state.update {
                        it.copy(searchItemList = searchList, isInProgress = false)
                    }
                } else {
                    _state.update {
                        it.copy(searchItemList = emptyList(), isInProgress = false)
                    }
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

    private fun isParentHandleIsInvalidHandle(handle: Long?) = handle == MegaApiJava.INVALID_HANDLE

    /**
     * This method Returns [Node] for respective selected [DrawerItem]
     * @param currentTab current selected tab
     * @param parentHandle parent handle
     * @return [Node]
     */
    private suspend fun getNodeHandle(currentTab: DrawerItem?, parentHandle: Long?) =
        if (isParentHandleIsInvalidHandle(state.value.parentHandle)) {
            if (currentTab == DrawerItem.HOMEPAGE) getRootNodeUseCase()
            else if (currentTab == DrawerItem.CLOUD_DRIVE) getNodeByHandleUseCase(state.value.parentHandle)
            else {
                if (!isParentHandleIsInvalidHandle(parentHandle))
                    getNodeByHandleUseCase(state.value.parentHandle)
                else {
                    when (currentTab) {
                        DrawerItem.RUBBISH_BIN -> {
                            getRubbishNodeUseCase()
                        }

                        DrawerItem.INBOX -> {
                            getInboxNodeUseCase()
                        }

                        else -> {
                            null
                        }
                    }
                }
            }
        } else {
            getNodeByHandleUseCase(state.value.parentHandle)
        }

    /**
     * This method returns list of search items
     * @param currentTab current tab
     * @param sharesTab share tab id
     * @param query query to be searched
     * @param isFirstLevel is first level
     * @param node Node
     * @return list of TypedNode
     */
    private suspend fun getSearchResults(
        currentTab: DrawerItem?,
        sharesTab: Int,
        query: String,
        isFirstLevel: Boolean,
        node: Node?,
    ) =
        when (currentTab) {
            DrawerItem.SHARED_ITEMS -> {
                when (SharesTab.fromPosition(sharesTab)) {
                    SharesTab.INCOMING_TAB -> {
                        incomingSharesTabSearchUseCase(
                            query = query
                        )
                    }

                    SharesTab.OUTGOING_TAB -> {
                        outgoingSharesTabSearchUseCase(
                            query = query
                        )
                    }

                    SharesTab.LINKS_TAB -> {
                        linkSharesTabSearchUseCase(
                            query = query,
                            isFirstLevel = isFirstLevel
                        )
                    }

                    else -> {
                        searchInNodesUseCase(
                            nodeId = node?.id,
                            query = query,
                            searchType = -1
                        )
                    }
                }
            }

            else -> {
                searchInNodesUseCase(
                    nodeId = node?.id,
                    query = query,
                    searchType = -1
                )
            }
        }
}