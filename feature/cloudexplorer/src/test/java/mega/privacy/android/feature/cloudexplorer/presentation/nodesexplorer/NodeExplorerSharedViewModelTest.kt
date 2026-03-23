package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
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
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeUiItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeExplorerSharedViewModelTest {

    private lateinit var viewModel: NodeExplorerSharedViewModel

    private val monitorNodeUpdatesByIdUseCase = mock<MonitorNodeUpdatesByIdUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val nodeUiItemMapper = mock<NodeUiItemMapper>()

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
            nodeUiItemMapper,
        )
        whenever(monitorStorageStateUseCase()) doReturn flowOf()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf()
        whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf()
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn emptyFlow()

        initViewModel()
    }

    private fun initViewModel(
        loadNodesImpl: () -> Unit = {},
        refreshNodesImpl: () -> Unit = {},
    ) {
        viewModel = object : NodeExplorerSharedViewModel(
            monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeUiItemMapper,
            args = args
        ) {
            override fun loadNodes() = loadNodesImpl()
            override fun refreshNodes() = refreshNodesImpl()
        }
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
        val nodeUiItems = listOf<NodeUiItem<TypedNode>>(mock())

        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = nodeSourceType,
            )
        ) doReturn nodeUiItems

        viewModel.setItems(nodes, NodesLoadingState.FullyLoaded)
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

    private fun storageStates() = listOf(
        arrayOf(StorageState.Red, true),
        arrayOf(StorageState.PayWall, true),
        arrayOf(StorageState.Green, false),
        arrayOf(StorageState.Change, false),
        arrayOf(StorageState.Orange, false),
        arrayOf(StorageState.Unknown, false)
    )

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
