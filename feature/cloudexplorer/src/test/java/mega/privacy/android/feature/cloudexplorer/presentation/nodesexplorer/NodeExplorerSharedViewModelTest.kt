package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
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
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel.NodesResult
import mega.privacy.android.shared.nodes.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeViewItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
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
@ExtendWith(CoroutineMainDispatcherExtension::class)
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
        whenever(monitorStorageStateUseCase()) doReturn emptyFlow()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn emptyFlow()
        whenever(monitorShowHiddenItemsUseCase()) doReturn emptyFlow()
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn emptyFlow()
        whenever(monitorConnectivityUseCase()) doReturn emptyFlow()
        whenever {
            nodeViewItemMapper(any(), any(), anyOrNull(), any(), anyOrNull(), any())
        } doReturn emptyList()

        initViewModel()
    }

    private fun initViewModel() {
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
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        assertThat(viewModel.uiState.value).isEqualTo(NodeExplorerUiState.Loading)
    }

    @Test
    fun `test that the loaded state reflects args and defaults`() = runTest {
        viewModel.uiState.test {
            val actual = awaitData()
            assertThat(actual.currentFolderId).isEqualTo(nodeId)
            assertThat(actual.nodeSourceType).isEqualTo(nodeSourceType)
            assertThat(actual.isStorageOverQuota).isFalse()
            assertThat(actual.isHiddenNodesEnabled).isFalse()
            assertThat(actual.showHiddenNodes).isFalse()
            assertThat(actual.items).isEmpty()
            assertThat(actual.navigateBack).isEqualTo(consumed)
            assertThat(actual.isConnected).isTrue()
            cancelAndIgnoreRemainingEvents()
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

        viewModel.uiState.test {
            awaitData()
            viewModel.onSearchQuery("doc")
            val actual = awaitDataUntil { it.searchItems.isNotEmpty() }
            assertThat(actual.searchItems).isEqualTo(items)
            assertThat(actual.searchedQuery).isEqualTo("doc")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that searchLoadingState stays Loading while the search runs and settles afterwards`() =
        runTest {
            val gate = CompletableDeferred<List<TypedNode>>()
            whenever(nodeSourceTypeToSearchTargetMapper(any())) doReturn SearchTarget.INCOMING_SHARE
            whenever { searchUseCase(any(), any(), any()) } doSuspendableAnswer { gate.await() }
            whenever {
                nodeViewItemMapper(any(), any(), anyOrNull(), any(), anyOrNull(), any())
            } doReturn emptyList()

            viewModel.uiState.test {
                awaitData()
                viewModel.onSearchQuery("doc")
                val loading = awaitDataUntil { it.searchedQuery == "doc" }
                assertThat(loading.searchLoadingState).isEqualTo(NodesLoadingState.Loading)

                gate.complete(emptyList())
                val settled =
                    awaitDataUntil { it.searchLoadingState == NodesLoadingState.FullyLoaded }
                assertThat(settled.searchedQuery).isEqualTo("doc")
                cancelAndIgnoreRemainingEvents()
            }
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

        viewModel.uiState.test {
            val actual = awaitDataUntil { it.isStorageOverQuota == expectedOverQuota }
            assertThat(actual.isStorageOverQuota).isEqualTo(expectedOverQuota)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that hidden node settings are updated when use cases emit`() = runTest {
        val hiddenEnabledFlow = MutableStateFlow(true)
        val showHiddenFlow = MutableStateFlow(true)

        whenever(monitorHiddenNodesEnabledUseCase()) doReturn hiddenEnabledFlow
        whenever(monitorShowHiddenItemsUseCase()) doReturn showHiddenFlow

        initViewModel()

        viewModel.uiState.test {
            val initial = awaitDataUntil { it.isHiddenNodesEnabled && it.showHiddenNodes }
            assertThat(initial.isHiddenNodesEnabled).isTrue()
            assertThat(initial.showHiddenNodes).isTrue()

            hiddenEnabledFlow.value = false
            val afterHidden = awaitDataUntil { !it.isHiddenNodesEnabled }
            assertThat(afterHidden.showHiddenNodes).isTrue()

            showHiddenFlow.value = false
            val afterShow = awaitDataUntil { !it.showHiddenNodes }
            assertThat(afterShow.isHiddenNodesEnabled).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that nodes are mapped to UI items`() = runTest {
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

        viewModel.nodesResult = NodesResult(nodes, NodesLoadingState.FullyLoaded)

        viewModel.uiState.test {
            val actual = awaitDataUntil { it.items.isNotEmpty() }
            assertThat(actual.items).isEqualTo(nodeUiItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that navigateBack is triggered when the node is removed`() = runTest {
        val nodeChangesFlow = MutableStateFlow<NodeChanges?>(null)
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn
                nodeChangesFlow.filterNotNull()

        initViewModel()

        viewModel.uiState.test {
            awaitData()
            nodeChangesFlow.value = NodeChanges.Remove
            val actual = awaitDataUntil { it.navigateBack == triggered }
            assertThat(actual.navigateBack).isEqualTo(triggered)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that nodes are refetched when the change is not Remove`() = runTest {
        val nodeChangesFlow = MutableStateFlow<NodeChanges?>(null)
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn
                nodeChangesFlow.filterNotNull()

        initViewModel()

        viewModel.uiState.test {
            awaitDataUntil { it.nodesLoadingState == NodesLoadingState.FullyLoaded }
            viewModel.nodesResult = NodesResult(emptyList(), NodesLoadingState.PartiallyLoaded)
            nodeChangesFlow.value = NodeChanges.Attributes
            val actual =
                awaitDataUntil { it.nodesLoadingState == NodesLoadingState.PartiallyLoaded }
            assertThat(actual.nodesLoadingState).isEqualTo(NodesLoadingState.PartiallyLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that refreshNodes refetches the nodes`() = runTest {
        viewModel.uiState.test {
            awaitDataUntil { it.nodesLoadingState == NodesLoadingState.FullyLoaded }
            viewModel.nodesResult = NodesResult(emptyList(), NodesLoadingState.PartiallyLoaded)
            viewModel.refreshNodes()
            val actual =
                awaitDataUntil { it.nodesLoadingState == NodesLoadingState.PartiallyLoaded }
            assertThat(actual.nodesLoadingState).isEqualTo(NodesLoadingState.PartiallyLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onNavigateBackEventConsumed consumes the event`() = runTest {
        val nodeChangesFlow = MutableStateFlow<NodeChanges?>(null)
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn
                nodeChangesFlow.filterNotNull()

        initViewModel()

        viewModel.uiState.test {
            awaitData()
            nodeChangesFlow.value = NodeChanges.Remove
            awaitDataUntil { it.navigateBack == triggered }

            viewModel.onNavigateBackEventConsumed()
            val actual = awaitDataUntil { it.navigateBack == consumed }
            assertThat(actual.navigateBack).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that isConnected mirrors connectivity changes`() = runTest {
        val connectivityFlow = MutableStateFlow(true)
        whenever(monitorConnectivityUseCase()) doReturn connectivityFlow

        initViewModel()

        viewModel.uiState.test {
            assertThat(awaitDataUntil { it.isConnected }.isConnected).isTrue()
            connectivityFlow.value = false
            assertThat(awaitDataUntil { !it.isConnected }.isConnected).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that noConnectionEvent is triggered when the screen opens offline`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(false)

        initViewModel()

        viewModel.uiState.test {
            val actual = awaitDataUntil { it.noConnectionEvent == triggered }
            assertThat(actual.noConnectionEvent).isEqualTo(triggered)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that noConnectionEvent stays consumed when the screen opens online`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(true)

        initViewModel()

        viewModel.uiState.test {
            val actual = awaitDataUntil { it.isConnected }
            assertThat(actual.noConnectionEvent).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onNoConnectionEventConsumed consumes the event`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(false)

        initViewModel()

        viewModel.uiState.test {
            awaitDataUntil { it.noConnectionEvent == triggered }
            viewModel.onNoConnectionEventConsumed()
            val actual = awaitDataUntil { it.noConnectionEvent == consumed }
            assertThat(actual.noConnectionEvent).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<NodeExplorerUiState>.awaitData(): NodeExplorerUiState.Data {
        var item = awaitItem()
        while (item !is NodeExplorerUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private suspend fun ReceiveTurbine<NodeExplorerUiState>.awaitDataUntil(
        predicate: (NodeExplorerUiState.Data) -> Boolean,
    ): NodeExplorerUiState.Data {
        while (true) {
            val item = awaitItem()
            if (item is NodeExplorerUiState.Data && predicate(item)) return item
        }
    }

    private fun storageStates() = listOf(
        arrayOf<Any>(StorageState.Red, true),
        arrayOf<Any>(StorageState.PayWall, true),
        arrayOf<Any>(StorageState.Green, false),
        arrayOf<Any>(StorageState.Change, false),
        arrayOf<Any>(StorageState.Orange, false),
        arrayOf<Any>(StorageState.Unknown, false),
    )
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
    var nodesResult = NodesResult(emptyList(), NodesLoadingState.FullyLoaded)

    override val folderNameFlow: Flow<LocalizedText> = flowOf(LocalizedText.Literal(""))

    override val isRootNodeFlow: Flow<Boolean> = flowOf(true)

    override val nodesFlow: Flow<NodesResult> = refreshSignal
        .onStart { emit(Unit) }
        .map { nodesResult }
}
