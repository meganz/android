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
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeExplorerSharedViewModelTest {

    private lateinit var viewModel: NodeExplorerSharedViewModel

    private val monitorNodeUpdatesByIdUseCase = mock<MonitorNodeUpdatesByIdUseCase>()
    private val monitorViewTypeUseCase = mock<MonitorViewType>()
    private val setViewTypeUseCase = mock<SetViewType>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val nodeSortConfigurationUiMapper = mock<NodeSortConfigurationUiMapper>()
    private val nodeUiItemMapper = mock<NodeUiItemMapper>()

    private val nodeId = NodeId(1L)
    private val nodeSourceType = NodeSourceType.CLOUD_DRIVE
    private val args = NodeExplorerSharedViewModel.Args(nodeId, nodeSourceType)

    @BeforeEach
    fun setUp() {
        reset(
            monitorNodeUpdatesByIdUseCase,
            monitorViewTypeUseCase,
            setViewTypeUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            monitorSortCloudOrderUseCase,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            nodeUiItemMapper,
        )
        whenever(monitorViewTypeUseCase()) doReturn flowOf()
        whenever(monitorStorageStateUseCase()) doReturn flowOf()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn flowOf()
        whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf()
        whenever(monitorSortCloudOrderUseCase()) doReturn emptyFlow()
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn emptyFlow()

        initViewModel()
    }

    private fun initViewModel(
        loadNodesImpl: () -> Unit = {},
        refreshNodesImpl: () -> Unit = {},
    ) {
        viewModel = object : NodeExplorerSharedViewModel(
            monitorNodeUpdatesByIdUseCase,
            monitorViewTypeUseCase,
            setViewTypeUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            monitorSortCloudOrderUseCase,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
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
            assertThat(actual.viewType).isEqualTo(ViewType.LIST)
            assertThat(actual.isStorageOverQuota).isFalse()
            assertThat(actual.isHiddenNodesEnabled).isFalse()
            assertThat(actual.showHiddenNodes).isFalse()
            assertThat(actual.items).isEmpty()
            assertThat(actual.navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that view type is updated when use case emits`() = runTest {
        val viewTypeFlow = MutableStateFlow(ViewType.LIST)

        whenever(monitorViewTypeUseCase()) doReturn viewTypeFlow

        initViewModel()

        viewModel.nodeExplorerSharedUiState.test {
            assertThat(awaitItem().viewType).isEqualTo(ViewType.LIST)
            viewTypeFlow.value = ViewType.GRID
            assertThat(awaitItem().viewType).isEqualTo(ViewType.GRID)
        }
    }

    @Test
    fun `test that updateViewType should toggle view type and track event`() = runTest {
        viewModel.updateViewType()

        verify(setViewTypeUseCase).invoke(ViewType.GRID)
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
    fun `test that monitorSortOrder should update state and refresh nodes`() = runTest {
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC
        val nodeSortConfiguration =
            NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        val sortOrderFlow = MutableStateFlow<SortOrder?>(null)

        whenever(nodeSortConfigurationUiMapper(sortOrder)) doReturn nodeSortConfiguration
        whenever(monitorSortCloudOrderUseCase()) doReturn sortOrderFlow

        val refreshNodesMock = mock<() -> Unit>()
        initViewModel(refreshNodesImpl = refreshNodesMock)

        sortOrderFlow.value = sortOrder

        viewModel.nodeExplorerSharedUiState.test {
            val actual = awaitItem()
            assertThat(actual.sortOrder).isEqualTo(sortOrder)
            assertThat(actual.nodeSortConfiguration).isEqualTo(nodeSortConfiguration)
        }
        verify(refreshNodesMock).invoke()
    }

    @Test
    fun `test that updateNodeSortConfiguration should call use case`() = runTest {
        val nodeSortConfiguration =
            NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC

        whenever(nodeSortConfigurationUiMapper(nodeSortConfiguration)) doReturn sortOrder

        viewModel.updateNodeSortConfiguration(nodeSortConfiguration)

        verify(setCloudSortOrderUseCase).invoke(sortOrder)
    }

    @Test
    fun `test that setItems should map nodes to UI items and update state`() = runTest {
        val nodes = listOf<TypedNode>(mock())
        val nodeUiItems = listOf<NodeUiItem<TypedNode>>(mock())

        whenever(
            nodeUiItemMapper(
                nodes,
                emptyList(),
                nodeSourceType,
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

            nodeChangesFlow.value = NodeChanges.Remove

            viewModel.nodeExplorerSharedUiState.test {
                assertThat(awaitItem().navigateBack).isEqualTo(triggered)
            }
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
        nodeChangesFlow.value = NodeChanges.Remove


        viewModel.nodeExplorerSharedUiState.test {
            assertThat(awaitItem().navigateBack).isEqualTo(triggered)
        }

        // Now, consume it
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
