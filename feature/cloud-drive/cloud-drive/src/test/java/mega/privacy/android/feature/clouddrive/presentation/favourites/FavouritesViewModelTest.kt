package mega.privacy.android.feature.clouddrive.presentation.favourites

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FavouritesViewModelTest {
    companion object {
        private val testScheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(testScheduler))
    }

    private val getAllFavoritesUseCase: GetAllFavoritesUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val setCloudSortOrderUseCase: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    @AfterEach
    fun tearDown() {
        reset(
            getAllFavoritesUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            nodeUiItemMapper,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase,
            getFeatureFlagValueUseCase
        )
    }

    private fun createViewModel() = FavouritesViewModel(
        getAllFavoritesUseCase = getAllFavoritesUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        setCloudSortOrderUseCase = setCloudSortOrderUseCase,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
        monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
    )

    private suspend fun setupTestData(items: List<TypedNode>) {
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(
            NodeSortConfiguration.default
        )
        whenever(getAllFavoritesUseCase()).thenReturn(flowOf(items))

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
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                highlightedNames = null,
            )
        ).thenReturn(nodeUiItems)
        whenever(
            nodeUiItemMapper(
                nodeList = items,
                existingItems = nodeUiItems,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                highlightedNames = null,
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
            assertThat(initialState.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initialState.openedFileNode).isNull()
        }
    }

    @Test
    fun `test that monitorFavourites populates items correctly`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedFolderNode> {
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
            val node1 = mock<TypedFolderNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }
            val node2 = mock<TypedFolderNode> {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Test Node 2"
            }

            setupTestData(listOf(node1, node2))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isTrue()
                assertThat(updatedState.items[0].isSelected).isTrue()
                assertThat(updatedState.items[1].isSelected).isFalse()
            }
        }

    @Test
    fun `test that ItemClicked action in selection mode toggles item selection`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(FavouritesAction.ItemClicked(nodeUiItem2))
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

        underTest.processAction(FavouritesAction.ItemClicked(nodeUiItem))

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

        underTest.processAction(FavouritesAction.ItemClicked(nodeUiItem))

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.openedFileNode).isEqualTo(fileNode)
        }
    }

    @Test
    fun `test that DeselectAllItems action deselects all items in state`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify initial state
        val loadedState = underTest.uiState.value
        assertThat(loadedState.isLoading).isFalse()

        underTest.processAction(FavouritesAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        underTest.processAction(FavouritesAction.DeselectAllItems)
        testScheduler.advanceUntilIdle()

        val updatedState = underTest.uiState.value
        assertThat(updatedState.isInSelectionMode).isFalse()
        assertThat(updatedState.items[0].isSelected).isFalse()
        assertThat(updatedState.items[1].isSelected).isFalse()
    }

    @Test
    fun `test that DeselectAllItems action does nothing when no items are selected`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.processAction(FavouritesAction.DeselectAllItems)

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items[0].isSelected).isFalse()
        }
    }

    @Test
    fun `test that SelectAllItems selects all items`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify we're in loaded state
        val loadedState = underTest.uiState.value
        assertThat(loadedState.isLoading).isFalse()

        underTest.processAction(FavouritesAction.SelectAllItems)
        // Advance the coroutine to let it execute
        testScheduler.advanceUntilIdle()

        val stateAfterSelectAll = underTest.uiState.value
        // Verify that all items are selected
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

            underTest.processAction(FavouritesAction.SelectAllItems)

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items).isEmpty()
        }
    }

    @Test
    fun `test that toggleItemSelection removes item from selection when already selected`() =
        runTest {
            val node1 = mock<TypedFolderNode> {
                on { id } doReturn NodeId(1L)
            }

            setupTestData(listOf(node1))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem1))
                val stateAfterSelection = awaitItem()

                val updatedNodeUiItem1 = stateAfterSelection.items[0]
                underTest.processAction(FavouritesAction.ItemLongClicked(updatedNodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isFalse()
                assertThat(updatedState.items[0].isSelected).isFalse()
            }
        }

    @Test
    fun `test that isInSelectionMode is true when items are selected`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem1))
            val state = awaitItem()

            assertThat(state.isInSelectionMode).isTrue()
        }
    }

    @Test
    fun `test that isInSelectionMode is false when no items are selected`() = runTest {
        val node1 = mock<TypedFolderNode> {
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
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem2))
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

        underTest.processAction(FavouritesAction.ItemClicked(nodeUiItem))
        underTest.uiState.test {
            val stateAfterClick = awaitItem()
            underTest.processAction(FavouritesAction.NavigateToFolderEventConsumed)
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

        underTest.processAction(FavouritesAction.ItemClicked(nodeUiItem))
        underTest.uiState.test {
            val stateAfterClick = awaitItem()
            underTest.processAction(FavouritesAction.OpenedFileNodeHandled)
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

        underTest.processAction(FavouritesAction.ChangeViewTypeClicked)
        testScheduler.advanceUntilIdle()

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

        underTest.processAction(FavouritesAction.ChangeViewTypeClicked)
        testScheduler.advanceUntilIdle()

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
    fun `test that monitorCloudSortOrder updates selectedSort in UI state on success`() = runTest {
        setupTestData(emptyList())
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC
        val expectedSortConfiguration = NodeSortConfiguration.default

        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(expectedSortOrder))
        whenever(nodeSortConfigurationUiMapper(expectedSortOrder)).thenReturn(
            expectedSortConfiguration
        )

        val underTest = createViewModel()
        testScheduler.advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedSortConfiguration).isEqualTo(expectedSortConfiguration)
            assertThat(state.selectedSortOrder).isEqualTo(expectedSortOrder)
        }
    }

    @Test
    fun `test that setCloudSortOrder calls use case`() = runTest {
        setupTestData(emptyList())
        val sortConfiguration =
            NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC

        whenever(nodeSortConfigurationUiMapper(sortConfiguration)).thenReturn(expectedSortOrder)
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(expectedSortOrder))

        val underTest = createViewModel()
        testScheduler.advanceUntilIdle()

        underTest.setCloudSortOrder(sortConfiguration)
        testScheduler.advanceUntilIdle()

        verify(setCloudSortOrderUseCase).invoke(expectedSortOrder)
    }

    @Test
    fun `test that selectedItemsCount returns correct count`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.selectedItemsCount).isEqualTo(0)

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem1))
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
        val node1 = mock<TypedFolderNode> {
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
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.selectedNodes).isEmpty()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem1))
            val stateAfterSelection = awaitItem()

            assertThat(stateAfterSelection.selectedNodes).hasSize(1)
            assertThat(stateAfterSelection.selectedNodes[0].id).isEqualTo(NodeId(1L))
        }
    }

    @Test
    fun `test that isAllSelected returns true when all items are selected`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        testScheduler.advanceUntilIdle()

        underTest.processAction(FavouritesAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.isAllSelected).isTrue()
    }

    @Test
    fun `test that isAllSelected returns false when not all items are selected`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        testScheduler.advanceUntilIdle()

        val loadedState = underTest.uiState.value
        val nodeUiItem1 = loadedState.items[0]
        underTest.processAction(FavouritesAction.ItemLongClicked(nodeUiItem1))
        testScheduler.advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.isAllSelected).isFalse()
    }

    @Test
    fun `test that monitorCloudSortOrder calls loadNodes when sort order changes`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        whenever(monitorSortCloudOrderUseCase()).thenReturn(
            flowOf(SortOrder.ORDER_DEFAULT_ASC, SortOrder.ORDER_MODIFICATION_DESC)
        )
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(
            NodeSortConfiguration.default
        )
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_MODIFICATION_DESC)).thenReturn(
            NodeSortConfiguration(NodeSortOption.Modified, SortDirection.Descending)
        )

        val underTest = createViewModel()
        testScheduler.advanceUntilIdle()

        // Verify that getAllFavoritesUseCase was called multiple times:
        // 1. From monitorFavourites() (continuous collection)
        // 2. From loadNodes() when first sort order is emitted
        // 3. From loadNodes() when second sort order is emitted
        verify(getAllFavoritesUseCase, times(3)).invoke()
    }

    @Test
    fun `test that monitorCloudSortOrder filters out null values`() = runTest {
        setupTestData(emptyList())
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(null, SortOrder.ORDER_DEFAULT_ASC))
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(
            NodeSortConfiguration.default
        )

        val underTest = createViewModel()
        testScheduler.advanceUntilIdle()

        // Wait for all state updates to complete
        testScheduler.advanceUntilIdle()

        val finalState = underTest.uiState.value
        // Should only update when non-null value is received (null is filtered out)
        assertThat(finalState.selectedSortOrder).isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
        assertThat(finalState.selectedSortConfiguration).isEqualTo(NodeSortConfiguration.default)
    }

    @Test
    fun `test that monitorFavourites updates items when favourites change`() = runTest {
        val node1 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedFolderNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1))
        whenever(getAllFavoritesUseCase()).thenReturn(
            flowOf(listOf(node1), listOf(node1, node2))
        )

        val nodeUiItems1: List<NodeUiItem<TypedNode>> = listOf(
            NodeUiItem(node = node1, isSelected = false)
        )
        val nodeUiItems2: List<NodeUiItem<TypedNode>> = listOf(
            NodeUiItem(node = node1, isSelected = false),
            NodeUiItem(node = node2, isSelected = false)
        )

        whenever(
            nodeUiItemMapper(
                nodeList = listOf(node1),
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                highlightedNames = null,
            )
        ).thenReturn(nodeUiItems1)
        whenever(
            nodeUiItemMapper(
                nodeList = listOf(node1, node2),
                existingItems = nodeUiItems1,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                highlightedNames = null,
            )
        ).thenReturn(nodeUiItems2)

        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val firstState = awaitItem() // After first emission
            assertThat(firstState.items).hasSize(1)

            val secondState = awaitItem() // After second emission
            assertThat(secondState.items).hasSize(2)
        }
    }

    @Test
    fun `test that isSearchRevampEnabled is updated when feature flag is enabled`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(AppFeatures.SearchRevamp)).thenReturn(true)

        val underTest = createViewModel()
        testScheduler.advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isSearchRevampEnabled).isTrue()
        }
    }
}
