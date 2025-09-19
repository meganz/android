package mega.privacy.android.feature.clouddrive.presentation.clouddrive


import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
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
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.components.banners.StorageCapacityMapper
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaCapacity
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeNameByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.MonitorAlmostFullStorageBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.SetAlmostFullStorageBannerClosingTimestampUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.navigation.destination.CloudDrive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CloudDriveViewModelTest {
    private val getNodeNameByIdUseCase: GetNodeNameByIdUseCase = mock()
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase = mock()
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val scannerHandler: ScannerHandler = mock()
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase = mock()
    private val getCloudSortOrderUseCase: GetCloudSortOrder = mock()
    private val setCloudSortOrderUseCase: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val storageCapacityMapper: StorageCapacityMapper = mock()
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase = mock()
    private val monitorAlmostFullStorageBannerVisibilityUseCase: MonitorAlmostFullStorageBannerVisibilityUseCase =
        mock()
    private val setAlmostFullStorageBannerClosingTimestampUseCase: SetAlmostFullStorageBannerClosingTimestampUseCase =
        mock()
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
            getNodeNameByIdUseCase,
            getFileBrowserNodeChildrenUseCase,
            getNodesByIdInChunkUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            getFeatureFlagValueUseCase,
            monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorNodeUpdatesByIdUseCase,
            nodeUiItemMapper,
            scannerHandler,
            getRootNodeIdUseCase,
            getCloudSortOrderUseCase,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            storageCapacityMapper,
            monitorStorageStateUseCase,
            monitorAlmostFullStorageBannerVisibilityUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase
        )
    }

    private fun createViewModel(nodeHandle: Long = folderNodeHandle) = CloudDriveViewModel(
        getNodeNameByIdUseCase = getNodeNameByIdUseCase,
        getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
        monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
        monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        scannerHandler = scannerHandler,
        getRootNodeIdUseCase = getRootNodeIdUseCase,
        getNodesByIdInChunkUseCase = getNodesByIdInChunkUseCase,
        getCloudSortOrderUseCase = getCloudSortOrderUseCase,
        setCloudSortOrderUseCase = setCloudSortOrderUseCase,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
        storageCapacityMapper = storageCapacityMapper,
        monitorStorageStateUseCase = monitorStorageStateUseCase,
        monitorAlmostFullStorageBannerVisibilityUseCase = monitorAlmostFullStorageBannerVisibilityUseCase,
        setAlmostFullStorageBannerClosingTimestampUseCase = setAlmostFullStorageBannerClosingTimestampUseCase,
        savedStateHandle = SavedStateHandle.Companion.invoke(
            route = CloudDrive(nodeHandle)
        ),
    )

    private suspend fun setupTestData(items: List<TypedNode>) {
        whenever(getCloudSortOrderUseCase()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(nodeSortConfigurationUiMapper(any<SortOrder>())).thenReturn(NodeSortConfiguration.default)
        whenever(getNodeNameByIdUseCase(eq(folderNodeId))).thenReturn("Test folder")
        whenever(getFileBrowserNodeChildrenUseCase(folderNodeHandle)).thenReturn(items)

        // Setup the new chunked use case to return a flow with the items and hasMore flag
        whenever(getNodesByIdInChunkUseCase.invoke(folderNodeId)).thenReturn(
            flowOf(
                Pair(
                    items,
                    false
                )
            )
        )

        val nodeUiItems = items.map { node ->
            NodeUiItem(
                node = node,
                isSelected = false
            )
        }
        whenever(
            nodeUiItemMapper(
                nodeList = items,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                isPublicNodes = false,
                showPublicLinkCreationTime = false,
                highlightedNodeId = null,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ).thenReturn(nodeUiItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)

        // Setup storage monitoring mocks
        whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Green))
        whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(true))
        whenever(storageCapacityMapper(any(), any())).thenReturn(StorageOverQuotaCapacity.DEFAULT)
    }

    private suspend fun setupTestDataWithPartialLoad(items: List<TypedNode>) {
        whenever(getCloudSortOrderUseCase()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(nodeSortConfigurationUiMapper(any<SortOrder>())).thenReturn(NodeSortConfiguration.default)
        whenever(getNodeNameByIdUseCase(eq(folderNodeId))).thenReturn("Test folder")
        whenever(getFileBrowserNodeChildrenUseCase(folderNodeHandle)).thenReturn(items)

        // Setup the new chunked use case to return a flow with the items and hasMore = true (partial load)
        whenever(getNodesByIdInChunkUseCase.invoke(folderNodeId)).thenReturn(
            flowOf(
                Pair(
                    items,
                    true
                )
            )
        )

        val nodeUiItems = items.map { node ->
            NodeUiItem(
                node = node,
                isSelected = false
            )
        }
        whenever(
            nodeUiItemMapper(
                nodeList = items,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                isPublicNodes = false,
                showPublicLinkCreationTime = false,
                highlightedNodeId = null,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ).thenReturn(nodeUiItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)

        // Setup storage monitoring mocks
        whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Green))
        whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(true))
        whenever(storageCapacityMapper(any(), any())).thenReturn(StorageOverQuotaCapacity.DEFAULT)
    }

    @Test
    fun `test that initial state is set correctly`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.currentFolderId).isEqualTo(folderNodeId)
            assertThat(initialState.nodesLoadingState).isEqualTo(NodesLoadingState.Loading)
            assertThat(initialState.isHiddenNodeSettingsLoading).isTrue()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()
            assertThat(initialState.isInSelectionMode).isFalse()
            assertThat(initialState.navigateToFolderEvent).isEqualTo(consumed())
            assertThat(initialState.navigateBack).isEqualTo(consumed)
            assertThat(initialState.showHiddenNodes).isFalse()
            assertThat(initialState.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that title is updated correctly`() = runTest {
        whenever(getNodeNameByIdUseCase(any())).thenReturn("Test Folder2")
        setupTestData(emptyList())
        val underTest = createViewModel()
        advanceUntilIdle()
        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.title).isEqualTo(LocalizedText.Literal("Test Folder2"))
        }
    }

    @Test
    fun `test that monitorNodeUpdates triggers navigateBack when NodeChanges_Remove is received`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf(NodeChanges.Remove))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val updatedState = awaitItem() // State after monitorNodeUpdates processes Remove
                assertThat(updatedState.navigateBack).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that monitorNodeUpdates triggers loadNodes when NodeChanges_Attributes is received`() =
        runTest {
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }
            val node2 = mock<TypedNode> {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Test Node 2"
            }

            setupTestData(listOf(node1, node2))

            // Override the monitorNodeUpdatesByIdUseCase to emit NodeChanges.Attributes
            whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf(NodeChanges.Attributes))

            // Ensure that getFileBrowserNodeChildrenUseCase is mocked for the node update scenario
            // This will be called when getNodeUiItems() is invoked during the node update
            whenever(getFileBrowserNodeChildrenUseCase(folderNodeHandle)).thenReturn(
                listOf(
                    node1,
                    node2
                )
            )

            // Ensure that nodeUiItemMapper is mocked for the node update scenario
            val updatedNodeUiItems = listOf(
                NodeUiItem(node = node1, isSelected = false),
                NodeUiItem(node = node2, isSelected = false)
            )
            whenever(
                nodeUiItemMapper(
                    nodeList = listOf(node1, node2),
                    existingItems = listOf(
                        NodeUiItem(node = node1, isSelected = false),
                        NodeUiItem(node = node2, isSelected = false)
                    ),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    isPublicNodes = false,
                    showPublicLinkCreationTime = false,
                    highlightedNodeId = null,
                    highlightedNames = null,
                    isContactVerificationOn = false,
                )
            ).thenReturn(updatedNodeUiItems)

            val underTest = createViewModel()

            // Wait for initial loading to complete
            underTest.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.nodesLoadingState).isEqualTo(NodesLoadingState.Loading)

                val loadedState = awaitItem()
                assertThat(loadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
                assertThat(loadedState.items).hasSize(2)

                // Wait for hidden nodes loading to complete
                val finalState = awaitItem()
                assertThat(finalState.isHiddenNodeSettingsLoading).isFalse()
                assertThat(finalState.isLoading).isFalse()
            }

            // Now wait for the node update to be processed
            advanceUntilIdle()

            // Verify that getFileBrowserNodeChildrenUseCase was called for the node update
            // The call should happen when NodeChanges.Attributes is processed
            verify(getFileBrowserNodeChildrenUseCase).invoke(folderNodeHandle)
        }

    @Test
    fun `test that monitorNodeUpdates handles multiple NodeChanges correctly`() = runTest {
        setupTestData(emptyList())
        val nodeChangesFlow = flowOf(NodeChanges.Attributes, NodeChanges.Remove)
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(nodeChangesFlow)

        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            awaitItem() // State after initial loadNodes
            // Due to collectLatest, only the last Remove will be processed
            val finalState = awaitItem() // State after Remove triggers navigateBack
            assertThat(finalState.navigateBack).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that monitorNodeUpdates handles rapid NodeChanges with conflate`() = runTest {
        setupTestData(emptyList())
        val nodeChangesFlow = flowOf(
            NodeChanges.Attributes,
            NodeChanges.Attributes,
            NodeChanges.Attributes,
            NodeChanges.Remove
        )
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(nodeChangesFlow)

        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            awaitItem() // State after initial loadNodes
            awaitItem() // State after loading hidden nodes
            // Due to collectLatest, only the last Remove will be processed
            val finalState = awaitItem() // State after Remove triggers navigateBack
            assertThat(finalState.navigateBack).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that monitorNodeUpdates does not trigger navigateBack for Attributes`() = runTest {
        setupTestData(emptyList())
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf(NodeChanges.Attributes))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val updatedState = awaitItem() // State after monitorNodeUpdates processes Attributes
            assertThat(updatedState.navigateBack).isEqualTo(consumed)
        }
    }


    @Test
    fun `test that monitorNodeUpdates does not trigger loadNodes for Remove`() = runTest {
        setupTestData(emptyList())
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf(NodeChanges.Remove))

        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            awaitItem() // State after initial loadNodes
            val updatedState = awaitItem() // State after monitorNodeUpdates processes Remove
            assertThat(updatedState.navigateBack).isEqualTo(triggered)
            // Should not trigger additional loadNodes calls
        }
    }

    @Test
    fun `test that NavigateBackEventConsumed action consumes the navigate back event`() = runTest {
        setupTestData(emptyList())
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf(NodeChanges.Remove))

        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            awaitItem() // State after initial loadNodes
            val stateAfterRemove = awaitItem() // State after Remove triggers navigateBack
            assertThat(stateAfterRemove.navigateBack).isEqualTo(triggered)

            underTest.processAction(CloudDriveAction.NavigateBackEventConsumed)
            val stateAfterConsume = awaitItem() // State after consuming the event
            assertThat(stateAfterConsume.navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that loadNodes populates items correctly`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            assertThat(loadedState.isLoading).isTrue() // Still true because hidden node settings are still loading
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
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }
            val node2 = mock<TypedNode> {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Test Node 2"
            }

            setupTestData(listOf(node1, node2))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(CloudDriveAction.ItemLongClicked(nodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isTrue()
                assertThat(updatedState.items[0].isSelected).isTrue()
                assertThat(updatedState.items[1].isSelected).isFalse()
            }
        }

    @Test
    fun `test that ItemClicked action in selection mode toggles item selection`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(CloudDriveAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(CloudDriveAction.ItemClicked(nodeUiItem2))
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

        underTest.processAction(CloudDriveAction.ItemClicked(nodeUiItem))

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.navigateToFolderEvent).isEqualTo(triggered(folderNode))
        }
    }


    @Test
    fun `test that DeselectAllItems action deselects all items in state`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify initial state
        val loadedState = underTest.uiState.value
        assertThat(loadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)

        underTest.processAction(CloudDriveAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        underTest.processAction(CloudDriveAction.DeselectAllItems)
        testScheduler.advanceUntilIdle()

        val updatedState = underTest.uiState.value
        assertThat(updatedState.isInSelectionMode).isFalse()
        assertThat(updatedState.isSelecting).isFalse()
        assertThat(updatedState.items[0].isSelected).isFalse()
        assertThat(updatedState.items[1].isSelected).isFalse()
    }

    @Test
    fun `test that DeselectAllItems action does nothing when no items are selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.processAction(CloudDriveAction.DeselectAllItems)

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items[0].isSelected).isFalse()
        }
    }

    @Test
    fun `test that SelectAllItems action does nothing when no items exist`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            underTest.processAction(CloudDriveAction.SelectAllItems)

            assertThat(loadedState.isInSelectionMode).isFalse()
            assertThat(loadedState.items).isEmpty()
        }
    }

    @Test
    fun `test that SelectAllItems selects all items when nodes are fully loaded`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify we're in fully loaded state
        val fullyLoadedState = underTest.uiState.value
        assertThat(fullyLoadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)

        underTest.processAction(CloudDriveAction.SelectAllItems)
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
    fun `test that SelectAllItems sets isSelecting to true when nodes are partially loaded`() =
        runTest {
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
            }
            val node2 = mock<TypedNode> {
                on { id } doReturn NodeId(2L)
            }

            // Setup test data with hasMore = true to simulate partially loaded state
            setupTestDataWithPartialLoad(listOf(node1, node2))
            val underTest = createViewModel()

            // Wait for initial loading to complete
            testScheduler.advanceUntilIdle()

            // Verify we're in partially loaded state
            val partiallyLoadedState = underTest.uiState.value
            assertThat(partiallyLoadedState.nodesLoadingState).isEqualTo(NodesLoadingState.PartiallyLoaded)

            underTest.processAction(CloudDriveAction.SelectAllItems)
            // Advance the coroutine to let it execute
            testScheduler.advanceUntilIdle()

            val stateAfterSelectAll = underTest.uiState.value
            // Verify that isSelecting is true because nodes are not fully loaded and selection is pending
            assertThat(stateAfterSelectAll.isSelecting).isTrue()
            assertThat(stateAfterSelectAll.isInSelectionMode).isTrue()
            assertThat(stateAfterSelectAll.items[0].isSelected).isTrue()
            assertThat(stateAfterSelectAll.items[1].isSelected).isTrue()
        }

    @Test
    fun `test that DeselectAllItems sets isSelecting to false`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify initial state
        val loadedState = underTest.uiState.value
        assertThat(loadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)

        // First select all items
        underTest.processAction(CloudDriveAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        // Then deselect all items
        underTest.processAction(CloudDriveAction.DeselectAllItems)
        testScheduler.advanceUntilIdle()

        val updatedState = underTest.uiState.value
        assertThat(updatedState.isInSelectionMode).isFalse()
        assertThat(updatedState.isSelecting).isFalse()
        assertThat(updatedState.items[0].isSelected).isFalse()
        assertThat(updatedState.items[1].isSelected).isFalse()
    }

    @Test
    fun `test that DeselectAllItems cancels pending selection job`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        // Setup test data with hasMore = true to simulate partially loaded state
        setupTestDataWithPartialLoad(listOf(node1, node2))
        val underTest = createViewModel()

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify we're in partially loaded state
        val partiallyLoadedState = underTest.uiState.value
        assertThat(partiallyLoadedState.nodesLoadingState).isEqualTo(NodesLoadingState.PartiallyLoaded)

        // Trigger select all while partially loaded
        underTest.processAction(CloudDriveAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        val stateAfterSelectAll = underTest.uiState.value
        // Verify isSelecting is true
        assertThat(stateAfterSelectAll.isSelecting).isTrue()

        // Now deselect all items - this should cancel the pending selection job
        underTest.processAction(CloudDriveAction.DeselectAllItems)
        testScheduler.advanceUntilIdle()

        val stateAfterDeselect = underTest.uiState.value
        // Verify isSelecting is false and items are not selected
        assertThat(stateAfterDeselect.isSelecting).isFalse()
        assertThat(stateAfterDeselect.isInSelectionMode).isFalse()
        assertThat(stateAfterDeselect.items[0].isSelected).isFalse()
        assertThat(stateAfterDeselect.items[1].isSelected).isFalse()
    }


    @Test
    fun `test that toggleItemSelection removes item from selection when already selected`() =
        runTest {
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
            }

            setupTestData(listOf(node1))
            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(CloudDriveAction.ItemLongClicked(nodeUiItem1))
                val stateAfterSelection = awaitItem()

                val updatedNodeUiItem1 = stateAfterSelection.items[0]
                underTest.processAction(CloudDriveAction.ItemLongClicked(updatedNodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isFalse()
                assertThat(updatedState.items[0].isSelected).isFalse()
            }
        }

    @Test
    fun `test that isInSelectionMode is true when items are selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(CloudDriveAction.ItemLongClicked(nodeUiItem1))
            val state = awaitItem()

            assertThat(state.isInSelectionMode).isTrue()
        }
    }

    @Test
    fun `test that isInSelectionMode is false when no items are selected`() = runTest {
        val node1 = mock<TypedNode> {
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
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(CloudDriveAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(CloudDriveAction.ItemLongClicked(nodeUiItem2))
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isTrue()
            assertThat(updatedState.items[0].isSelected).isTrue()
            assertThat(updatedState.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that selection state is maintained when items are updated`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(CloudDriveAction.ItemLongClicked(nodeUiItem1))
            val state = awaitItem()

            assertThat(state.isInSelectionMode).isTrue()
            assertThat(state.items[0].isSelected).isTrue()
        }
    }

    @Test
    fun `test that selection state is preserved when loading new items`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(CloudDriveAction.ItemLongClicked(nodeUiItem1))
            val stateAfterSelection = awaitItem()

            assertThat(stateAfterSelection.isInSelectionMode).isTrue()
            assertThat(stateAfterSelection.items[0].isSelected).isTrue()
            assertThat(stateAfterSelection.items[1].isSelected).isFalse()
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

        underTest.processAction(CloudDriveAction.ItemClicked(nodeUiItem))
        underTest.uiState.test {
            val stateAfterClick = awaitItem()
            underTest.processAction(CloudDriveAction.NavigateToFolderEventConsumed)
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

        underTest.processAction(CloudDriveAction.ChangeViewTypeClicked)
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

        underTest.processAction(CloudDriveAction.ChangeViewTypeClicked)
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
    fun `test that multiple view type changes are handled correctly`() = runTest {
        setupTestData(emptyList())
        // Use individual flows since flowOf emits all values synchronously
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val gridState = awaitItem() // State after monitorViewType flow emits GRID
            assertThat(gridState.currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that view type is correctly set in initial state`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            // Initial state should have default value before monitoring starts
            assertThat(initialState.currentViewType).isEqualTo(ViewType.LIST)

            val updatedState = awaitItem()
            // After monitoring starts, it should be updated from the flow
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    // Hidden Nodes Tests

    @Test
    fun `test that monitorHiddenNodes is not called when feature flag is disabled`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
            false
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            awaitItem() // Hidden nodes settings loaded state updated
            val finalState = awaitItem() // State after nodes are loaded

            // When feature flag is disabled, hidden node settings loading should remain true
            // because the hidden node monitoring is never started
            assertThat(finalState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            assertThat(finalState.isHiddenNodeSettingsLoading).isFalse()
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.showHiddenNodes).isFalse()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that monitorShowHiddenNodesSettings updates showHiddenNodes when feature flag is enabled`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

            val underTest = createViewModel()

            underTest.uiState.test {
                awaitItem() // Initial state
                awaitItem() // State after nodes are loaded
                val finalState = awaitItem() // State after hidden node settings are loaded

                assertThat(finalState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
                assertThat(finalState.isHiddenNodeSettingsLoading).isFalse()
                assertThat(finalState.isLoading).isFalse()
                assertThat(finalState.showHiddenNodes).isTrue()
            }
        }

    @Test
    fun `test that monitorShowHiddenNodesSettings updates showHiddenNodes to false`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.showHiddenNodes).isFalse()
        }
    }

    @Test
    fun `test that monitorHiddenNodesEnabledUseCase enables hidden nodes for paid account`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.isHiddenNodesEnabled).isTrue()
            }
        }

    @Test
    fun `test that monitorHiddenNodesEnabledUseCase disables hidden nodes for free account`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.isHiddenNodesEnabled).isFalse()
            }
        }

    @Test
    fun `test that monitorHiddenNodesEnabledUseCase disables hidden nodes for expired business account`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()

                assertThat(finalState.isHiddenNodesEnabled).isFalse()
            }
        }

    @Test
    fun `test that monitorHiddenNodesEnabledUseCase enables hidden nodes for active business account`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.isHiddenNodesEnabled).isTrue()
            }
        }

    @Test
    fun `test that monitorHiddenNodesEnabledUseCase handles disabled state gracefully`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that monitorHiddenNodesEnabledUseCase handles enabled state correctly`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesEnabled).isTrue()
        }
    }

    @Test
    fun `test that all hidden nodes properties are updated correctly when feature flag is enabled`() =
        runTest {
            setupTestData(emptyList())
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.showHiddenNodes).isTrue()
                assertThat(finalState.isHiddenNodesEnabled).isTrue()
            }
        }

    @Test
    fun `test that hidden nodes properties remain false when feature flag is disabled`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
            false
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.showHiddenNodes).isFalse()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that processAction handles OpenedFileNodeHandled action correctly`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()
        advanceUntilIdle()

        // Test OpenedFileNodeHandled action
        underTest.processAction(CloudDriveAction.OpenedFileNodeHandled)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.openedFileNode).isNull()
        }
    }

    // Document Scanner Tests

    @Test
    fun `test that prepareDocumentScanner updates state with gmsDocumentScanner on success`() =
        runTest {
            setupTestData(emptyList())
            val gmsDocumentScanner =
                mock<com.google.mlkit.vision.documentscanner.GmsDocumentScanner>()
            whenever(scannerHandler.prepareDocumentScanner()).thenReturn(gmsDocumentScanner)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.prepareDocumentScanner()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.gmsDocumentScanner).isEqualTo(gmsDocumentScanner)
                assertThat(state.documentScanningError).isNull()
            }
        }

    @Test
    fun `test that prepareDocumentScanner updates state with InsufficientRAM error on failure`() =
        runTest {
            setupTestData(emptyList())
            whenever(scannerHandler.prepareDocumentScanner()).thenAnswer {
                throw InsufficientRAMToLaunchDocumentScanner()
            }

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.prepareDocumentScanner()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.gmsDocumentScanner).isNull()
                assertThat(state.documentScanningError).isEqualTo(DocumentScanningError.InsufficientRAM)
            }
        }

    @Test
    fun `test that prepareDocumentScanner updates state with GenericError on other failure`() =
        runTest {
            setupTestData(emptyList())
            whenever(scannerHandler.prepareDocumentScanner()).thenThrow(RuntimeException("Test exception"))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.prepareDocumentScanner()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.gmsDocumentScanner).isNull()
                assertThat(state.documentScanningError).isEqualTo(DocumentScanningError.GenericError)
            }
        }

    @Test
    fun `test that onDocumentScannerFailedToOpen updates state with GenericError`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.onDocumentScannerFailedToOpen()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.documentScanningError).isEqualTo(DocumentScanningError.GenericError)
        }
    }

    @Test
    fun `test that onGmsDocumentScannerConsumed resets gmsDocumentScanner to null`() = runTest {
        setupTestData(emptyList())
        val gmsDocumentScanner = mock<com.google.mlkit.vision.documentscanner.GmsDocumentScanner>()
        whenever(scannerHandler.prepareDocumentScanner()).thenReturn(gmsDocumentScanner)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.prepareDocumentScanner()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.gmsDocumentScanner).isEqualTo(gmsDocumentScanner)

            underTest.onGmsDocumentScannerConsumed()
            val updatedState = awaitItem()
            assertThat(updatedState.gmsDocumentScanner).isNull()
        }
    }

    @Test
    fun `test that onDocumentScanningErrorConsumed resets documentScanningError to null`() =
        runTest {
            setupTestData(emptyList())
            whenever(scannerHandler.prepareDocumentScanner()).thenAnswer {
                throw InsufficientRAMToLaunchDocumentScanner()
            }

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.prepareDocumentScanner()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.documentScanningError).isEqualTo(DocumentScanningError.InsufficientRAM)

                underTest.onDocumentScanningErrorConsumed()
                val updatedState = awaitItem()
                assertThat(updatedState.documentScanningError).isNull()
            }
        }

    @Test
    fun `test that initial state has null gmsDocumentScanner and documentScanningError`() =
        runTest {
            setupTestData(emptyList())
            val underTest = createViewModel()

            underTest.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.gmsDocumentScanner).isNull()
                assertThat(initialState.documentScanningError).isNull()
            }
        }

    @Test
    fun `test that prepareDocumentScanner handles multiple calls correctly`() = runTest {
        setupTestData(emptyList())
        val gmsDocumentScanner1 = mock<com.google.mlkit.vision.documentscanner.GmsDocumentScanner>()
        val gmsDocumentScanner2 = mock<com.google.mlkit.vision.documentscanner.GmsDocumentScanner>()
        whenever(scannerHandler.prepareDocumentScanner())
            .thenReturn(gmsDocumentScanner1)
            .thenReturn(gmsDocumentScanner2)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.prepareDocumentScanner()
        advanceUntilIdle()

        underTest.uiState.test {
            val state1 = awaitItem()
            assertThat(state1.gmsDocumentScanner).isEqualTo(gmsDocumentScanner1)

            underTest.onGmsDocumentScannerConsumed()
            awaitItem()

            underTest.prepareDocumentScanner()
            advanceUntilIdle()

            val state2 = awaitItem()
            assertThat(state2.gmsDocumentScanner).isEqualTo(gmsDocumentScanner2)
        }
    }

    @Test
    fun `test that prepareDocumentScanner handles error recovery correctly`() = runTest {
        setupTestData(emptyList())
        val gmsDocumentScanner = mock<com.google.mlkit.vision.documentscanner.GmsDocumentScanner>()
        var callCount = 0
        whenever(scannerHandler.prepareDocumentScanner()).thenAnswer {
            callCount++
            if (callCount == 1) {
                throw InsufficientRAMToLaunchDocumentScanner()
            } else {
                gmsDocumentScanner
            }
        }

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.prepareDocumentScanner()
        advanceUntilIdle()

        underTest.uiState.test {
            val errorState = awaitItem()
            assertThat(errorState.documentScanningError).isEqualTo(DocumentScanningError.InsufficientRAM)

            underTest.onDocumentScanningErrorConsumed()
            awaitItem()

            underTest.prepareDocumentScanner()
            advanceUntilIdle()

            val successState = awaitItem()
            assertThat(successState.gmsDocumentScanner).isEqualTo(gmsDocumentScanner)
            assertThat(successState.documentScanningError).isNull()
        }
    }

    // Root Node Fallback Logic Tests
    @Test
    fun `test that loadNodes calls getRootNodeIdUseCase when folderId is -1L`() = runTest {
        val rootNodeId = NodeId(789L)

        setupTestData(emptyList())
        whenever(getRootNodeIdUseCase()).thenReturn(rootNodeId)
        whenever(getFileBrowserNodeChildrenUseCase(rootNodeId.longValue)).thenReturn(emptyList())
        // Setup the new chunked use case for root node
        whenever(getNodesByIdInChunkUseCase.invoke(rootNodeId)).thenReturn(
            flowOf(
                Pair(
                    emptyList<TypedNode>(),
                    false
                )
            )
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any())).thenReturn(flowOf())

        createViewModel(-1L)
        advanceUntilIdle()

        verify(getRootNodeIdUseCase).invoke()
    }

    @Test
    fun `test that loadNodes uses NodeId(-1L) when getRootNodeIdUseCase returns null`() = runTest {
        setupTestData(emptyList())
        whenever(getRootNodeIdUseCase()).thenReturn(null)
        whenever(getNodesByIdInChunkUseCase(NodeId(-1L))).thenReturn(
            flowOf(
                Pair(
                    emptyList<TypedNode>(),
                    false
                )
            )
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any())).thenReturn(flowOf())

        createViewModel(-1L)
        advanceUntilIdle()

        verify(getRootNodeIdUseCase).invoke()
        verify(getNodesByIdInChunkUseCase).invoke(NodeId(-1L))
    }

    @Test
    fun `test that isCloudDriveRoot is true when nodeHandle is -1L`() = runTest {
        setupTestData(emptyList())
        whenever(getRootNodeIdUseCase()).thenReturn(null)
        whenever(getFileBrowserNodeChildrenUseCase(-1L)).thenReturn(emptyList())
        whenever(getNodesByIdInChunkUseCase(NodeId(-1L))).thenReturn(
            flowOf(
                Pair(
                    emptyList<TypedNode>(),
                    false
                )
            )
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any())).thenReturn(flowOf())

        val underTest = createViewModel(-1L)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem() // State after loadNodes
            assertThat(state.isCloudDriveRoot).isTrue()
        }
    }

    @Test
    fun `test that isCloudDriveRoot is false when nodeHandle is not -1L`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel(123L)

        underTest.uiState.test {
            awaitItem() // Initial state
            val state = awaitItem() // State after loadNodes
            assertThat(state.isCloudDriveRoot).isFalse()
        }
    }

    @Test
    fun `test that isCloudDriveRoot is true when nodeHandle is -1L and getRootNodeIdUseCase returns a node id`() =
        runTest {
            val rootNodeId = NodeId(789L)
            val rootNode = mock<TypedFolderNode> {
                on { id } doReturn rootNodeId
                on { name } doReturn "Root"
            }

            setupTestData(emptyList())
            whenever(getRootNodeIdUseCase()).thenReturn(rootNodeId)
            whenever(getFileBrowserNodeChildrenUseCase(rootNodeId.longValue)).thenReturn(emptyList())
            // Setup the new chunked use case for root node
            whenever(getNodesByIdInChunkUseCase.invoke(rootNodeId)).thenReturn(
                flowOf(
                    Pair(
                        emptyList<TypedNode>(),
                        false
                    )
                )
            )
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorNodeUpdatesByIdUseCase(any())).thenReturn(flowOf())

            val underTest = createViewModel(-1L)

            underTest.uiState.test {
                awaitItem() // Initial state
                val state = awaitItem() // State after loadNodes
                assertThat(state.isCloudDriveRoot).isTrue()
                assertThat(state.currentFolderId).isEqualTo(rootNodeId)
            }

            // Verify that the new chunked use case was called with the root node ID
            verify(getNodesByIdInChunkUseCase).invoke(rootNodeId)
        }

    @Test
    fun `test that new chunked use case is properly called for regular folder`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()

        underTest.uiState.test {
            awaitItem() // Initial state
            val loadedState = awaitItem() // State after nodes are loaded
            assertThat(loadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            assertThat(loadedState.items).hasSize(1)
        }

        // Verify that the new chunked use case was called
        verify(getNodesByIdInChunkUseCase).invoke(folderNodeId)
    }

    @Test
    fun `test that setupNodesLoading loads items only once when hidden nodes feature is enabled`() =
        runTest {
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }
            val node2 = mock<TypedNode> {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Test Node 2"
            }

            setupTestData(listOf(node1, node2))
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                true
            )

            // Create flows that emit multiple times to test the conditional logic
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false, true, false))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true, false, true))

            val underTest = createViewModel()

            underTest.uiState.test {
                // Initial state
                val initialState = awaitItem()
                assertThat(initialState.isLoading).isTrue()
                assertThat(initialState.nodesLoadingState).isEqualTo(NodesLoadingState.Loading)
                assertThat(initialState.isHiddenNodeSettingsLoading).isTrue()
                assertThat(initialState.items).isEmpty()

                // First emission: nodes loaded, but hidden node settings still loading
                val firstState = awaitItem()

                // Nodes are loaded but hidden node settings are still loading
                assertThat(firstState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
                assertThat(firstState.isHiddenNodeSettingsLoading).isTrue() // Still loading
                assertThat(firstState.isLoading).isTrue() // Still loading because hidden node settings are loading
                assertThat(firstState.items).hasSize(2)
                // Hidden node flags should have default values since they're still loading
                assertThat(firstState.isHiddenNodesEnabled).isFalse()
                assertThat(firstState.showHiddenNodes).isFalse()

                // Second emission: hidden node flags loaded (first values from flows)
                val secondState = awaitItem()
                assertThat(secondState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded) // Should remain fully loaded
                assertThat(secondState.isHiddenNodeSettingsLoading).isFalse() // Now loaded
                assertThat(secondState.isLoading).isFalse() // No longer loading
                assertThat(secondState.items).hasSize(2) // Should remain the same
                assertThat(secondState.isHiddenNodesEnabled).isFalse() // First value from flow
                assertThat(secondState.showHiddenNodes).isTrue() // First value from flow

                // Third emission: hidden node flags updated (second values from flows)
                val thirdState = awaitItem()
                assertThat(thirdState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded) // Should remain fully loaded
                assertThat(thirdState.isHiddenNodeSettingsLoading).isFalse() // Should remain false
                assertThat(thirdState.isLoading).isFalse() // Should remain false
                assertThat(thirdState.items).hasSize(2) // Should remain the same
                assertThat(thirdState.isHiddenNodesEnabled).isTrue() // Second value from flow
                assertThat(thirdState.showHiddenNodes).isFalse() // Second value from flow

                // Fourth emission: hidden node flags updated again (third values from flows)
                val fourthState = awaitItem()
                assertThat(fourthState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded) // Should remain fully loaded
                assertThat(fourthState.isHiddenNodeSettingsLoading).isFalse() // Should remain false
                assertThat(fourthState.isLoading).isFalse() // Should remain false
                assertThat(fourthState.items).hasSize(2) // Should remain the same
                assertThat(fourthState.isHiddenNodesEnabled).isFalse() // Third value from flow
                assertThat(fourthState.showHiddenNodes).isTrue() // Third value from flow
            }

            // Verify that getNodesByIdInChunkUseCase was called (the new use case)
            verify(getNodesByIdInChunkUseCase).invoke(folderNodeId)
            // getFileBrowserNodeChildrenUseCase is only called on node updates, not during initial loading
        }

    @Test
    fun `test that setupNodesLoading handles hidden node flows correctly`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "Test Node 2"
        }

        setupTestData(listOf(node1, node2))
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false, true))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true, false))

        val underTest = createViewModel()

        underTest.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()
            awaitItem()
            // First emission: items loaded with initial hidden node flags
            val firstState = awaitItem()
            assertThat(firstState.isLoading).isFalse()
            assertThat(firstState.items).hasSize(2)
            assertThat(firstState.isHiddenNodesEnabled).isFalse()
            assertThat(firstState.showHiddenNodes).isTrue()

            // Second emission: hidden node flags updated
            val secondState = awaitItem()
            assertThat(secondState.isHiddenNodesEnabled).isTrue()
            assertThat(secondState.showHiddenNodes).isFalse()

            // Items should still be present
            assertThat(secondState.items).hasSize(2)
        }
    }

    @Test
    fun `test that setupNodesLoading handles hidden node flows that emit immediately`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }

        setupTestData(listOf(node1))
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)

        // Flows that emit immediately
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))

        val underTest = createViewModel()

        underTest.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()
            awaitItem()
            // Final state: items loaded + hidden node flags set
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodeSettingsLoading).isFalse()
            assertThat(finalState.isHiddenNodesEnabled).isTrue()
            assertThat(finalState.showHiddenNodes).isFalse()
        }
    }

    @Test
    fun `test that setupNodesLoading works correctly when hidden nodes feature is disabled`() =
        runTest {
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node 1"
            }

            setupTestData(listOf(node1))
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                false
            )

            val underTest = createViewModel()

            underTest.uiState.test {
                // Initial state
                val initialState = awaitItem()
                assertThat(initialState.nodesLoadingState).isEqualTo(NodesLoadingState.Loading)
                assertThat(initialState.isHiddenNodeSettingsLoading).isTrue()
                assertThat(initialState.isLoading).isTrue()
                assertThat(initialState.items).isEmpty()

                awaitItem() // Hidden nodes updated

                // Final state: items loaded, hidden node flags remain default
                val finalState = awaitItem()
                assertThat(finalState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
                assertThat(finalState.isHiddenNodeSettingsLoading).isFalse()
                assertThat(finalState.isLoading).isFalse() // Still true because hidden node settings are still loading
                assertThat(finalState.items).hasSize(1)
                assertThat(finalState.isHiddenNodesEnabled).isFalse() // Default value
                assertThat(finalState.showHiddenNodes).isFalse() // Default value
            }
        }

    @Test
    fun `test that setupNodesLoading handles feature flag check failure gracefully`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }

        setupTestData(listOf(node1))
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease))
            .thenThrow(RuntimeException("Feature flag check failed"))

        val underTest = createViewModel()

        underTest.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertThat(initialState.nodesLoadingState).isEqualTo(NodesLoadingState.Loading)
            assertThat(initialState.isHiddenNodeSettingsLoading).isTrue()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()

            awaitItem() // hidden nodes loading state updated

            // Final state: items loaded, hidden node flags remain default (feature disabled path)
            val finalState = awaitItem()
            assertThat(finalState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            assertThat(finalState.isHiddenNodeSettingsLoading).isFalse()
            assertThat(finalState.isLoading).isFalse() // Still true because hidden node settings are still loading
            assertThat(finalState.items).hasSize(1)
            assertThat(finalState.isHiddenNodesEnabled).isFalse() // Default value
            assertThat(finalState.showHiddenNodes).isFalse() // Default value
        }
    }

    @Test
    fun `test that getCloudSortOrder updates selectedSort in UI state on success`() = runTest {
        setupTestData(emptyList())
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC
        val expectedSortConfiguration = NodeSortConfiguration.default

        whenever(getCloudSortOrderUseCase()).thenReturn(expectedSortOrder)
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
    fun `test that setCloudSortOrder calls use case and refetches sort order`() = runTest {
        setupTestData(emptyList())
        val sortConfiguration =
            NodeSortConfiguration(NodeSortOption.Size, SortDirection.Ascending)
        val expectedSortOrder = SortOrder.ORDER_SIZE_ASC

        whenever(nodeSortConfigurationUiMapper(sortConfiguration)).thenReturn(expectedSortOrder)
        whenever(getCloudSortOrderUseCase()).thenReturn(expectedSortOrder)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.setCloudSortOrder(sortConfiguration)
        advanceUntilIdle()

        // Verify that getCloudSortOrderUseCase was called at least twice:
        // 1. During initialization
        // 2. After setting the sort order (refetch)
        verify(getCloudSortOrderUseCase, times(2)).invoke()
        verify(setCloudSortOrderUseCase).invoke(expectedSortOrder)
    }

    // Storage Over Quota Tests

    @Test
    fun `test that initial state has default storage capacity`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.storageCapacity).isEqualTo(StorageOverQuotaCapacity.DEFAULT)
        }
    }

    @Test
    fun `test that monitorStorageOverQuotaCapacity updates storage capacity with FULL state`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Red))
            whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(true))
            whenever(storageCapacityMapper(StorageState.Red, true)).thenReturn(
                StorageOverQuotaCapacity.FULL
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.storageCapacity).isEqualTo(StorageOverQuotaCapacity.FULL)
            }
        }

    @Test
    fun `test that monitorStorageOverQuotaCapacity updates storage capacity with ALMOST_FULL state`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Orange))
            whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(true))
            whenever(storageCapacityMapper(StorageState.Orange, true)).thenReturn(
                StorageOverQuotaCapacity.ALMOST_FULL
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.storageCapacity).isEqualTo(StorageOverQuotaCapacity.ALMOST_FULL)
            }
        }

    @Test
    fun `test that monitorStorageOverQuotaCapacity updates storage capacity with DEFAULT when banner should not show`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Orange))
            whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(false))
            whenever(storageCapacityMapper(StorageState.Orange, false)).thenReturn(
                StorageOverQuotaCapacity.DEFAULT
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.storageCapacity).isEqualTo(StorageOverQuotaCapacity.DEFAULT)
            }
        }

    @Test
    fun `test that monitorStorageOverQuotaCapacity handles Green storage state correctly`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Green))
            whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(true))
            whenever(storageCapacityMapper(StorageState.Green, true)).thenReturn(
                StorageOverQuotaCapacity.DEFAULT
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.storageCapacity).isEqualTo(StorageOverQuotaCapacity.DEFAULT)
            }
        }

    @Test
    fun `test that StorageAlmostFullWarningDismiss action calls setStorageCapacityAsDefault`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Orange))
            whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(true))
            whenever(storageCapacityMapper(StorageState.Orange, true)).thenReturn(
                StorageOverQuotaCapacity.ALMOST_FULL
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.processAction(CloudDriveAction.StorageAlmostFullWarningDismiss)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.storageCapacity).isEqualTo(StorageOverQuotaCapacity.DEFAULT)
            }

            verify(setAlmostFullStorageBannerClosingTimestampUseCase).invoke()
        }

    @Test
    fun `test that setStorageCapacityAsDefault updates state and calls use case`() = runTest {
        setupTestData(emptyList())
        whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Red))
        whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(true))
        whenever(
            storageCapacityMapper(
                StorageState.Red,
                true
            )
        ).thenReturn(StorageOverQuotaCapacity.FULL)

        val underTest = createViewModel()
        advanceUntilIdle()

        // Verify initial state has FULL capacity
        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.storageCapacity).isEqualTo(StorageOverQuotaCapacity.FULL)

            underTest.setStorageCapacityAsDefault()
            val updatedState = awaitItem()
            assertThat(updatedState.storageCapacity).isEqualTo(StorageOverQuotaCapacity.DEFAULT)
        }

        advanceUntilIdle()
        verify(setAlmostFullStorageBannerClosingTimestampUseCase).invoke()
    }

    @Test
    fun `test that setAlmostFullStorageBannerClosingTimestampUseCase handles errors gracefully`() =
        runTest {
            setupTestData(emptyList())
            whenever(setAlmostFullStorageBannerClosingTimestampUseCase.invoke()).thenThrow(
                RuntimeException("Failed to set timestamp")
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            // Should not crash when setting storage capacity as default
            underTest.processAction(CloudDriveAction.StorageAlmostFullWarningDismiss)
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.storageCapacity).isEqualTo(StorageOverQuotaCapacity.DEFAULT)
            }
        }

    @Test
    fun `test that storage monitoring starts immediately on ViewModel creation`() = runTest {
        setupTestData(emptyList())
        whenever(monitorStorageStateUseCase()).thenReturn(flowOf(StorageState.Red))
        whenever(monitorAlmostFullStorageBannerVisibilityUseCase()).thenReturn(flowOf(true))
        whenever(
            storageCapacityMapper(
                StorageState.Red,
                true
            )
        ).thenReturn(StorageOverQuotaCapacity.FULL)

        createViewModel()
        advanceUntilIdle()

        verify(monitorStorageStateUseCase).invoke()
        verify(monitorAlmostFullStorageBannerVisibilityUseCase).invoke()
    }
} 