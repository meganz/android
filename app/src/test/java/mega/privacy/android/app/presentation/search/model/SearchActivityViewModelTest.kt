package mega.privacy.android.app.presentation.search.model

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.search.GetSearchCategoriesUseCase
import mega.privacy.android.domain.usecase.search.SearchNodesUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchActivityViewModelTest {
    private lateinit var underTest: SearchActivityViewModel
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase = mock()
    private val searchNodesUseCase: SearchNodesUseCase = mock()
    private val getSearchCategoriesUseCase: GetSearchCategoriesUseCase = mock()
    private val searchFilterMapper: SearchFilterMapper = mock()
    private val emptySearchViewMapper: EmptySearchViewMapper = mock()
    private val stateHandle: SavedStateHandle = mock()
    private val setViewType: SetViewType = mock()
    private val monitorViewType: MonitorViewType = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase = mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode = mock()
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase = mock()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()

    private val nodeList = mutableListOf<TypedNode>()

    private val parentHandle = 123456L
    private val isFirstLevel = false
    private val nodeSourceType = NodeSourceType.CLOUD_DRIVE

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private fun initViewModel() {
        underTest = SearchActivityViewModel(
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            stateHandle = stateHandle,
            getCloudSortOrder = getCloudSortOrder,
            cancelCancelTokenUseCase = cancelCancelTokenUseCase,
            searchNodesUseCase = searchNodesUseCase,
            getSearchCategoriesUseCase = getSearchCategoriesUseCase,
            searchFilterMapper = searchFilterMapper,
            emptySearchViewMapper = emptySearchViewMapper,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
        )
    }

    @Test
    fun `test that the selected item is updated by 1 when nodeUIItem is long clicked`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            underTest.onSortOrderChanged()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedNodes.size).isEqualTo(1)
                Truth.assertThat(state.selectedNodes.filter { it is FileNode }.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that the checked index gets decremented by 1 when the item is clicked and some items are already selected in the list`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            underTest.onSortOrderChanged()
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedNodes.size).isEqualTo(0)
            }
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem2,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedNodes.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that the checked index gets incremented by 1 when the selected item gets clicked`() =
        runTest {
            val nodesListItem1 = mock<TypedFileNode>()
            val nodesListItem2 = mock<TypedFolderNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            underTest.onSortOrderChanged()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem2,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedNodes.size).isEqualTo(2)
                Truth.assertThat(state.selectedNodes.filter { it is FileNode }.size).isEqualTo(1)
            }
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedNodes.size).isEqualTo(1)
                Truth.assertThat(state.selectedNodes.any { it is FileNode }).isFalse()
            }
        }

    @Test
    fun `test that setViewType is called at least once when changing the view type to grid`() =
        runTest {
            underTest.onChangeViewTypeClicked()
            verify(setViewType).invoke(ViewType.GRID)
        }

    @Test
    fun `test that the current file node and item index have correct values when onItemPerformedClicked is invoked`() =
        runTest {
            underTest.onItemPerformedClicked()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.lastSelectedNode).isNull()
            }
        }

    @Test
    fun `test that the search functionality is performed with an updated filter when update filter is called`() =
        runTest {
            val filter = SearchFilter(name = "Images", filter = SearchCategory.IMAGES)
            val typedFolderNode = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(345L))
                on { name }.thenReturn("folder node")
            }
            val typedFileNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(123L))
                on { name }.thenReturn("file node")
            }
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(
                searchNodesUseCase(
                    query = "",
                    parentHandle = parentHandle,
                    nodeSourceType = nodeSourceType,
                    isFirstLevel = isFirstLevel,
                    searchCategory = filter.filter
                )
            ).thenReturn(listOf(typedFileNode, typedFolderNode))
            underTest.updateFilter(filter)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedFilter).isEqualTo(filter)
                Truth.assertThat(state.searchItemList.size).isEqualTo(2)
            }
        }


    @Test
    fun `test that the search functionality is performed with an updated filter when update search query is called`() =
        runTest {
            val query = "query"

            val typedFolderNode = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(345L))
                on { name }.thenReturn("folder node")
            }
            val typedFileNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(123L))
                on { name }.thenReturn("file node")
            }

            val parentHandle = 123456L
            val isFirstLevel = false
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))

            nodeList.add(typedFileNode)
            nodeList.add(typedFolderNode)

            whenever(
                searchNodesUseCase(
                    query = query,
                    parentHandle = parentHandle,
                    nodeSourceType = nodeSourceType,
                    isFirstLevel = isFirstLevel,
                )
            ).thenReturn(nodeList)
            underTest.updateSearchQuery(query)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.searchQuery).isEqualTo(query)
                Truth.assertThat(state.searchItemList.size).isEqualTo(nodeList.size)
            }
        }


    @Test
    fun `test that an empty search list is returned when the search functionality throws an exception`() =
        runTest {
            val filter = SearchFilter(name = "Images", filter = SearchCategory.IMAGES)
            val parentHandle = 123456L
            val isFirstLevel = false
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(
                searchNodesUseCase(
                    query = "",
                    parentHandle = parentHandle,
                    nodeSourceType = nodeSourceType,
                    isFirstLevel = isFirstLevel,
                    searchCategory = filter.filter
                )
            ).thenThrow(IllegalStateException("Search exception"))
            underTest.updateFilter(filter)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedFilter).isEqualTo(filter)
                Truth.assertThat(state.searchItemList).isEmpty()
            }
        }

    @Test
    fun `test that the error message id is updated when show error message is called`() =
        runTest {
            val errorMessageId = 123
            underTest.showShowErrorMessage(errorMessageId)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.errorMessageId).isEqualTo(errorMessageId)
            }
            underTest.errorMessageShown()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.errorMessageId).isNull()
            }
        }

    @Test
    fun `test that selected list updates when select all and clear selection performed`() =
        runTest {
            val query = "query"
            val typedFolderNode = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(345L))
                on { name }.thenReturn("folder node")
            }
            val typedFileNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(123L))
                on { name }.thenReturn("file node")
            }

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(
                searchNodesUseCase(
                    query = query,
                    parentHandle = parentHandle,
                    nodeSourceType = nodeSourceType,
                    isFirstLevel = isFirstLevel,
                )
            ).thenReturn(listOf(typedFileNode, typedFolderNode))
            underTest.updateSearchQuery(query)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.searchQuery).isEqualTo(query)
                Truth.assertThat(state.searchItemList.size).isEqualTo(2)
                Truth.assertThat(state.selectedNodes).isEmpty()
                underTest.selectAll()
                val selectAllState = awaitItem()
                Truth.assertThat(selectAllState.selectedNodes).contains(typedFileNode)
                Truth.assertThat(selectAllState.selectedNodes).contains(typedFolderNode)
                Truth.assertThat(selectAllState.selectedNodes.size).isEqualTo(2)
                underTest.clearSelection()
                val clearedState = awaitItem()
                Truth.assertThat(clearedState.selectedNodes.size).isEqualTo(0)
            }
        }

    private suspend fun stubCommon() {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

        whenever(stateHandle.get<NodeSourceType>(SearchActivity.SEARCH_TYPE)).thenReturn(
            NodeSourceType.CLOUD_DRIVE)
        whenever(stateHandle.get<Long>(SearchActivity.PARENT_HANDLE)).thenReturn(123456L)
        whenever(stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL)).thenReturn(
            false
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        nodeList.clear()
        reset(
            monitorNodeUpdatesUseCase,
            cancelCancelTokenUseCase,
            searchNodesUseCase,
            getSearchCategoriesUseCase,
            searchFilterMapper,
            emptySearchViewMapper,
            stateHandle,
            setViewType,
            monitorViewType,
            getCloudSortOrder,
            getRubbishNodeUseCase,
            getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode,
            isNodeInBackupsUseCase,
            monitorOfflineNodeUpdatesUseCase
        )
    }
}
