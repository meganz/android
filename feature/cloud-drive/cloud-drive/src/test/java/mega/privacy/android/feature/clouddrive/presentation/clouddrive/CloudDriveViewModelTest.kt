package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeInfo
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingShareParentUserEmailUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.destination.CloudDriveNavKey
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
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase = mock()
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase = mock()
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase = mock()
    private val setCloudSortOrderUseCase: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase = mock()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()
    private val getIncomingShareParentUserEmailUseCase: GetIncomingShareParentUserEmailUseCase =
        mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
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
            getNodeInfoByIdUseCase,
            getFileBrowserNodeChildrenUseCase,
            getNodesByIdInChunkUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorNodeUpdatesByIdUseCase,
            nodeUiItemMapper,
            getRootNodeIdUseCase,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            monitorStorageStateEventUseCase,
            monitorTransferOverQuotaUseCase,
            getContactVerificationWarningUseCase,
            areCredentialsVerifiedUseCase,
            getIncomingShareParentUserEmailUseCase,
            getNodeAccessPermission,
            monitorSortCloudOrderUseCase,
            getFeatureFlagValueUseCase,
        )
    }

    private fun createViewModel(
        nodeHandle: Long = folderNodeHandle,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    ) = CloudDriveViewModel(
        getNodeInfoByIdUseCase = getNodeInfoByIdUseCase,
        getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
        monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
        monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        getRootNodeIdUseCase = getRootNodeIdUseCase,
        getNodesByIdInChunkUseCase = getNodesByIdInChunkUseCase,
        setCloudSortOrderUseCase = setCloudSortOrderUseCase,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
        monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
        monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
        getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
        areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
        getIncomingShareParentUserEmailUseCase = getIncomingShareParentUserEmailUseCase,
        getNodeAccessPermission = getNodeAccessPermission,
        monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        navKey = CloudDriveNavKey(nodeHandle, nodeSourceType = nodeSourceType),
    )

    private suspend fun setupTestData(
        items: List<TypedNode>,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    ) {
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(
            NodeSortConfiguration.default
        )
        val nodeInfo = mock<NodeInfo> {
            on { name }.thenReturn("Test folder")
            on { isNodeKeyDecrypted }.thenReturn(true)
        }
        whenever(getNodeInfoByIdUseCase(eq(folderNodeId))).thenReturn(nodeInfo)
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
                nodeSourceType = nodeSourceType,
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
        whenever(
            monitorNodeUpdatesByIdUseCase(
                folderNodeId,
                nodeSourceType
            )
        ).thenReturn(flowOf())

        // Setup quota monitoring mocks
        whenever(monitorStorageStateEventUseCase()).thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    1L,
                    StorageState.Green
                )
            )
        )
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf(false))

        // Setup contact verification mocks
        whenever(getContactVerificationWarningUseCase()).thenReturn(false)
        whenever(areCredentialsVerifiedUseCase(any())).thenReturn(false)
        whenever(getIncomingShareParentUserEmailUseCase(any())).thenReturn(null)
    }

    private suspend fun setupTestDataWithPartialLoad(items: List<TypedNode>) {
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(
            NodeSortConfiguration.default
        )
        val nodeInfo = mock<NodeInfo> {
            on { name }.thenReturn("Test folder")
            on { isNodeKeyDecrypted }.thenReturn(true)
        }
        whenever(getNodeInfoByIdUseCase(eq(folderNodeId))).thenReturn(nodeInfo)
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

        // Setup quota monitoring mocks
        whenever(monitorStorageStateEventUseCase()).thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    1L,
                    StorageState.Green
                )
            )
        )
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf(false))

        // Setup contact verification mocks
        whenever(getContactVerificationWarningUseCase()).thenReturn(false)
        whenever(areCredentialsVerifiedUseCase(any())).thenReturn(false)
        whenever(getIncomingShareParentUserEmailUseCase(any())).thenReturn(null)

        // Setup permission check mock
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.OWNER)
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
    fun `test that title is updated correctly when node key is decrypted`() = runTest {
        val nodeName = "Test Folder2"
        val nodeInfo = mock<NodeInfo> {
            on { name }.thenReturn(nodeName)
            on { isNodeKeyDecrypted }.thenReturn(true)
        }
        whenever(getNodeInfoByIdUseCase(any())).thenReturn(nodeInfo)
        setupTestData(emptyList())
        val underTest = createViewModel()
        advanceUntilIdle()
        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.title).isEqualTo(LocalizedText.Literal(nodeName))
        }
    }

    @Test
    fun `test that title is updated correctly when node key is not decrypted`() = runTest {
        val nodeInfo = mock<NodeInfo> {
            on { name }.thenReturn("Test Folder2")
            on { isNodeKeyDecrypted }.thenReturn(false)
        }
        whenever(getNodeInfoByIdUseCase(any())).thenReturn(nodeInfo)
        setupTestData(emptyList())
        val underTest = createViewModel()
        advanceUntilIdle()
        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.title).isEqualTo(LocalizedText.StringRes(resId = R.string.shared_items_verify_credentials_undecrypted_folder))
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
            // called twice after reload
            verify(getFileBrowserNodeChildrenUseCase, times(2)).invoke(folderNodeHandle)
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

    @Test
    fun `test that monitorShowHiddenNodesSettings updates showHiddenNodes`() =
        runTest {
            setupTestData(emptyList())
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
    fun `test that monitorHiddenNodesEnabledUseCase enables hidden nodes for paid account`() =
        runTest {
            setupTestData(emptyList())
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
    fun `test that all hidden nodes properties are updated correctly`() =
        runTest {
            setupTestData(emptyList())
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
                    emptyList(),
                    false
                )
            )
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())

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
                    emptyList(),
                    false
                )
            )
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())

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
                    emptyList(),
                    false
                )
            )
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())

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

            setupTestData(emptyList())
            whenever(getRootNodeIdUseCase()).thenReturn(rootNodeId)
            whenever(getFileBrowserNodeChildrenUseCase(rootNodeId.longValue)).thenReturn(emptyList())
            // Setup the new chunked use case for root node
            whenever(getNodesByIdInChunkUseCase.invoke(rootNodeId)).thenReturn(
                flowOf(
                    Pair(
                        emptyList(),
                        false
                    )
                )
            )
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())

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
    fun `test that getCloudSortOrder updates selectedSort in UI state on success`() = runTest {
        setupTestData(emptyList())
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC
        val expectedSortConfiguration = NodeSortConfiguration.default

        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(expectedSortOrder))
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
            NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC

        whenever(nodeSortConfigurationUiMapper(sortConfiguration)).thenReturn(expectedSortOrder)
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(expectedSortOrder))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.setCloudSortOrder(sortConfiguration)
        advanceUntilIdle()

        // Verify that getCloudSortOrderUseCase was called at least twice:
        // 1. During initialization
        // 2. After setting the sort order (refetch)
        verify(getFileBrowserNodeChildrenUseCase).invoke(any())
        verify(setCloudSortOrderUseCase).invoke(expectedSortOrder)
    }

    // Quota Warning Tests

    @Test
    fun `test that initial state has default quota values`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isStorageOverQuota).isFalse()
            assertThat(initialState.isTransferOverQuota).isFalse()
        }
    }

    @Test
    fun `test that monitorStorageOverQuota updates isStorageOverQuota with Red state`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateEventUseCase()).thenReturn(
                MutableStateFlow(StorageStateEvent(1L, StorageState.Red))
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isStorageOverQuota).isTrue()
                assertThat(state.isTransferOverQuota).isFalse()
            }
        }

    @Test
    fun `test that monitorStorageOverQuota updates isStorageOverQuota with PayWall state`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateEventUseCase()).thenReturn(
                MutableStateFlow(StorageStateEvent(1L, StorageState.PayWall))
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isStorageOverQuota).isTrue()
                assertThat(state.isTransferOverQuota).isFalse()
            }
        }

    @Test
    fun `test that monitorStorageOverQuota handles Green storage state correctly`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateEventUseCase()).thenReturn(
                MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
            )

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isStorageOverQuota).isFalse()
                assertThat(state.isTransferOverQuota).isFalse()
            }
        }

    @Test
    fun `test that monitorTransferOverQuota updates isTransferOverQuota correctly`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf(true))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isTransferOverQuota).isTrue()
                assertThat(state.isStorageOverQuota).isFalse()
            }
        }

    @Test
    fun `test that ConsumeOverQuotaWarning action resets both quota flags`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorStorageStateEventUseCase()).thenReturn(
                MutableStateFlow(StorageStateEvent(1L, StorageState.Red))
            )
            whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf(true))

            val underTest = createViewModel()
            advanceUntilIdle()

            // Verify both flags are set
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isStorageOverQuota).isTrue()
                assertThat(state.isTransferOverQuota).isTrue()

                // Consume the warning
                underTest.processAction(CloudDriveAction.OverQuotaConsumptionWarning)
                val updatedState = awaitItem()
                assertThat(updatedState.isStorageOverQuota).isFalse()
                assertThat(updatedState.isTransferOverQuota).isFalse()
            }
        }

    @Test
    fun `test that quota monitoring starts immediately on ViewModel creation`() = runTest {
        setupTestData(emptyList())
        whenever(monitorStorageStateEventUseCase()).thenReturn(
            MutableStateFlow(StorageStateEvent(1L, StorageState.Red))
        )
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf(true))

        createViewModel()
        advanceUntilIdle()

        verify(monitorStorageStateEventUseCase).invoke()
        verify(monitorTransferOverQuotaUseCase).invoke()
    }

    // Contact Verification Tests

    @Test
    fun `test that checkCurrentFolderContactVerification does not run for CLOUD_DRIVE source type`() =
        runTest {
            setupTestData(emptyList())
            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.isContactVerificationOn).isFalse()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse()
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification does not run for OUTGOING_SHARES when contact verification is disabled`() =
        runTest {
            setupTestData(emptyList(), NodeSourceType.OUTGOING_SHARES)
            whenever(getContactVerificationWarningUseCase()).thenReturn(false)

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.OUTGOING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.isContactVerificationOn).isFalse()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse()
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification enables contact verification for OUTGOING_SHARES when enabled`() =
        runTest {
            setupTestData(emptyList(), NodeSourceType.OUTGOING_SHARES)
            whenever(getContactVerificationWarningUseCase()).thenReturn(true)

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.OUTGOING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.isContactVerificationOn).isTrue()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse() // Should be false for outgoing shares
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification enables contact verification for INCOMING_SHARES with verified contact`() =
        runTest {
            setupTestData(emptyList(), NodeSourceType.INCOMING_SHARES)
            whenever(getContactVerificationWarningUseCase()).thenReturn(true)
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenReturn("test@example.com")
            whenever(areCredentialsVerifiedUseCase("test@example.com")).thenReturn(true)

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.isContactVerificationOn).isTrue()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse() // Should be false for verified contact
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification enables contact verification for INCOMING_SHARES with unverified contact`() =
        runTest {
            setupTestData(emptyList(), NodeSourceType.INCOMING_SHARES)
            whenever(getContactVerificationWarningUseCase()).thenReturn(true)
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenReturn("test@example.com")
            whenever(areCredentialsVerifiedUseCase("test@example.com")).thenReturn(false)

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.isContactVerificationOn).isTrue()
                assertThat(initialState.showContactNotVerifiedBanner).isTrue() // Should be true for unverified contact
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification handles null email for INCOMING_SHARES`() =
        runTest {
            setupTestData(emptyList(), NodeSourceType.INCOMING_SHARES)
            whenever(getContactVerificationWarningUseCase()).thenReturn(true)
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenReturn(null)

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.isContactVerificationOn).isTrue()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse() // Should be false when email is null
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification handles exception gracefully`() = runTest {
        setupTestData(emptyList(), NodeSourceType.INCOMING_SHARES)
        whenever(getContactVerificationWarningUseCase()).thenThrow(RuntimeException("Test exception"))

        val underTest =
            createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
        advanceUntilIdle()

        underTest.uiState.test {
            val initialState = awaitItem()
            // Should not crash and maintain default values
            assertThat(initialState.isContactVerificationOn).isFalse()
            assertThat(initialState.showContactNotVerifiedBanner).isFalse()
        }
    }

    @Test
    fun `test that checkCurrentFolderContactVerification handles exception in getIncomingShareParentUserEmailUseCase`() =
        runTest {
            setupTestData(emptyList(), NodeSourceType.INCOMING_SHARES)
            whenever(getContactVerificationWarningUseCase()).thenReturn(true)
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenThrow(
                RuntimeException("Test exception")
            )

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitItem()
                // Should not crash and maintain default values
                assertThat(initialState.isContactVerificationOn).isFalse()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse()
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification handles exception in areCredentialsVerifiedUseCase`() =
        runTest {
            setupTestData(emptyList(), NodeSourceType.INCOMING_SHARES)
            whenever(getContactVerificationWarningUseCase()).thenReturn(true)
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenReturn("test@example.com")
            whenever(areCredentialsVerifiedUseCase("test@example.com")).thenThrow(RuntimeException("Test exception"))

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitItem()
                // Should not crash and maintain default values
                assertThat(initialState.isContactVerificationOn).isFalse()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse()
            }
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to true for OWNER permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.OWNER)

            val underTest = createViewModel()
            testScheduler.advanceUntilIdle()

            assertThat(underTest.uiState.value.hasWritePermission).isTrue()
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to true for READWRITE permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.READWRITE)

            val underTest = createViewModel()
            testScheduler.advanceUntilIdle()

            assertThat(underTest.uiState.value.hasWritePermission).isTrue()
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to true for FULL permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.FULL)

            val underTest = createViewModel()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasWritePermission).isTrue()
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to false for READ permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.READ)

            val underTest = createViewModel()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasWritePermission).isFalse()
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to false for UNKNOWN permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.UNKNOWN)

            val underTest = createViewModel()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasWritePermission).isFalse()
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to false for null permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(null)

            val underTest = createViewModel()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasWritePermission).isFalse()
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to false when exception is thrown`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenThrow(RuntimeException("Test exception"))

            val underTest = createViewModel()
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasWritePermission).isFalse()
        }

    @Test
    fun `test that isSearchRevampEnabled is updated when feature flag is enabled`() = runTest {
        setupTestData(emptyList())
        whenever(getFeatureFlagValueUseCase(AppFeatures.SearchRevamp)).thenReturn(true)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isSearchRevampEnabled).isTrue()
        }
    }
}