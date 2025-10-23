package mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth
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
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFileNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.shares.GetOutgoingSharesChildrenNodeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model.OutgoingSharesAction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
class OutgoingSharesViewModelTest {
    private val getOutgoingSharesChildrenNodeUseCase: GetOutgoingSharesChildrenNodeUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val setCloudSortOrder: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val folderNodeHandle = 123L
    private val folderNodeId = NodeId(folderNodeHandle)

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
            getOutgoingSharesChildrenNodeUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            monitorNodeUpdatesByIdUseCase,
            nodeUiItemMapper,
            getCloudSortOrder,
            setCloudSortOrder,
            nodeSortConfigurationUiMapper,
            getContactVerificationWarningUseCase
        )
    }

    private fun createViewModel() = OutgoingSharesViewModel(
        getOutgoingSharesChildrenNodeUseCase = getOutgoingSharesChildrenNodeUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        getCloudSortOrderUseCase = getCloudSortOrder,
        setCloudSortOrderUseCase = setCloudSortOrder,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
        getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
    )

    private suspend fun setupTestData(items: List<ShareNode>, nodeName: String = "Test Folder") {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(NodeSortConfiguration.Companion.default)
        whenever(getOutgoingSharesChildrenNodeUseCase(-1L)).thenReturn(items)

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
                nodeSourceType = NodeSourceType.OUTGOING_SHARES,
            )
        ).thenReturn(nodeUiItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(-1L),
                NodeSourceType.OUTGOING_SHARES
            )
        ).thenReturn(flowOf())

        // Setup contact verification mock
        whenever(getContactVerificationWarningUseCase()).thenReturn(false)
    }

    @Test
    fun `test that initial state is set correctly`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            Truth.assertThat(initialState.currentFolderId).isEqualTo(NodeId(-1L))
            Truth.assertThat(initialState.isLoading).isTrue()
            Truth.assertThat(initialState.items).isEmpty()
            Truth.assertThat(initialState.isInSelectionMode).isFalse()
            Truth.assertThat(initialState.navigateToFolderEvent).isEqualTo(consumed())
            Truth.assertThat(initialState.navigateBack).isEqualTo(consumed)
            Truth.assertThat(initialState.currentViewType).isEqualTo(ViewType.LIST)
            Truth.assertThat(initialState.isSelecting).isFalse()
        }
    }

    @Test
    fun `test that monitorNodeUpdates triggers navigateBack when NodeChanges_Remove is received`() =
        runTest {
            setupTestData(emptyList())
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(-1L),
                    NodeSourceType.OUTGOING_SHARES
                )
            ).thenReturn(flowOf(NodeChanges.Remove))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.navigateBack).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that NavigateBackEventConsumed action consumes the navigate back event`() = runTest {
        setupTestData(emptyList())
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(-1L),
                NodeSourceType.OUTGOING_SHARES
            )
        ).thenReturn(flowOf(NodeChanges.Remove))

        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            awaitItem()
            val stateAfterRemove = awaitItem()
            Truth.assertThat(stateAfterRemove.navigateBack).isEqualTo(triggered)

            underTest.processAction(OutgoingSharesAction.NavigateBackEventConsumed)
            val stateAfterConsume = awaitItem()
            assertThat(stateAfterConsume.navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that loadNodes populates items correctly`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<ShareFileNode> {
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
            val node1 = mock<ShareFileNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }
            val node2 = mock<ShareFileNode> {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Test Node 2"
            }

            setupTestData(listOf(node1, node2))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isTrue()
                assertThat(updatedState.items[0].isSelected).isTrue()
                assertThat(updatedState.items[1].isSelected).isFalse()
            }
        }

    @Test
    fun `test that ItemClicked action in selection mode toggles item selection`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<ShareFileNode> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(OutgoingSharesAction.ItemClicked(nodeUiItem2))
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

        val folderNode = mock<ShareFolderNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = folderNode,
            isSelected = false
        )

        underTest.processAction(OutgoingSharesAction.ItemClicked(nodeUiItem))

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.navigateToFolderEvent).isEqualTo(triggered(folderNode))
        }
    }

    @Test
    fun `test that DeselectAllItems action deselects all items in state`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<ShareFileNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        testScheduler.advanceUntilIdle()

        val loadedState = underTest.uiState.value
        assertThat(loadedState.isLoading).isFalse()

        underTest.processAction(OutgoingSharesAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        underTest.processAction(OutgoingSharesAction.DeselectAllItems)
        testScheduler.advanceUntilIdle()

        val updatedState = underTest.uiState.value
        assertThat(updatedState.isInSelectionMode).isFalse()
        assertThat(updatedState.isSelecting).isFalse()
        assertThat(updatedState.items[0].isSelected).isFalse()
        assertThat(updatedState.items[1].isSelected).isFalse()
    }

    @Test
    fun `test that DeselectAllItems action does nothing when no items are selected`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.processAction(OutgoingSharesAction.DeselectAllItems)

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items[0].isSelected).isFalse()
        }
    }

    @Test
    fun `test that SelectAllItems selects all items`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<ShareFileNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        testScheduler.advanceUntilIdle()

        val loadedState = underTest.uiState.value
        assertThat(loadedState.isLoading).isFalse()

        underTest.processAction(OutgoingSharesAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        val stateAfterSelectAll = underTest.uiState.value
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

            underTest.processAction(OutgoingSharesAction.SelectAllItems)

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items).isEmpty()
        }
    }

    @Test
    fun `test that toggleItemSelection removes item from selection when already selected`() =
        runTest {
            val node1 = mock<ShareFileNode> {
                on { id } doReturn NodeId(1L)
            }

            setupTestData(listOf(node1))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem1))
                val stateAfterSelection = awaitItem()

                val updatedNodeUiItem1 = stateAfterSelection.items[0]
                underTest.processAction(OutgoingSharesAction.ItemLongClicked(updatedNodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isFalse()
                assertThat(updatedState.items[0].isSelected).isFalse()
            }
        }

    @Test
    fun `test that isInSelectionMode is true when items are selected`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem1))
            val state = awaitItem()

            assertThat(state.isInSelectionMode).isTrue()
        }
    }

    @Test
    fun `test that isInSelectionMode is false when no items are selected`() = runTest {
        val node1 = mock<ShareFileNode> {
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
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<ShareFileNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem2))
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
            on { id } doReturn folderNodeId
        }

        underTest.processAction(OutgoingSharesAction.ItemClicked(nodeUiItem))
        underTest.uiState.test {
            val stateAfterClick = awaitItem()
            underTest.processAction(OutgoingSharesAction.NavigateToFolderEventConsumed)
            val stateAfterConsume = awaitItem()

            assertThat(stateAfterClick.navigateToFolderEvent).isEqualTo(triggered(folderNode))
            assertThat(stateAfterConsume.navigateToFolderEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from LIST to GRID`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.processAction(OutgoingSharesAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setViewTypeUseCase).invoke(ViewType.GRID)
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from GRID to LIST`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val stateWithGrid = awaitItem()
            assertThat(stateWithGrid.currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }

        underTest.processAction(OutgoingSharesAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setViewTypeUseCase).invoke(ViewType.LIST)
    }

    @Test
    fun `test that monitorViewType updates currentViewType in ui state`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val updatedState = awaitItem()
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    @Test
    fun `test that monitorViewType handles GRID view type correctly`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val updatedState = awaitItem()
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getCloudSortOrder updates selectedSort in UI state on success`() = runTest {
        setupTestData(emptyList())
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC
        val expectedSortConfiguration = NodeSortConfiguration.Companion.default

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
            NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC

        whenever(nodeSortConfigurationUiMapper(sortConfiguration)).thenReturn(expectedSortOrder)
        whenever(getCloudSortOrder()).thenReturn(expectedSortOrder)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.setSortOrder(sortConfiguration)
        advanceUntilIdle()

        verify(getCloudSortOrder, times(2)).invoke()
        verify(setCloudSortOrder).invoke(expectedSortOrder)
    }

    @Test
    fun `test that selectedItemsCount returns correct count`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<ShareFileNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.selectedItemsCount).isEqualTo(0)

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem1))
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
        val node1 = mock<ShareFileNode> {
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
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<ShareFileNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.selectedNodes).isEmpty()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem1))
            val stateAfterSelection = awaitItem()

            assertThat(stateAfterSelection.selectedNodes).hasSize(1)
            assertThat(stateAfterSelection.selectedNodes[0].id).isEqualTo(NodeId(1L))
        }
    }

    @Test
    fun `test that selectedNodeIds returns correct node ids`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<ShareFileNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.selectedNodeIds).isEmpty()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(OutgoingSharesAction.ItemLongClicked(nodeUiItem1))
            val stateAfterSelection = awaitItem()

            assertThat(stateAfterSelection.selectedNodeIds).hasSize(1)
            assertThat(stateAfterSelection.selectedNodeIds[0]).isEqualTo(NodeId(1L))
        }
    }

    // Contact Verification Tests

    @Test
    fun `test that contact verification is checked when loading nodes with feature enabled`() =
        runTest {
            val node1 = mock<ShareFileNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }

            whenever(getContactVerificationWarningUseCase()).thenReturn(true)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                nodeSortConfigurationUiMapper(
                    SortOrder.ORDER_DEFAULT_ASC,
                )
            ).thenReturn(
                NodeSortConfiguration.default
            )
            whenever(getOutgoingSharesChildrenNodeUseCase(-1L)).thenReturn(listOf(node1))

            val nodeUiItems = listOf(NodeUiItem<TypedNode>(node = node1, isSelected = false))
            whenever(
                nodeUiItemMapper(
                    nodeList = listOf(node1),
                    existingItems = emptyList(),
                    nodeSourceType = NodeSourceType.OUTGOING_SHARES,
                    isContactVerificationOn = true,
                )
            ).thenReturn(nodeUiItems)
            whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(-1L),
                    NodeSourceType.OUTGOING_SHARES
                )
            ).thenReturn(flowOf())

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.items).hasSize(1)
            }
        }

    @Test
    fun `test that contact verification is checked when loading nodes with feature disabled`() =
        runTest {
            val node1 = mock<ShareFileNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }

            whenever(getContactVerificationWarningUseCase()).thenReturn(false)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                nodeSortConfigurationUiMapper(
                    SortOrder.ORDER_DEFAULT_ASC
                )
            ).thenReturn(
                NodeSortConfiguration.default
            )
            whenever(getOutgoingSharesChildrenNodeUseCase(-1L)).thenReturn(listOf(node1))

            val nodeUiItems = listOf(NodeUiItem<TypedNode>(node = node1, isSelected = false))
            whenever(
                nodeUiItemMapper(
                    nodeList = listOf(node1),
                    existingItems = emptyList(),
                    nodeSourceType = NodeSourceType.OUTGOING_SHARES,
                    isContactVerificationOn = false,
                )
            ).thenReturn(nodeUiItems)
            whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(-1L),
                    NodeSourceType.OUTGOING_SHARES
                )
            ).thenReturn(flowOf())

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.items).hasSize(1)
            }
        }

    @Test
    fun `test that contact verification exception is handled gracefully`() = runTest {
        val node1 = mock<ShareFileNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }

        whenever(getContactVerificationWarningUseCase()).thenThrow(RuntimeException("Test exception"))
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            nodeSortConfigurationUiMapper(
                SortOrder.ORDER_DEFAULT_ASC
            )
        ).thenReturn(NodeSortConfiguration.default)
        whenever(getOutgoingSharesChildrenNodeUseCase(-1L)).thenReturn(listOf(node1))

        val nodeUiItems = listOf(NodeUiItem<TypedNode>(node = node1, isSelected = false))
        whenever(
            nodeUiItemMapper(
                nodeList = listOf(node1),
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.OUTGOING_SHARES,
                isContactVerificationOn = false,
            )
        ).thenReturn(nodeUiItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(-1L),
                NodeSourceType.OUTGOING_SHARES
            )
        ).thenReturn(flowOf())

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            // Should not crash and load items with default verification value (false)
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).hasSize(1)
        }
    }
}