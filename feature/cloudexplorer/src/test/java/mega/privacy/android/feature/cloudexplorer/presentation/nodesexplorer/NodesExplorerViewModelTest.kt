package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeInfo
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodesExplorerViewModelTest {

    private lateinit var viewModel: NodesExplorerViewModel

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
    private val getFileBrowserNodeChildrenUseCase = mock<GetFileBrowserNodeChildrenUseCase>()
    private val getNodesByIdInChunkUseCase = mock<GetNodesByIdInChunkUseCase>()
    private val getNodeInfoByIdUseCase = mock<GetNodeInfoByIdUseCase>()

    private val nodeId = NodeId(1L)
    private val nodeSourceType = NodeSourceType.CLOUD_DRIVE
    private val args = NodeExplorerSharedViewModel.Args(nodeId, nodeSourceType)
    private val defaultNodeInfo = mock<NodeInfo> {
        on { name } doReturn ""
        on { isNodeKeyDecrypted } doReturn true
    }

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
            getFileBrowserNodeChildrenUseCase,
            getNodesByIdInChunkUseCase,
            getNodeInfoByIdUseCase
        )
        whenever(monitorViewTypeUseCase()) doReturn emptyFlow()
        whenever(monitorStorageStateUseCase()) doReturn emptyFlow()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn emptyFlow()
        whenever(monitorShowHiddenItemsUseCase()) doReturn emptyFlow()
        whenever(monitorSortCloudOrderUseCase()) doReturn emptyFlow()
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn emptyFlow()
        wheneverBlocking { getNodesByIdInChunkUseCase(nodeId) } doReturn emptyFlow()
        wheneverBlocking { getNodeInfoByIdUseCase(nodeId) } doReturn defaultNodeInfo
    }

    private fun initViewModel() {
        viewModel = NodesExplorerViewModel(
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
            getFileBrowserNodeChildrenUseCase,
            getNodesByIdInChunkUseCase,
            getNodeInfoByIdUseCase,
            args = args
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        initViewModel()

        viewModel.nodesExplorerUiState.test {
            assertThat(awaitItem().folderName).isEqualTo(LocalizedText.Literal(""))
        }
    }

    @Test
    fun `test that nodes are loaded`() = runTest {
        val nodes = listOf<TypedNode>(mock())
        val nodeUiItems = emptyList<NodeUiItem<TypedNode>>()

        whenever(getNodesByIdInChunkUseCase(nodeId)) doReturn flowOf(nodes to false)
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = nodeSourceType,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.loadNodes()
        advanceUntilIdle()

        verify(getNodesByIdInChunkUseCase).invoke(nodeId)
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that nodes are refreshed`() = runTest {
        val nodes = listOf<TypedNode>(mock())
        val nodeUiItems = emptyList<NodeUiItem<TypedNode>>()

        whenever(getFileBrowserNodeChildrenUseCase(nodeId.longValue)) doReturn nodes
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = nodeSourceType,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.refreshNodes()
        advanceUntilIdle()

        verify(getFileBrowserNodeChildrenUseCase).invoke(nodeId.longValue)
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that folder name is updated`() = runTest {
        val nodeInfo = mock<NodeInfo> {
            on { name } doReturn "folderName"
            on { isNodeKeyDecrypted } doReturn true
        }

        whenever(getNodeInfoByIdUseCase(nodeId)) doReturn nodeInfo

        initViewModel()

        viewModel.nodesExplorerUiState.test {
            assertThat(awaitItem().folderName).isEqualTo(LocalizedText.Literal("folderName"))
        }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
