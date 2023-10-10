package mega.privacy.android.app.presentation.search.model

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.search.GetSearchCategoriesUseCase
import mega.privacy.android.domain.usecase.search.SearchNodesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchActivityViewModelTest {
    private lateinit var underTest: SearchActivityViewModel
    private val monitorNodeUpdates: MonitorNodeUpdates = FakeMonitorUpdates()
    private val getParentNodeHandle: GetParentNodeHandle = mock()
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase = mock()
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase = mock()
    private val searchNodesUseCase: SearchNodesUseCase = mock()
    private val getSearchCategoriesUseCase: GetSearchCategoriesUseCase = mock()
    private val searchFilterMapper: SearchFilterMapper = mock()
    private val emptySearchViewMapper: EmptySearchViewMapper = mock()
    private val stateHandle: SavedStateHandle = mock()
    private val setViewType: SetViewType = mock()
    private val monitorViewType: MonitorViewType = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val fileDurationMapper: FileDurationMapper = mock {
        onBlocking { invoke(any()) }.thenReturn(null)
    }

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getParentNodeHandle,
            isAvailableOfflineUseCase,
            cancelCancelTokenUseCase,
            searchNodesUseCase,
            getSearchCategoriesUseCase,
            searchFilterMapper,
            emptySearchViewMapper,
            stateHandle,
            setViewType,
            monitorViewType,
            getCloudSortOrder,
            fileDurationMapper
        )
    }

    private fun initViewModel() {
        underTest = SearchActivityViewModel(
            monitorNodeUpdates = monitorNodeUpdates,
            getParentNodeHandle = getParentNodeHandle,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            stateHandle = stateHandle,
            getCloudSortOrder = getCloudSortOrder,
            fileDurationMapper = fileDurationMapper,
            cancelCancelTokenUseCase = cancelCancelTokenUseCase,
            searchNodesUseCase = searchNodesUseCase,
            getSearchCategoriesUseCase = getSearchCategoriesUseCase,
            searchFilterMapper = searchFilterMapper,
            emptySearchViewMapper = emptySearchViewMapper,
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
            initViewModel()
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
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(1)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(0)
                Truth.assertThat(state.selectedNodeHandles.size).isEqualTo(1)
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
            initViewModel()
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
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(0)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(0)
                Truth.assertThat(state.selectedNodeHandles.size).isEqualTo(0)
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
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(0)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(0)
                Truth.assertThat(state.selectedNodeHandles.size).isEqualTo(0)
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
            initViewModel()
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
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(1)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(1)
                Truth.assertThat(state.selectedNodeHandles.size).isEqualTo(2)
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
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(1)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(0)
                Truth.assertThat(state.selectedNodeHandles.size).isEqualTo(1)
            }
        }

    @Test
    fun `test that setViewType is called at least once when changing the view type to grid`() =
        runTest {
            initViewModel()
            underTest.onChangeViewTypeClicked()
            verify(setViewType).invoke(ViewType.GRID)
        }

    @Test
    fun `test that the current file node and item index have correct values when onItemPerformedClicked is invoked`() =
        runTest {
            initViewModel()
            underTest.onItemPerformedClicked()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.currentFileNode).isNull()
                Truth.assertThat(state.itemIndex).isEqualTo(-1)
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
            val parentHandle = 123456L
            val isFirstLevel = false
            val searchType = SearchType.CLOUD_DRIVE
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(stateHandle.get<SearchType>(SearchActivity.SEARCH_TYPE)).thenReturn(searchType)
            whenever(stateHandle.get<Long>(SearchActivity.PARENT_HANDLE)).thenReturn(parentHandle)
            whenever(stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL)).thenReturn(
                isFirstLevel
            )
            whenever(isAvailableOfflineUseCase(any())).thenReturn(false)
            whenever(
                searchNodesUseCase(
                    query = "",
                    parentHandle = parentHandle,
                    searchType = searchType,
                    isFirstLevel = isFirstLevel,
                    searchCategory = filter.filter
                )
            ).thenReturn(listOf(typedFileNode, typedFolderNode))
            initViewModel()
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
            val searchType = SearchType.CLOUD_DRIVE
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(stateHandle.get<SearchType>(SearchActivity.SEARCH_TYPE)).thenReturn(searchType)
            whenever(stateHandle.get<Long>(SearchActivity.PARENT_HANDLE)).thenReturn(parentHandle)
            whenever(stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL)).thenReturn(
                isFirstLevel
            )
            whenever(isAvailableOfflineUseCase(any())).thenReturn(false)
            whenever(
                searchNodesUseCase(
                    query = query,
                    parentHandle = parentHandle,
                    searchType = searchType,
                    isFirstLevel = isFirstLevel,
                )
            ).thenReturn(listOf(typedFileNode, typedFolderNode))
            initViewModel()
            underTest.updateSearchQuery(query)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.searchQuery).isEqualTo(query)
                Truth.assertThat(state.searchItemList.size).isEqualTo(2)
            }
        }


    @Test
    fun `test that an empty search list is returned when the search functionality throws an exception`() =
        runTest {
            val filter = SearchFilter(name = "Images", filter = SearchCategory.IMAGES)
            val parentHandle = 123456L
            val isFirstLevel = false
            val searchType = SearchType.CLOUD_DRIVE
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(stateHandle.get<SearchType>(SearchActivity.SEARCH_TYPE)).thenReturn(searchType)
            whenever(stateHandle.get<Long>(SearchActivity.PARENT_HANDLE)).thenReturn(parentHandle)
            whenever(stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL)).thenReturn(
                isFirstLevel
            )
            whenever(isAvailableOfflineUseCase(any())).thenReturn(false)
            whenever(
                searchNodesUseCase(
                    query = "",
                    parentHandle = parentHandle,
                    searchType = searchType,
                    isFirstLevel = isFirstLevel,
                    searchCategory = filter.filter
                )
            ).thenThrow(IllegalStateException("Search exception"))
            initViewModel()
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
            initViewModel()
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

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
