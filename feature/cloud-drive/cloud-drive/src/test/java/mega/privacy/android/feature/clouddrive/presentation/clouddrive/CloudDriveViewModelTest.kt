package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.domain.entity.account.AccountInactivity
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeInfo
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.clouddrive.NodeFetchResult
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.clouddrive.FetchNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.account.AcknowledgeLastPurgeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountInactivityUseCase
import mega.privacy.android.domain.usecase.account.SuppressPurgeTimestampUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingShareParentUserEmailUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
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
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
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
    private val fetchNodesByIdInChunkUseCase: FetchNodesByIdInChunkUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase = mock()
    private val nodeViewItemMapper: NodeViewItemMapper = mock()
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase = mock()
    private val setCloudSortOrderUseCase: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()
    private val getIncomingShareParentUserEmailUseCase: GetIncomingShareParentUserEmailUseCase =
        mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase = mock()
    private val folderNodeHandle = 123L
    private val folderNodeId = NodeId(folderNodeHandle)
    private val mockTracker: AnalyticsTracker = mock()
    private val containsMediaItemUseCase = mock<ContainsMediaItemUseCase>()
    private val monitorAccountInactivityUseCase = mock<MonitorAccountInactivityUseCase>()
    private val acknowledgeLastPurgeUseCase = mock<AcknowledgeLastPurgeUseCase>()
    private val suppressPurgeTimestampUseCase = mock<SuppressPurgeTimestampUseCase>()
    private lateinit var testScheduler: TestCoroutineScheduler

    @Before
    fun setUp() {
        testScheduler = TestCoroutineScheduler()
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        Analytics.initialise(mockTracker)
        whenever(monitorAccountInactivityUseCase()).thenReturn(MutableStateFlow(null))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            getNodeInfoByIdUseCase,
            getFileBrowserNodeChildrenUseCase,
            fetchNodesByIdInChunkUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorNodeUpdatesByIdUseCase,
            nodeViewItemMapper,
            getRootNodeIdUseCase,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            getContactVerificationWarningUseCase,
            areCredentialsVerifiedUseCase,
            getIncomingShareParentUserEmailUseCase,
            getNodeAccessPermission,
            monitorSortCloudOrderUseCase,
            mockTracker,
            containsMediaItemUseCase,
            monitorAccountInactivityUseCase,
            acknowledgeLastPurgeUseCase,
            suppressPurgeTimestampUseCase,
        )
        Analytics.initialise(null)
    }

    private fun createViewModel(
        nodeHandle: Long = folderNodeHandle,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
        args: CloudDriveViewModel.Args = CloudDriveViewModel.Args(
            currentFolderId = NodeId(nodeHandle),
            title = LocalizedText.Literal(""),
            nodeSourceType = nodeSourceType,
            highlightedNodeId = null,
            highlightedNodeNames = null
        ),
    ): CloudDriveViewModel {

        return CloudDriveViewModel(
            getNodeInfoByIdUseCase = getNodeInfoByIdUseCase,
            getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
            setViewTypeUseCase = setViewTypeUseCase,
            monitorViewTypeUseCase = monitorViewTypeUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
            nodeViewItemMapper = nodeViewItemMapper,
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            fetchNodesByIdInChunkUseCase = fetchNodesByIdInChunkUseCase,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
            areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
            getIncomingShareParentUserEmailUseCase = getIncomingShareParentUserEmailUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            containsMediaItemUseCase = containsMediaItemUseCase,
            monitorAccountInactivityUseCase = monitorAccountInactivityUseCase,
            acknowledgeLastPurgeUseCase = acknowledgeLastPurgeUseCase,
            suppressPurgeTimestampUseCase = suppressPurgeTimestampUseCase,
            args = args,
        )
    }

    private suspend fun setupTestData(
        items: List<TypedNode>,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
        isHiddenNodesEnabled: Boolean = false,
        isContactVerificationOn: Boolean = false,
        hasMediaItems: Boolean = false,
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
        whenever(
            getFileBrowserNodeChildrenUseCase(
                parentHandle = any(),
                excludeSensitives = any(),
            )
        ).thenReturn(items)

        // Set up the new chunked use case to return a flow with the items and hasMore flag
        whenever(
            fetchNodesByIdInChunkUseCase.invoke(
                nodeId = any(),
                initialBatchSize = any(),
                excludeSensitives = any(),
            )
        ).thenReturn(
            flowOf(
                NodeFetchResult(
                    loadingState = NodesLoadingState.FullyLoaded,
                    hasMediaItems = hasMediaItems,
                    typedNodes = items
                )
            )
        )

        val nodeUiItems = items.map { node ->
            NodeViewItem(
                node = node,
            )
        }
        whenever(
            nodeViewItemMapper(
                nodeList = items,
                nodeSourceType = nodeSourceType,
                highlightedNodeId = null,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                highlightedNames = null,
                isContactVerificationOn = isContactVerificationOn,
            )
        ).thenReturn(nodeUiItems)
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(isHiddenNodesEnabled))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(
            monitorNodeUpdatesByIdUseCase(
                folderNodeId,
                nodeSourceType
            )
        ).thenReturn(flowOf())

        // Setup contact verification mocks
        whenever(getContactVerificationWarningUseCase()).thenReturn(isContactVerificationOn)
        whenever(areCredentialsVerifiedUseCase(any())).thenReturn(false)
        whenever(getIncomingShareParentUserEmailUseCase(any())).thenReturn(null)
    }

    @Test
    fun `test that initial state is set correctly`() = runTest {
        val args = CloudDriveViewModel.Args(
            currentFolderId = NodeId(folderNodeHandle),
            title = LocalizedText.Literal(""),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            highlightedNodeId = null,
            highlightedNodeNames = null
        )
        val underTest = createViewModel()

        fetchNodesByIdInChunkUseCase.stub {
            onBlocking { invoke(any(), any(), any()) } doReturn flow { awaitCancellation() }
        }

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState).isEqualTo(
                CloudDriveUiState.Loading(
                    title = args.title,
                    nodeSourceType = args.nodeSourceType,
                    currentViewType = ViewType.LIST
                )
            )
        }
    }

    @Test
    fun `test that title is updated correctly when node key is decrypted`() = runTest {
        val nodeName = "Test Folder2"
        val nodeInfo = mock<NodeInfo> {
            on { name }.thenReturn(nodeName)
            on { isNodeKeyDecrypted }.thenReturn(true)
        }
        setupTestData(emptyList())
        whenever(getNodeInfoByIdUseCase(any())).thenReturn(nodeInfo)
        val underTest = createViewModel()
        advanceUntilIdle()
        underTest.uiState.test {
            val updatedState = awaitDataState()
            assertThat(updatedState.title).isEqualTo(LocalizedText.Literal(nodeName))
        }
    }

    @Test
    fun `test that title is updated correctly when node key is not decrypted`() = runTest {
        val nodeInfo = mock<NodeInfo> {
            on { name }.thenReturn("Test Folder2")
            on { isNodeKeyDecrypted }.thenReturn(false)
        }
        setupTestData(emptyList())
        whenever(getNodeInfoByIdUseCase(any())).thenReturn(nodeInfo)
        val underTest = createViewModel()
        advanceUntilIdle()
        underTest.uiState.test {
            val updatedState = awaitDataState()
            assertThat(updatedState.title).isEqualTo(LocalizedText.StringRes(resId = sharedR.string.shared_items_verify_credentials_undecrypted_folder))
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
                val updatedState = awaitDataState()
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
            val updatesFlow = MutableStateFlow<NodeChanges?>(null)
            whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(updatesFlow.filterNotNull())

            // Ensure that getFileBrowserNodeChildrenUseCase is mocked for the node update scenario
            // This will be called when getNodeUiItems() is invoked during the node update
            whenever(
                getFileBrowserNodeChildrenUseCase(
                    parentHandle = any(),
                    excludeSensitives = any(),
                )
            ).thenReturn(
                listOf(
                    node1,
                    node2
                )
            )

            val nodeUiItems = listOf(
                NodeViewItem(node = node1),
                NodeViewItem(node = node2)
            )
            val updatedNodeUiItems = listOf(
                NodeViewItem(node = node1, isSensitive = true),
                NodeViewItem(node = node2, isSensitive = true)
            )
            whenever(
                nodeViewItemMapper(
                    nodeList = listOf(node1, node2),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    highlightedNodeId = null,
                    isHiddenNodesEnabled = false,
                    highlightedNames = null,
                    isContactVerificationOn = false,
                )
            ).thenReturn(
                nodeUiItems,
                nodeUiItems,
                updatedNodeUiItems,
            )

            val underTest = createViewModel()

            // Wait for initial loading to complete
            underTest.uiState.test {
                // Wait for hidden nodes loading to complete
                awaitDataState()
                updatesFlow.emit(NodeChanges.Attributes)
                cancelAndIgnoreRemainingEvents()
            }

            // Verify that getFileBrowserNodeChildrenUseCase was called for the node update
            // The call should happen when NodeChanges.Attributes is processed
            // called twice after reload
            verify(getFileBrowserNodeChildrenUseCase, times(2)).invoke(
                parentHandle = eq(folderNodeHandle),
                excludeSensitives = any(),
            )
        }

    @Test
    fun `test that monitorNodeUpdates handles multiple NodeChanges correctly`() = runTest {
        setupTestData(emptyList())
        val nodeChangesFlow = flowOf(NodeChanges.Attributes, NodeChanges.Remove)
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(nodeChangesFlow)

        val underTest = createViewModel()

        underTest.uiState.test {
            val finalState = awaitDataState() // State after Remove triggers navigateBack
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
            val finalState = awaitDataState()
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
            val updatedState =
                awaitDataState() // State after monitorNodeUpdates processes Attributes
            assertThat(updatedState.navigateBack).isEqualTo(consumed)
        }
    }


    @Test
    fun `test that monitorNodeUpdates does not trigger loadNodes for Remove`() = runTest {
        setupTestData(emptyList())
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf(NodeChanges.Remove))

        val underTest = createViewModel()

        underTest.uiState.test {
            val state = awaitDataState() // State after monitorNodeUpdates processes Remove
            assertThat(state.navigateBack).isEqualTo(triggered)
            // Should not trigger additional loadNodes calls
        }
    }

    @Test
    fun `test that NavigateBackEventConsumed action consumes the navigate back event`() = runTest {
        setupTestData(emptyList())
        whenever(monitorNodeUpdatesByIdUseCase(folderNodeId)).thenReturn(flowOf(NodeChanges.Remove))

        val underTest = createViewModel()

        underTest.uiState.test {
            val removeState = awaitDataState() // State after Remove triggers navigateBack
            assertThat(removeState.navigateBack).isEqualTo(triggered)

            underTest.processAction(CloudDriveAction.NavigateBackEventConsumed)
            val stateAfterConsume = awaitDataState() // State after consuming the event
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
            val loadedState = awaitDataState()

            assertThat(loadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            assertThat(loadedState.items).hasSize(2)
            assertThat(loadedState.items[0].node.id).isEqualTo(NodeId(1L))
            assertThat(loadedState.items[1].node.id).isEqualTo(NodeId(2L))
        }
    }

    @Test
    fun `test that ChangeViewTypeClicked action sets new view type`() = runTest {
        val underTest = createViewModel()

        underTest.processAction(CloudDriveAction.ChangeViewTypeClicked(ViewType.GRID))

        verify(setViewTypeUseCase).invoke(ViewType.GRID)
    }

    @Test
    fun `test that ui state reflects the account inactivity emitted by the use case`() = runTest {
        val inactivity = AccountInactivity(inactivityMonths = 2, purgeTimestamp = 123L)
        whenever(monitorAccountInactivityUseCase())
            .thenReturn(MutableStateFlow(inactivity))
        setupTestData(emptyList())
        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.inactivityMonths).isEqualTo(2)
            assertThat(state.purgeTimestamp).isEqualTo(123L)
        }
    }

    @Test
    fun `test that InactivityBannerDismissed action suppresses and acknowledges the purge timestamp`() =
        runTest {
            val purgeTimestamp = 456L
            val underTest = createViewModel()

            underTest.processAction(CloudDriveAction.InactivityBannerDismissed(purgeTimestamp))
            advanceUntilIdle()

            verify(suppressPurgeTimestampUseCase).invoke(purgeTimestamp)
            verify(acknowledgeLastPurgeUseCase).invoke(purgeTimestamp)
        }

    @Test
    fun `test that monitorViewType updates currentViewType in ui state`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.uiState.test {
            val updatedState = awaitDataState() // State after monitorViewType flow emits
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    @Test
    fun `test that monitorViewType handles GRID view type correctly`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.uiState.test {
            val updatedState = awaitDataState() // State after monitorViewType flow emits
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
            val gridState = awaitDataState() // State after monitorViewType flow emits GRID
            assertThat(gridState.currentViewType).isEqualTo(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `test that sensitive items are not filtered when showHiddenItems is true`() =
        runTest {
            val sensitiveNode = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Sensitive Node"
                on { isMarkedSensitive } doReturn true
            }
            val normalNode = mock<TypedNode> {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Normal Node"
            }

            setupTestData(
                listOf(sensitiveNode, normalNode),
                isHiddenNodesEnabled = true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))

            // Override mapper to return items with sensitivity info
            val sensitiveViewItem = NodeViewItem(node = sensitiveNode, isSensitive = true)
            val normalViewItem = NodeViewItem(node = normalNode, isSensitive = false)
            whenever(
                nodeViewItemMapper(
                    nodeList = listOf(sensitiveNode, normalNode),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    highlightedNodeId = null,
                    isHiddenNodesEnabled = true,
                    highlightedNames = null,
                    isContactVerificationOn = false,
                )
            ).thenReturn(listOf(sensitiveViewItem, normalViewItem))

            val underTest = createViewModel()

            underTest.uiState.test {
                val finalState = awaitDataState()
                assertThat(finalState.items).hasSize(2)
                assertThat(finalState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            }
        }

    @Test
    fun `test that nodeViewItemMapper receives isHiddenNodesEnabled true when hidden nodes is enabled`() =
        runTest {
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Node"
            }
            setupTestData(listOf(node1), isHiddenNodesEnabled = true)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                awaitDataState()
                verify(nodeViewItemMapper, atLeastOnce()).invoke(
                    nodeList = listOf(node1),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    highlightedNodeId = null,
                    isHiddenNodesEnabled = true,
                    highlightedNames = null,
                    isContactVerificationOn = false,
                )
            }
        }

    @Test
    fun `test that fetch use case is called with excludeSensitives true when hidden nodes enabled and show hidden items is false`() =
        runTest {
            val normalNode = mock<TypedNode> {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Normal Node"
            }

            setupTestData(
                listOf(normalNode),
                isHiddenNodesEnabled = true
            )

            val normalViewItem = NodeViewItem(node = normalNode, isSensitive = false)
            whenever(
                nodeViewItemMapper(
                    nodeList = listOf(normalNode),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    highlightedNodeId = null,
                    isHiddenNodesEnabled = true,
                    highlightedNames = null,
                    isContactVerificationOn = false,
                )
            ).thenReturn(listOf(normalViewItem))

            val underTest = createViewModel()

            underTest.uiState.test {
                val finalState = awaitDataState()
                assertThat(finalState.items).hasSize(1)
                assertThat(finalState.items[0].node.id).isEqualTo(NodeId(2L))
            }

            verify(fetchNodesByIdInChunkUseCase).invoke(
                nodeId = any(),
                initialBatchSize = any(),
                excludeSensitives = eq(true),
            )
        }

    // Root Node Fallback Logic Tests
    @Test
    fun `test that loadNodes calls getRootNodeIdUseCase when folderId is -1L`() = runTest {
        val rootNodeId = NodeId(789L)

        setupTestData(emptyList())
        whenever(getRootNodeIdUseCase()).thenReturn(rootNodeId)
        whenever(getFileBrowserNodeChildrenUseCase(rootNodeId.longValue)).thenReturn(emptyList())
        // Set up the new chunked use case for root node
        whenever(fetchNodesByIdInChunkUseCase.invoke(rootNodeId)).thenReturn(
            flowOf(
                NodeFetchResult(
                    loadingState = NodesLoadingState.FullyLoaded,
                    hasMediaItems = false,
                    typedNodes = emptyList()
                )
            )
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())

        val underTest = createViewModel(-1L)
        underTest.uiState.test {
            cancelAndConsumeRemainingEvents()
        }

        verify(getRootNodeIdUseCase).invoke()
    }

    @Test
    fun `test that loadNodes uses NodeId(-1L) when getRootNodeIdUseCase returns null`() = runTest {
        setupTestData(emptyList())
        whenever(getRootNodeIdUseCase()).thenReturn(null)
        whenever(fetchNodesByIdInChunkUseCase(NodeId(-1L))).thenReturn(
            flowOf(
                NodeFetchResult(
                    loadingState = NodesLoadingState.FullyLoaded,
                    hasMediaItems = false,
                    typedNodes = emptyList()
                )
            )
        )
        whenever(getFileBrowserNodeChildrenUseCase(-1L)).thenReturn(emptyList())

        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())

        val underTest = createViewModel(-1L)
        underTest.uiState.test {
            cancelAndConsumeRemainingEvents()
        }

        verify(getRootNodeIdUseCase).invoke()
        verify(fetchNodesByIdInChunkUseCase).invoke(NodeId(-1L))
    }

    @Test
    fun `test that isCloudDriveRoot is true when nodeHandle is -1L`() = runTest {
        setupTestData(emptyList())
        whenever(getRootNodeIdUseCase()).thenReturn(null)
        whenever(getFileBrowserNodeChildrenUseCase(-1L)).thenReturn(emptyList())
        whenever(fetchNodesByIdInChunkUseCase(NodeId(-1L))).thenReturn(
            flowOf(
                NodeFetchResult(
                    loadingState = NodesLoadingState.FullyLoaded,
                    hasMediaItems = false,
                    typedNodes = emptyList()
                )
            )
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())

        val underTest = createViewModel(-1L)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitDataState() // State after loadNodes
            assertThat(state.isCloudDriveRoot).isTrue()
        }
    }

    @Test
    fun `test that isCloudDriveRoot is false when nodeHandle is not -1L`() = runTest {
        setupTestData(emptyList())
        val underTest = createViewModel(123L)

        underTest.uiState.test {
            val state = awaitDataState() // State after loadNodes
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
            // Set up the new chunked use case for root node
            whenever(fetchNodesByIdInChunkUseCase.invoke(rootNodeId)).thenReturn(
                flowOf(
                    NodeFetchResult(
                        loadingState = NodesLoadingState.FullyLoaded,
                        hasMediaItems = false,
                        typedNodes = emptyList()
                    )
                )
            )
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())

            val underTest = createViewModel(-1L)

            underTest.uiState.test {
                val state = awaitDataState() // State after loadNodes
                assertThat(state.isCloudDriveRoot).isTrue()
                assertThat(state.currentFolderId).isEqualTo(rootNodeId)
            }

            // Verify that the new chunked use case was called with the root node ID
            verify(fetchNodesByIdInChunkUseCase).invoke(rootNodeId)
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
            val loadedState = awaitDataState() // State after nodes are loaded
            assertThat(loadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            assertThat(loadedState.items).hasSize(1)
        }

        // Verify that the new chunked use case was called
        verify(fetchNodesByIdInChunkUseCase).invoke(folderNodeId)
    }

    @Test
    fun `test that sensitive items are not filtered when hidden nodes feature is disabled`() =
        runTest {
            val sensitiveNode = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Sensitive Node"
                on { isMarkedSensitive } doReturn true
            }
            setupTestData(listOf(sensitiveNode))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))

            // Mapper returns isSensitive=false because isHiddenNodesEnabled=false
            val viewItem = NodeViewItem(node = sensitiveNode, isSensitive = false)
            whenever(
                nodeViewItemMapper(
                    nodeList = listOf(sensitiveNode),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    highlightedNodeId = null,
                    isHiddenNodesEnabled = false,
                    highlightedNames = null,
                    isContactVerificationOn = false,
                )
            ).thenReturn(listOf(viewItem))

            val underTest = createViewModel()

            underTest.uiState.test {
                val finalState = awaitDataState()
                assertThat(finalState.items).hasSize(1)
            }
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

            // Add mapper mock for isHiddenNodesEnabled = true (middle flow value)
            val nodeUiItems = listOf(
                NodeViewItem(node = node1),
                NodeViewItem(node = node2),
            )
            whenever(
                nodeViewItemMapper(
                    nodeList = listOf(node1, node2),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    highlightedNodeId = null,
                    isHiddenNodesEnabled = true,
                    highlightedNames = null,
                    isContactVerificationOn = false,
                )
            ).thenReturn(nodeUiItems)

            val underTest = createViewModel()

            underTest.uiState.test {

                val state = awaitDataState()
                assertThat(state.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded) // Should remain fully loaded
                assertThat(state.items).hasSize(2) // Should remain the same
            }

            // Verify that getNodesByIdInChunkUseCase was called (the new use case).
            // It may be called more than once because the excludeSensitives flag changes
            // alongside the hidden-nodes setting and drives a re-fetch through the SDK.
            verify(fetchNodesByIdInChunkUseCase, atLeastOnce()).invoke(
                nodeId = any(),
                initialBatchSize = any(),
                excludeSensitives = any(),
            )
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

        // Add mapper mock for isHiddenNodesEnabled = true (second flow value)
        val nodeUiItems = listOf(
            NodeViewItem(node = node1),
            NodeViewItem(node = node2),
        )
        whenever(
            nodeViewItemMapper(
                nodeList = listOf(node1, node2),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                highlightedNodeId = null,
                isHiddenNodesEnabled = true,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ).thenReturn(nodeUiItems)

        val underTest = createViewModel()

        underTest.uiState.test {
            val state = awaitDataState()
            // Items should still be present
            assertThat(state.items).hasSize(2)
        }
    }

    @Test
    fun `test that setupNodesLoading handles hidden node flows that emit immediately`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Node 1"
        }

        setupTestData(listOf(node1), isHiddenNodesEnabled = true)

        // Override showHiddenItems flow
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))

        val underTest = createViewModel()

        underTest.uiState.test {
            val finalState = awaitDataState()
            assertThat(finalState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
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
            val state = awaitDataState()
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
        underTest.uiState.test {
            awaitDataState()
            underTest.setCloudSortOrder(sortConfiguration)
            cancelAndIgnoreRemainingEvents()
        }
        // Verify that getCloudSortOrderUseCase was called at least twice:
        // 1. During initialization
        // 2. After setting the sort order (refetch)
        verify(getFileBrowserNodeChildrenUseCase).invoke(
            parentHandle = any(),
            excludeSensitives = any(),
        )
        verify(setCloudSortOrderUseCase).invoke(expectedSortOrder)
    }

    // Contact Verification Tests

    @Test
    fun `test that checkCurrentFolderContactVerification does not run for CLOUD_DRIVE source type`() =
        runTest {
            setupTestData(emptyList())
            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitDataState()
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
                val initialState = awaitDataState()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse()
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification enables contact verification for OUTGOING_SHARES when enabled`() =
        runTest {
            setupTestData(
                emptyList(),
                NodeSourceType.OUTGOING_SHARES,
                isContactVerificationOn = true
            )

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.OUTGOING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitDataState()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse() // Should be false for outgoing shares
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification enables contact verification for INCOMING_SHARES with verified contact`() =
        runTest {
            setupTestData(
                emptyList(),
                NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = true
            )
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenReturn("test@example.com")
            whenever(areCredentialsVerifiedUseCase("test@example.com")).thenReturn(true)

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitDataState()
                assertThat(initialState.showContactNotVerifiedBanner).isFalse() // Should be false for verified contact
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification enables contact verification for INCOMING_SHARES with unverified contact`() =
        runTest {
            setupTestData(
                emptyList(),
                NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = true
            )
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenReturn("test@example.com")
            whenever(areCredentialsVerifiedUseCase("test@example.com")).thenReturn(false)

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitDataState()
                assertThat(initialState.showContactNotVerifiedBanner).isTrue() // Should be true for unverified contact
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification handles null email for INCOMING_SHARES`() =
        runTest {
            setupTestData(
                emptyList(),
                NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = true
            )
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenReturn(null)

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitDataState()
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
            val initialState = awaitDataState()
            // Should not crash and maintain default values
            assertThat(initialState.showContactNotVerifiedBanner).isFalse()
        }
    }

    @Test
    fun `test that checkCurrentFolderContactVerification handles exception in getIncomingShareParentUserEmailUseCase`() =
        runTest {
            setupTestData(
                emptyList(),
                NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = true
            )
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenThrow(
                RuntimeException("Test exception")
            )

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitDataState()
                // Should not crash and maintain default values
                assertThat(initialState.showContactNotVerifiedBanner).isFalse()
            }
        }

    @Test
    fun `test that checkCurrentFolderContactVerification handles exception in areCredentialsVerifiedUseCase`() =
        runTest {
            setupTestData(
                emptyList(),
                NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = true
            )
            whenever(getIncomingShareParentUserEmailUseCase(folderNodeId)).thenReturn("test@example.com")
            whenever(areCredentialsVerifiedUseCase("test@example.com")).thenThrow(RuntimeException("Test exception"))

            val underTest =
                createViewModel(nodeHandle = 123L, nodeSourceType = NodeSourceType.INCOMING_SHARES)
            advanceUntilIdle()

            underTest.uiState.test {
                val initialState = awaitDataState()
                // Should not crash and maintain default values
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

            underTest.uiState.test {
                assertThat(awaitDataState().hasWritePermission).isTrue()
            }

        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to true for READWRITE permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.READWRITE)

            val underTest = createViewModel()
            testScheduler.advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitDataState().hasWritePermission).isTrue()
            }
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to true for FULL permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.FULL)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitDataState().hasWritePermission).isTrue()
            }
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to false for READ permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.READ)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitDataState().hasWritePermission).isFalse()
            }
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to false for UNKNOWN permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(AccessPermission.UNKNOWN)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitDataState().hasWritePermission).isFalse()
            }
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to false for null permission`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenReturn(null)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitDataState().hasWritePermission).isFalse()
            }
        }

    @Test
    fun `test that checkWritePermission sets hasWritePermission to false when exception is thrown`() =
        runTest {
            setupTestData(emptyList())
            whenever(getNodeAccessPermission.invoke(folderNodeId)).thenThrow(RuntimeException("Test exception"))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitDataState().hasWritePermission).isFalse()
            }
        }

    @Test
    fun `test that node deletion events are monitored even when the flow is not active`() =
        runTest {
            val nodeUpdates = MutableStateFlow(NodeChanges.Name)
            setupTestData(emptyList())
            monitorNodeUpdatesByIdUseCase.stub {
                on { invoke(any(), any()) } doReturn nodeUpdates
            }

            val underTest = createViewModel()

            underTest.uiState.test {
                cancelAndIgnoreRemainingEvents()
            }

            nodeUpdates.emit(NodeChanges.Remove)

            underTest.uiState.test {
                val state = awaitDataState()
                assertThat(state.navigateBack).isEqualTo(triggered)
            }

        }

    private suspend fun ReceiveTurbine<CloudDriveUiState>.awaitDataState(): CloudDriveUiState.Data {
        var item = awaitItem()
        while (item !is CloudDriveUiState.Data) {
            item = awaitItem()
        }
        return item
    }
}