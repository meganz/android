package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNavigationStack
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeViewItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeExplorerSharedViewModelTest {

    private lateinit var viewModel: TestNodeExplorerSharedViewModel

    private val monitorNodeUpdatesByIdUseCase = mock<MonitorNodeUpdatesByIdUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val nodeViewItemMapper = mock<NodeViewItemMapper>()
    private val searchUseCase = mock<SearchUseCase>()
    private val nodeSourceTypeToSearchTargetMapper = mock<NodeSourceTypeToSearchTargetMapper>()
    private val getNodeNavigationStackUseCase = mock<GetNodeNavigationStackUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    private val nodeId = NodeId(1234L)
    private val nodeSourceType = NodeSourceType.INCOMING_SHARES
    private val args = NodeExplorerSharedViewModel.Args(nodeId, nodeSourceType)

    @BeforeEach
    fun setUp() {
        reset(
            monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeViewItemMapper,
            searchUseCase,
            nodeSourceTypeToSearchTargetMapper,
            getNodeNavigationStackUseCase,
            monitorConnectivityUseCase,
        )
        whenever(monitorStorageStateUseCase()) doReturn flowOf()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf()
        whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf()
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn emptyFlow()
        whenever(monitorConnectivityUseCase()) doReturn emptyFlow()

        initViewModel()
    }

    private fun initViewModel(
        loadNodesImpl: () -> Unit = {},
        refreshNodesImpl: () -> Unit = {},
    ) {
        viewModel = TestNodeExplorerSharedViewModel(
            monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            nodeViewItemMapper = nodeViewItemMapper,
            getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>(),
            searchUseCase = searchUseCase,
            nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
            getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            args = args,
            loadNodesImpl = loadNodesImpl,
            refreshNodesImpl = refreshNodesImpl
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        viewModel.nodeExplorerSharedUiState.test {
            val actual = awaitItem()
            assertThat(actual.currentFolderId).isEqualTo(nodeId)
            assertThat(actual.nodeSourceType).isEqualTo(nodeSourceType)
            assertThat(actual.isStorageOverQuota).isFalse()
            assertThat(actual.isHiddenNodesEnabled).isFalse()
            assertThat(actual.showHiddenNodes).isFalse()
            assertThat(actual.items).isEmpty()
            assertThat(actual.navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that onSearchQuery folds the mapped search results into searchItems`() = runTest {
        val nodes = listOf<TypedNode>(mock())
        val items = listOf<NodeViewItem<TypedNode>>(mock())
        whenever(nodeSourceTypeToSearchTargetMapper(any())) doReturn SearchTarget.INCOMING_SHARE
        whenever { searchUseCase(any(), any(), any()) } doReturn nodes
        whenever {
            nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = nodeSourceType,
                highlightedNodeId = null,
                isHiddenNodesEnabled = false,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        } doReturn items

        initViewModel()
        viewModel.onSearchQuery("doc")
        advanceUntilIdle()

        assertThat(viewModel.nodeExplorerSharedUiState.value.searchItems).isEqualTo(items)
    }

    @Test
    fun `test that onSearchQuery sets searchLoadingState to Loading for a new query`() = runTest {
        whenever(nodeSourceTypeToSearchTargetMapper(any())) doReturn SearchTarget.INCOMING_SHARE
        whenever { searchUseCase(any(), any(), any()) } doReturn emptyList()
        whenever {
            nodeViewItemMapper(any(), any(), anyOrNull(), any(), anyOrNull(), any())
        } doReturn emptyList()
        initViewModel()

        // A completed search leaves the state FullyLoaded.
        viewModel.onSearchQuery("first")
        advanceUntilIdle()
        assertThat(viewModel.nodeExplorerSharedUiState.value.searchLoadingState)
            .isEqualTo(NodesLoadingState.FullyLoaded)

        // A new query flips it back to Loading while the (suspended) search runs.
        val gate = CompletableDeferred<List<TypedNode>>()
        whenever { searchUseCase(any(), any(), any()) } doSuspendableAnswer { gate.await() }
        viewModel.onSearchQuery("second")

        assertThat(viewModel.nodeExplorerSharedUiState.value.searchLoadingState)
            .isEqualTo(NodesLoadingState.Loading)

        gate.complete(emptyList())
    }

    @Test
    fun `test that search records searchedQuery once the results settle`() = runTest {
        whenever(nodeSourceTypeToSearchTargetMapper(any())) doReturn SearchTarget.INCOMING_SHARE
        whenever { searchUseCase(any(), any(), any()) } doReturn emptyList()
        whenever {
            nodeViewItemMapper(any(), any(), anyOrNull(), any(), anyOrNull(), any())
        } doReturn emptyList()
        initViewModel()

        viewModel.onSearchQuery("doc")
        advanceUntilIdle()

        assertThat(viewModel.nodeExplorerSharedUiState.value.searchedQuery).isEqualTo("doc")
    }

    @Test
    fun `test that searchedQuery still trails the query while the search is running`() = runTest {
        whenever(nodeSourceTypeToSearchTargetMapper(any())) doReturn SearchTarget.INCOMING_SHARE
        whenever { searchUseCase(any(), any(), any()) } doReturn emptyList()
        whenever {
            nodeViewItemMapper(any(), any(), anyOrNull(), any(), anyOrNull(), any())
        } doReturn emptyList()
        initViewModel()

        viewModel.onSearchQuery("first")
        advanceUntilIdle()

        val gate = CompletableDeferred<List<TypedNode>>()
        whenever { searchUseCase(any(), any(), any()) } doSuspendableAnswer { gate.await() }
        viewModel.onSearchQuery("second")

        // The in-flight query has not produced results yet, so searchedQuery still points at "first".
        assertThat(viewModel.nodeExplorerSharedUiState.value.searchedQuery).isEqualTo("first")

        gate.complete(emptyList())
    }

    @Test
    fun `test that onSearchQuery keeps results without Loading when re-issuing the current query`() =
        runTest {
            whenever(nodeSourceTypeToSearchTargetMapper(any())) doReturn SearchTarget.INCOMING_SHARE
            whenever { searchUseCase(any(), any(), any()) } doReturn emptyList()
            whenever {
                nodeViewItemMapper(any(), any(), anyOrNull(), any(), anyOrNull(), any())
            } doReturn emptyList()
            initViewModel()

            viewModel.onSearchQuery("doc")
            advanceUntilIdle()

            // Re-issuing the already-searched query must not blink back to Loading.
            val gate = CompletableDeferred<List<TypedNode>>()
            whenever { searchUseCase(any(), any(), any()) } doSuspendableAnswer { gate.await() }
            viewModel.onSearchQuery("doc")

            assertThat(viewModel.nodeExplorerSharedUiState.value.searchLoadingState)
                .isEqualTo(NodesLoadingState.FullyLoaded)

            gate.complete(emptyList())
        }

    @Test
    fun `test that resolveSearchResultStack returns the path from the use case`() = runTest {
        val target = NodeId(5L)
        val path = listOf(NodeId(1L), NodeId(3L), target)
        whenever { getNodeNavigationStackUseCase(target) } doReturn
                NodeNavigationStack(stack = path, isUnderRootNode = true)

        assertThat(viewModel.resolveSearchResultStack(target)).isEqualTo(path)
    }

    @Test
    fun `test that resolveSearchResultStack falls back to the node id when the path is empty`() =
        runTest {
            val target = NodeId(5L)
            whenever { getNodeNavigationStackUseCase(target) } doReturn NodeNavigationStack()

            assertThat(viewModel.resolveSearchResultStack(target)).containsExactly(target)
        }

    @Test
    fun `test that resolveSearchResultStack falls back to the node id when the use case throws`() =
        runTest {
            val target = NodeId(5L)
            whenever { getNodeNavigationStackUseCase(target) } doAnswer {
                throw RuntimeException("boom")
            }

            assertThat(viewModel.resolveSearchResultStack(target)).containsExactly(target)
        }

    @ParameterizedTest
    @MethodSource("storageStates")
    fun `test that isStorageOverQuota is updated correctly based on storage state`(
        storageState: StorageState,
        expectedOverQuota: Boolean,
    ) = runTest {
        whenever(monitorStorageStateUseCase()) doReturn flowOf(storageState)

        initViewModel()

        viewModel.nodeExplorerSharedUiState.test {
            assertThat(awaitItem().isStorageOverQuota).isEqualTo(expectedOverQuota)
        }
    }

    @Test
    fun `test that hidden node settings are updated when use cases emit`() = runTest {
        val hiddenEnabledFlow = MutableStateFlow(true)
        val showHiddenFlow = MutableStateFlow(true)

        whenever(monitorHiddenNodesEnabledUseCase()) doReturn hiddenEnabledFlow
        whenever(monitorShowHiddenItemsUseCase()) doReturn showHiddenFlow

        initViewModel()

        viewModel.nodeExplorerSharedUiState.test {
            val initialActual = awaitItem()
            assertThat(initialActual.isHiddenNodesEnabled).isTrue()
            assertThat(initialActual.showHiddenNodes).isTrue()

            hiddenEnabledFlow.value = false
            val updatedActual = awaitItem()
            assertThat(updatedActual.isHiddenNodesEnabled).isFalse()
            assertThat(updatedActual.showHiddenNodes).isTrue()

            showHiddenFlow.value = false
            val finalActual = awaitItem()
            assertThat(finalActual.isHiddenNodesEnabled).isFalse()
            assertThat(finalActual.showHiddenNodes).isFalse()
        }
    }

    @Test
    fun `test that setItems should map nodes to UI items and update state`() = runTest {
        val nodes = listOf<TypedNode>(mock())
        val nodeUiItems = listOf<NodeViewItem<TypedNode>>(mock())

        whenever(
            nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = nodeSourceType,
                highlightedNodeId = null,
                isHiddenNodesEnabled = false,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ) doReturn nodeUiItems

        viewModel.setTestItems(nodes, NodesLoadingState.FullyLoaded)
        advanceUntilIdle()

        viewModel.nodeExplorerSharedUiState.test {
            assertThat(awaitItem().items).isEqualTo(nodeUiItems)
        }
    }

    @Test
    fun `test that monitorNodeUpdates should trigger navigateBack when node is removed`() =
        runTest {
            val nodeChangesFlow = MutableStateFlow<NodeChanges?>(null)

            whenever(
                monitorNodeUpdatesByIdUseCase(
                    nodeId,
                    nodeSourceType
                )
            ) doReturn nodeChangesFlow.filterNotNull()

            initViewModel()
            viewModel.monitorNodeUpdates()
            advanceUntilIdle()

            nodeChangesFlow.value = NodeChanges.Remove
            advanceUntilIdle()

            viewModel.nodeExplorerSharedUiState.test {
                assertThat(awaitItem().navigateBack).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that monitorNodeUpdates should invoke refreshNodes when change is not Remove`() =
        runTest {
            val nodeChangesFlow = MutableStateFlow<NodeChanges?>(null)

            whenever(
                monitorNodeUpdatesByIdUseCase(
                    nodeId,
                    nodeSourceType
                )
            ) doReturn nodeChangesFlow.filterNotNull()

            var refreshCalled = false
            initViewModel(refreshNodesImpl = { refreshCalled = true })

            viewModel.monitorNodeUpdates()
            advanceUntilIdle()

            nodeChangesFlow.value = NodeChanges.Attributes
            advanceUntilIdle()

            assertThat(refreshCalled).isTrue()
        }

    @Test
    fun `test that onNavigateBackEventConsumed should consume the event`() = runTest {
        val nodeChangesFlow = MutableStateFlow<NodeChanges?>(null)

        whenever(
            monitorNodeUpdatesByIdUseCase(
                nodeId,
                nodeSourceType
            )
        ) doReturn nodeChangesFlow.filterNotNull()

        initViewModel()
        viewModel.monitorNodeUpdates()
        advanceUntilIdle()

        nodeChangesFlow.value = NodeChanges.Remove
        advanceUntilIdle()

        viewModel.nodeExplorerSharedUiState.test {
            assertThat(awaitItem().navigateBack).isEqualTo(triggered)
        }

        viewModel.onNavigateBackEventConsumed()

        viewModel.nodeExplorerSharedUiState.test {
            assertThat(awaitItem().navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that isConnected mirrors connectivity changes`() = runTest {
        val connectivityFlow = MutableStateFlow(true)
        whenever(monitorConnectivityUseCase()) doReturn connectivityFlow

        initViewModel()

        assertThat(viewModel.nodeExplorerSharedUiState.value.isConnected).isTrue()

        connectivityFlow.value = false
        advanceUntilIdle()

        assertThat(viewModel.nodeExplorerSharedUiState.value.isConnected).isFalse()
    }

    @Test
    fun `test that noConnectionEvent is triggered when the screen opens offline`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(false)

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.nodeExplorerSharedUiState.value.noConnectionEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that noConnectionEvent stays consumed when the screen opens online`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(true)

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.nodeExplorerSharedUiState.value.noConnectionEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that noConnectionEvent stays consumed when connection is lost after opening online`() =
        runTest {
            val connectivityFlow = MutableStateFlow(true)
            whenever(monitorConnectivityUseCase()) doReturn connectivityFlow

            initViewModel()
            advanceUntilIdle()

            connectivityFlow.value = false
            advanceUntilIdle()

            assertThat(viewModel.nodeExplorerSharedUiState.value.noConnectionEvent)
                .isEqualTo(consumed)
        }

    @Test
    fun `test that onNoConnectionEventConsumed consumes the event`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(false)

        initViewModel()
        advanceUntilIdle()
        assertThat(viewModel.nodeExplorerSharedUiState.value.noConnectionEvent).isEqualTo(triggered)

        viewModel.onNoConnectionEventConsumed()

        assertThat(viewModel.nodeExplorerSharedUiState.value.noConnectionEvent).isEqualTo(consumed)
    }

    private fun storageStates() = listOf(
        arrayOf<Any>(StorageState.Red, true),
        arrayOf<Any>(StorageState.PayWall, true),
        arrayOf<Any>(StorageState.Green, false),
        arrayOf<Any>(StorageState.Change, false),
        arrayOf<Any>(StorageState.Orange, false),
        arrayOf<Any>(StorageState.Unknown, false)
    )

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}

private class TestNodeExplorerSharedViewModel(
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    nodeViewItemMapper: NodeViewItemMapper,
    getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    searchUseCase: SearchUseCase,
    nodeSourceTypeToSearchTargetMapper: NodeSourceTypeToSearchTargetMapper,
    getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    args: Args,
    private val loadNodesImpl: () -> Unit = {},
    private val refreshNodesImpl: () -> Unit = {},
) : NodeExplorerSharedViewModel(
    monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase = monitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
    nodeViewItemMapper = nodeViewItemMapper,
    getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
    searchUseCase = searchUseCase,
    nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
    getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
    monitorConnectivityUseCase = monitorConnectivityUseCase,
    args = args,
) {
    override fun loadNodes() = loadNodesImpl()
    override fun refreshNodes() = refreshNodesImpl()

    fun setTestItems(nodes: List<TypedNode>, nodesLoadingState: NodesLoadingState) =
        setItems(nodes, nodesLoadingState)
}
