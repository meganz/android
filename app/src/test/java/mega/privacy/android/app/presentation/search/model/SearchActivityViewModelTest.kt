package mega.privacy.android.app.presentation.search.model

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchActivityViewModelTest {
    private lateinit var underTest: SearchActivityViewModel
    private val monitorNodeUpdates: MonitorNodeUpdates = FakeMonitorUpdates()
    private val incomingSharesTabSearchUseCase: IncomingSharesTabSearchUseCase = mock()
    private val outgoingSharesTabSearchUseCase: OutgoingSharesTabSearchUseCase = mock()
    private val linkSharesTabSearchUseCase: LinkSharesTabSearchUseCase = mock()
    private val searchInNodesUseCase: SearchInNodesUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase = mock()
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase = mock()
    private val getParentNodeHandle: GetParentNodeHandle = mock()
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase = mock()
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
        underTest = SearchActivityViewModel(
            monitorNodeUpdates = monitorNodeUpdates,
            incomingSharesTabSearchUseCase = incomingSharesTabSearchUseCase,
            outgoingSharesTabSearchUseCase = outgoingSharesTabSearchUseCase,
            linkSharesTabSearchUseCase = linkSharesTabSearchUseCase,
            searchInNodesUseCase = searchInNodesUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            getBackupsNodeUseCase = getBackupsNodeUseCase,
            getParentNodeHandle = getParentNodeHandle,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            stateHandle = stateHandle,
            getCloudSortOrder = getCloudSortOrder,
            fileDurationMapper = fileDurationMapper,
        )
    }

    @Test
    fun `test when when nodeUIItem is long clicked, then it updates selected item by 1`() =
        runTest {
            val query = underTest.state.value.searchQuery
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(getRootNodeUseCase()).thenReturn(nodesListItem1)

            whenever(searchInNodesUseCase(nodeId = nodesListItem1.id, query = query))
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
    fun `test that when item is clicked and some items are already selected on list then checked index gets decremented by 1`() =
        runTest {
            val query = underTest.state.value.searchQuery
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(getRootNodeUseCase()).thenReturn(nodesListItem1)

            whenever(searchInNodesUseCase(nodeId = nodesListItem1.id, query = query))
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
        }

    @Test
    fun `test that when selected item gets clicked then checked index gets incremented by 1`() =
        runTest {
            val query = underTest.state.value.searchQuery
            val nodesListItem1 = mock<TypedFileNode>()
            val nodesListItem2 = mock<TypedFolderNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(getRootNodeUseCase()).thenReturn(nodesListItem1)

            whenever(searchInNodesUseCase(nodeId = nodesListItem1.id, query = query))
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
        }

    @Test
    fun `test that on clicking on change view type to Grid it calls setViewType atleast once`() =
        runTest {
            underTest.onChangeViewTypeClicked()
            verify(setViewType).invoke(ViewType.GRID)
        }

    @Test
    fun `test that when onItemPerformedClicked and check value of current file node and selected index`() =
        runTest {
            underTest.onItemPerformedClicked()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.currentFileNode).isNull()
                Truth.assertThat(state.itemIndex).isEqualTo(-1)
            }
        }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
