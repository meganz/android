package mega.privacy.android.feature.clouddrive.presentation.shares.links

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.publiclink.MonitorPublicLinksUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.links.model.LinksAction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LinksViewModelTest {
    private val monitorPublicLinksUseCase: MonitorPublicLinksUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val setCloudSortOrder: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()

    private lateinit var testScheduler: TestCoroutineScheduler

    @Before
    fun setUp() {
        testScheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            monitorPublicLinksUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            nodeUiItemMapper,
            getCloudSortOrder,
            setCloudSortOrder,
            nodeSortConfigurationUiMapper
        )
    }

    private fun createViewModel() = LinksViewModel(
        monitorPublicLinksUseCase = monitorPublicLinksUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        getCloudSortOrder = getCloudSortOrder,
        setCloudSortOrder = setCloudSortOrder,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
    )

    private suspend fun setupTestData(items: List<PublicLinkNode>) {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(nodeSortConfigurationUiMapper(any<SortOrder>())).thenReturn(NodeSortConfiguration.default)
        whenever(monitorPublicLinksUseCase()).thenReturn(flowOf(items))

        val nodeUiItems = items.map { node ->
            NodeUiItem<TypedNode>(
                node = node,
                isSelected = false
            )
        }
        whenever(
            nodeUiItemMapper(
                nodeList = items,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.LINKS,
            )
        ).thenReturn(nodeUiItems)
        whenever(
            nodeUiItemMapper(
                nodeList = items,
                existingItems = nodeUiItems,
                nodeSourceType = NodeSourceType.LINKS,
            )
        ).thenReturn(nodeUiItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
    }

    @Test
    fun `test that initial state is set correctly`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()
            assertThat(initialState.isInSelectionMode).isFalse()
            assertThat(initialState.navigateToFolderEvent).isEqualTo(consumed())
            assertThat(initialState.navigateBack).isEqualTo(consumed)
            assertThat(initialState.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initialState.isSelecting).isFalse()
            assertThat(initialState.openedFileNode).isNull()
        }
    }

    @Test
    fun `test that loadLinks populates items correctly`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isLoading).isFalse()
            assertThat(loadedState.items).hasSize(2)
            assertThat(loadedState.items[0].node.id).isEqualTo(NodeId(1L))
            assertThat(loadedState.items[1].node.id).isEqualTo(NodeId(2L))
            assertThat(loadedState.items[0].isSelected).isFalse()
            assertThat(loadedState.items[1].isSelected).isFalse()
        }
    }

    @Test
    fun `test that ItemLongClicked action toggles item selection and updates items state`() =
        runTest {
            val node1 = mock<PublicLinkFolder> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }
            val node2 = mock<PublicLinkFolder> {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Test Node 2"
            }

            setupTestData(listOf(node1, node2))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(LinksAction.ItemLongClicked(nodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isTrue()
                assertThat(updatedState.items[0].isSelected).isTrue()
                assertThat(updatedState.items[1].isSelected).isFalse()
            }
        }

    @Test
    fun `test that ItemClicked action in selection mode toggles item selection`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(LinksAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(LinksAction.ItemClicked(nodeUiItem2))
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isTrue()
            assertThat(updatedState.items[0].isSelected).isTrue()
            assertThat(updatedState.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that ItemClicked action in normal mode navigates to folder`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        val folderNode = mock<TypedFolderNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = folderNode,
            isSelected = false
        )

        underTest.processAction(LinksAction.ItemClicked(nodeUiItem))

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.navigateToFolderEvent).isEqualTo(triggered(folderNode))
        }
    }

    @Test
    fun `test that ItemClicked action in normal mode opens file node`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        val fileNode = mock<TypedFileNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = fileNode,
            isSelected = false
        )

        underTest.processAction(LinksAction.ItemClicked(nodeUiItem))

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.openedFileNode).isEqualTo(fileNode)
        }
    }

    @Test
    fun `test that DeselectAllItems action deselects all items in state`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify initial state
        val loadedState = underTest.uiState.value
        assertThat(loadedState.isLoading).isFalse()

        underTest.processAction(LinksAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        underTest.processAction(LinksAction.DeselectAllItems)
        testScheduler.advanceUntilIdle()

        val updatedState = underTest.uiState.value
        assertThat(updatedState.isInSelectionMode).isFalse()
        assertThat(updatedState.isSelecting).isFalse()
        assertThat(updatedState.items[0].isSelected).isFalse()
        assertThat(updatedState.items[1].isSelected).isFalse()
    }

    @Test
    fun `test that DeselectAllItems action does nothing when no items are selected`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.processAction(LinksAction.DeselectAllItems)

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items[0].isSelected).isFalse()
        }
    }

    @Test
    fun `test that SelectAllItems selects all items`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify we're in loaded state
        val loadedState = underTest.uiState.value
        assertThat(loadedState.isLoading).isFalse()

        underTest.processAction(LinksAction.SelectAllItems)
        // Advance the coroutine to let it execute
        testScheduler.advanceUntilIdle()

        val stateAfterSelectAll = underTest.uiState.value
        // Verify that isSelecting is false and all items are selected
        assertThat(stateAfterSelectAll.isSelecting).isFalse()
        assertThat(stateAfterSelectAll.isInSelectionMode).isTrue()
        assertThat(stateAfterSelectAll.items[0].isSelected).isTrue()
        assertThat(stateAfterSelectAll.items[1].isSelected).isTrue()
    }

    @Test
    fun `test that SelectAllItems does nothing when no items exist`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.processAction(LinksAction.SelectAllItems)

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items).isEmpty()
        }
    }

    @Test
    fun `test that toggleItemSelection removes item from selection when already selected`() =
        runTest {
            val node1 = mock<PublicLinkFolder> {
                on { id } doReturn NodeId(1L)
            }

            setupTestData(listOf(node1))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(LinksAction.ItemLongClicked(nodeUiItem1))
                val stateAfterSelection = awaitItem()

                val updatedNodeUiItem1 = stateAfterSelection.items[0]
                underTest.processAction(LinksAction.ItemLongClicked(updatedNodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isFalse()
                assertThat(updatedState.items[0].isSelected).isFalse()
            }
        }

    @Test
    fun `test that isInSelectionMode is true when items are selected`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(LinksAction.ItemLongClicked(nodeUiItem1))
            val state = awaitItem()

            assertThat(state.isInSelectionMode).isTrue()
        }
    }

    @Test
    fun `test that isInSelectionMode is false when no items are selected`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isInSelectionMode).isFalse()
        }
    }

    @Test
    fun `test that multiple items can be selected`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(LinksAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(LinksAction.ItemLongClicked(nodeUiItem2))
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isTrue()
            assertThat(updatedState.items[0].isSelected).isTrue()
            assertThat(updatedState.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that NavigateToFolderEventConsumed action consumes the navigation event`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        val folderNode = mock<TypedFolderNode>()
        val nodeUiItem = mock<NodeUiItem<TypedNode>> {
            on { node } doReturn folderNode
        }

        underTest.processAction(LinksAction.ItemClicked(nodeUiItem))
        underTest.uiState.test {
            val stateAfterClick = awaitItem()
            underTest.processAction(LinksAction.NavigateToFolderEventConsumed)
            val stateAfterConsume = awaitItem()

            assertThat(stateAfterClick.navigateToFolderEvent).isEqualTo(triggered(folderNode))
            assertThat(stateAfterConsume.navigateToFolderEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that OpenedFileNodeHandled action clears opened file node`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        val fileNode = mock<TypedFileNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = fileNode,
            isSelected = false
        )

        underTest.processAction(LinksAction.ItemClicked(nodeUiItem))
        underTest.uiState.test {
            val stateAfterClick = awaitItem()
            underTest.processAction(LinksAction.OpenedFileNodeHandled)
            val stateAfterHandled = awaitItem()

            assertThat(stateAfterClick.openedFileNode).isEqualTo(fileNode)
            assertThat(stateAfterHandled.openedFileNode).isNull()
        }
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from LIST to GRID`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.processAction(LinksAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setViewTypeUseCase).invoke(ViewType.GRID)
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from GRID to LIST`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val stateWithGrid = awaitItem() // State after monitorViewType updates
            assertThat(stateWithGrid.currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }

        underTest.processAction(LinksAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setViewTypeUseCase).invoke(ViewType.LIST)
    }

    @Test
    fun `test that monitorViewType updates currentViewType in ui state`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val updatedState = awaitItem() // State after monitorViewType flow emits
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    @Test
    fun `test that monitorViewType handles GRID view type correctly`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val updatedState = awaitItem() // State after monitorViewType flow emits
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getCloudSortOrder updates selectedSort in UI state on success`() = runTest {
        setupTestData(emptyList())
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC
        val expectedSortConfiguration = NodeSortConfiguration.default

        whenever(getCloudSortOrder()).thenReturn(expectedSortOrder)
        whenever(nodeSortConfigurationUiMapper(expectedSortOrder)).thenReturn(
            expectedSortConfiguration
        )

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedSortConfiguration).isEqualTo(expectedSortConfiguration)
            assertThat(state.selectedSortOrder).isEqualTo(expectedSortOrder)
        }
    }

    @Test
    fun `test that setSortOrder calls use case and refetches sort order`() = runTest {
        setupTestData(emptyList())
        val sortConfiguration =
            NodeSortConfiguration(NodeSortOption.Size, SortDirection.Ascending)
        val expectedSortOrder = SortOrder.ORDER_SIZE_ASC

        whenever(nodeSortConfigurationUiMapper(sortConfiguration)).thenReturn(expectedSortOrder)
        whenever(getCloudSortOrder()).thenReturn(expectedSortOrder)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.setSortOrder(sortConfiguration)
        advanceUntilIdle()

        // Verify that getCloudSortOrder was called at least twice:
        // 1. During initialization
        // 2. After setting the sort order (refetch)
        verify(getCloudSortOrder, times(2)).invoke()
        verify(setCloudSortOrder).invoke(expectedSortOrder)
    }

    @Test
    fun `test that selectedItemsCount returns correct count`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.selectedItemsCount).isEqualTo(0)

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(LinksAction.ItemLongClicked(nodeUiItem1))
            val stateAfterSelection = awaitItem()

            assertThat(stateAfterSelection.selectedItemsCount).isEqualTo(1)
        }
    }

    @Test
    fun `test that isEmpty returns true when no items and not loading`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isEmpty).isTrue()
        }
    }

    @Test
    fun `test that isEmpty returns false when loading`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isEmpty).isFalse()
        }
    }

    @Test
    fun `test that isEmpty returns false when items exist`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isEmpty).isFalse()
        }
    }

    @Test
    fun `test that selectedNodes returns correct nodes`() = runTest {
        val node1 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<PublicLinkFolder> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.selectedNodes).isEmpty()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(LinksAction.ItemLongClicked(nodeUiItem1))
            val stateAfterSelection = awaitItem()

            assertThat(stateAfterSelection.selectedNodes).hasSize(1)
            assertThat(stateAfterSelection.selectedNodes[0].id).isEqualTo(NodeId(1L))
        }
    }
}
