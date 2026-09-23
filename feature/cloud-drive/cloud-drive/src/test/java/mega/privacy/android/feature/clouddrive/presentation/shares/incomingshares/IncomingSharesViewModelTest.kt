package mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import mega.privacy.android.domain.entity.preference.FolderPreferenceKeys
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorOthersSortOrderUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingSharesChildrenNodeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class IncomingSharesViewModelTest {
    private val getIncomingSharesChildrenNodeUseCase: GetIncomingSharesChildrenNodeUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val monitorFolderViewTypeUseCase: MonitorFolderViewTypeUseCase = mock()
    private val setFolderViewTypeUseCase: SetFolderViewTypeUseCase = mock()
    private val monitorFolderSortOrderUseCase: MonitorFolderSortOrderUseCase = mock()
    private val setFolderSortOrderUseCase: SetFolderSortOrderUseCase = mock()
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase = mock()
    private val nodeViewItemMapper: NodeViewItemMapper = mock()
    private val monitorOthersSortOrderUseCase: MonitorOthersSortOrderUseCase = mock()
    private val setOthersSortOrder: SetOthersSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val snackbarEventQueue: SnackbarEventQueue = mock()
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
            getIncomingSharesChildrenNodeUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            monitorFolderViewTypeUseCase,
            setFolderViewTypeUseCase,
            monitorFolderSortOrderUseCase,
            setFolderSortOrderUseCase,
            monitorNodeUpdatesByIdUseCase,
            nodeViewItemMapper,
            monitorOthersSortOrderUseCase,
            setOthersSortOrder,
            nodeSortConfigurationUiMapper,
            getContactVerificationWarningUseCase,
            snackbarEventQueue,
        )
    }

    private fun createViewModel(nodeHandle: Long = folderNodeHandle) = IncomingSharesViewModel(
        getIncomingSharesChildrenNodeUseCase = getIncomingSharesChildrenNodeUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        monitorFolderViewTypeUseCase = monitorFolderViewTypeUseCase,
        setFolderViewTypeUseCase = setFolderViewTypeUseCase,
        monitorFolderSortOrderUseCase = monitorFolderSortOrderUseCase,
        setFolderSortOrderUseCase = setFolderSortOrderUseCase,
        monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
        nodeViewItemMapper = nodeViewItemMapper,
        monitorOthersSortOrderUseCase = monitorOthersSortOrderUseCase,
        setOthersSortOrder = setOthersSortOrder,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
        getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
        snackbarEventQueue = snackbarEventQueue,
    )

    private suspend fun setupTestData(items: List<ShareNode>) {
        whenever(monitorOthersSortOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(
            NodeSortConfiguration.default
        )
        whenever(monitorFolderViewTypeUseCase(any(), any()))
            .thenAnswer { it.getArgument<Flow<ViewType>>(1) }
        whenever(monitorFolderSortOrderUseCase(any(), any()))
            .thenAnswer { it.getArgument<Flow<SortOrder>>(1) }
        whenever(getIncomingSharesChildrenNodeUseCase(eq(-1L), anyOrNull())).thenReturn(items)

        val nodeItems = items.map { node -> NodeViewItem<TypedNode>(node = node) }
        whenever(
            nodeViewItemMapper(
                nodeList = items,
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = false,
                highlightedNodeId = null,
                highlightedNames = null,
                isHiddenNodesEnabled = false,
            )
        ).thenReturn(nodeItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(-1L),
                NodeSourceType.INCOMING_SHARES
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
            assertThat(initialState.currentFolderId).isEqualTo(NodeId(-1L))
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()
            assertThat(initialState.navigateBack).isEqualTo(consumed)
            assertThat(initialState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    @Test
    fun `test that monitorNodeUpdates triggers navigateBack when NodeChanges_Remove is received`() =
        runTest {
            setupTestData(emptyList())
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(-1L),
                    NodeSourceType.INCOMING_SHARES
                )
            ).thenReturn(flowOf(NodeChanges.Remove))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val updatedState = awaitItem() // State after monitorNodeUpdates processes Remove
                assertThat(updatedState.navigateBack).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that NavigateBackEventConsumed action consumes the navigate back event`() = runTest {
        setupTestData(emptyList())
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(-1L),
                NodeSourceType.INCOMING_SHARES
            )
        ).thenReturn(flowOf(NodeChanges.Remove))

        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            awaitItem() // State after initial loadNodes
            val stateAfterRemove = awaitItem() // State after Remove triggers navigateBack
            assertThat(stateAfterRemove.navigateBack).isEqualTo(triggered)

            underTest.processAction(IncomingSharesAction.NavigateBackEventConsumed)
            val stateAfterConsume = awaitItem() // State after consuming the event
            assertThat(stateAfterConsume.navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that loading stops and an error message is queued when loadNodes fails`() = runTest {
        setupTestData(emptyList())
        whenever(getIncomingSharesChildrenNodeUseCase(eq(-1L), anyOrNull()))
            .thenThrow(RuntimeException("Failed to fetch shares"))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).isEmpty()
        }
        verify(snackbarEventQueue).queueMessage(sharedR.string.general_request_failed_message)
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
        }
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from LIST to GRID`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.processAction(IncomingSharesAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setFolderViewTypeUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.INCOMING_SHARES),
            viewType = eq(ViewType.GRID),
            currentSortOrder = any(),
            orElse = any(),
        )
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

        underTest.processAction(IncomingSharesAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setFolderViewTypeUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.INCOMING_SHARES),
            viewType = eq(ViewType.LIST),
            currentSortOrder = any(),
            orElse = any(),
        )
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
    fun `test that monitorSortOrder updates selectedSort in UI state`() = runTest {
        setupTestData(emptyList())
        val expectedSortOrder = SortOrder.ORDER_SIZE_DESC
        val expectedSortConfiguration = NodeSortConfiguration.default

        whenever(monitorOthersSortOrderUseCase()).thenReturn(flowOf(expectedSortOrder))
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
    fun `test that setSortOrder writes via setFolderSortOrderUseCase`() = runTest {
        setupTestData(emptyList())
        val sortConfiguration =
            NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC

        whenever(nodeSortConfigurationUiMapper(sortConfiguration)).thenReturn(expectedSortOrder)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.setSortOrder(sortConfiguration)
        advanceUntilIdle()

        verify(setFolderSortOrderUseCase).invoke(
            folderKey = eq(FolderPreferenceKeys.INCOMING_SHARES),
            sortOrder = eq(expectedSortOrder),
            currentViewType = any(),
            orElse = any(),
        )
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
    fun `test that contact verification is checked when loading nodes with feature enabled`() =
        runTest {
            val node1 = mock<ShareFileNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }

            whenever(getContactVerificationWarningUseCase()).thenReturn(true)
            whenever(monitorOthersSortOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
            whenever(monitorFolderViewTypeUseCase(any(), any()))
                .thenAnswer { it.getArgument<Flow<ViewType>>(1) }
            whenever(monitorFolderSortOrderUseCase(any(), any()))
                .thenAnswer { it.getArgument<Flow<SortOrder>>(1) }
            whenever(
                nodeSortConfigurationUiMapper(
                    SortOrder.ORDER_DEFAULT_ASC,
                )
            ).thenReturn(
                NodeSortConfiguration.default
            )
            whenever(getIncomingSharesChildrenNodeUseCase(eq(-1L), anyOrNull())).thenReturn(listOf(node1))

            val nodeUiItems = listOf(NodeViewItem<TypedNode>(node = node1))
            whenever(
                nodeViewItemMapper(
                    nodeList = listOf(node1),
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                    isContactVerificationOn = true,
                    highlightedNodeId = null,
                    highlightedNames = null,
                    isHiddenNodesEnabled = false,
                )
            ).thenReturn(nodeUiItems)
            whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(-1L),
                    NodeSourceType.INCOMING_SHARES
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
            whenever(monitorOthersSortOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
            whenever(monitorFolderViewTypeUseCase(any(), any()))
                .thenAnswer { it.getArgument<Flow<ViewType>>(1) }
            whenever(monitorFolderSortOrderUseCase(any(), any()))
                .thenAnswer { it.getArgument<Flow<SortOrder>>(1) }
            whenever(
                nodeSortConfigurationUiMapper(
                    SortOrder.ORDER_DEFAULT_ASC
                )
            ).thenReturn(
                NodeSortConfiguration.default
            )
            whenever(getIncomingSharesChildrenNodeUseCase(eq(-1L), anyOrNull())).thenReturn(listOf(node1))

            val nodeUiItems = listOf(NodeViewItem<TypedNode>(node = node1))
            whenever(
                nodeViewItemMapper(
                    nodeList = listOf(node1),
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                    isContactVerificationOn = false,
                    highlightedNodeId = null,
                    highlightedNames = null,
                    isHiddenNodesEnabled = false,
                )
            ).thenReturn(nodeUiItems)
            whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(-1L),
                    NodeSourceType.INCOMING_SHARES
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
        whenever(monitorOthersSortOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
        whenever(monitorFolderViewTypeUseCase(any(), any()))
            .thenAnswer { it.getArgument<Flow<ViewType>>(1) }
        whenever(monitorFolderSortOrderUseCase(any(), any()))
            .thenAnswer { it.getArgument<Flow<SortOrder>>(1) }
        whenever(
            nodeSortConfigurationUiMapper(
                SortOrder.ORDER_DEFAULT_ASC
            )
        ).thenReturn(NodeSortConfiguration.default)
        whenever(getIncomingSharesChildrenNodeUseCase(eq(-1L), anyOrNull())).thenReturn(listOf(node1))

        val nodeUiItems = listOf(NodeViewItem<TypedNode>(node = node1))
        whenever(
            nodeViewItemMapper(
                nodeList = listOf(node1),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = false,
                highlightedNodeId = null,
                highlightedNames = null,
                isHiddenNodesEnabled = false,
            )
        ).thenReturn(nodeUiItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(-1L),
                NodeSourceType.INCOMING_SHARES
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