package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.annotation.Nullable
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeInfo
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
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
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodesExplorerViewModelTest {

    private lateinit var viewModel: NodesExplorerViewModel

    private val monitorNodeUpdatesByIdUseCase = mock<MonitorNodeUpdatesByIdUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val nodeViewItemMapper = mock<NodeViewItemMapper>()
    private val getFileBrowserNodeChildrenUseCase = mock<GetFileBrowserNodeChildrenUseCase>()
    private val getNodesByIdInChunkUseCase = mock<GetNodesByIdInChunkUseCase>()
    private val getNodeInfoByIdUseCase = mock<GetNodeInfoByIdUseCase>()
    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val searchUseCase = mock<SearchUseCase>()
    private val nodeSourceTypeToSearchTargetMapper = mock<NodeSourceTypeToSearchTargetMapper>()
    private val getNodeNavigationStackUseCase = mock<GetNodeNavigationStackUseCase>()

    private val nodeId = NodeId(rootNodeHandle)
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
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeViewItemMapper,
            getFileBrowserNodeChildrenUseCase,
            getNodesByIdInChunkUseCase,
            getNodeInfoByIdUseCase,
            getRootNodeIdUseCase,
        )
        whenever(monitorStorageStateUseCase()) doReturn emptyFlow()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn emptyFlow()
        whenever(monitorShowHiddenItemsUseCase()) doReturn emptyFlow()
        whenever(monitorNodeUpdatesByIdUseCase(nodeId, nodeSourceType)) doReturn emptyFlow()
        wheneverBlocking { getNodesByIdInChunkUseCase(nodeId) } doReturn emptyFlow()
        wheneverBlocking { getNodeInfoByIdUseCase(nodeId) } doReturn defaultNodeInfo
        wheneverBlocking { getRootNodeIdUseCase() } doReturn null
    }

    private fun initViewModel() {
        viewModel = NodesExplorerViewModel(
            monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            nodeViewItemMapper = nodeViewItemMapper,
            getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
            getNodesByIdInChunkUseCase = getNodesByIdInChunkUseCase,
            getNodeInfoByIdUseCase = getNodeInfoByIdUseCase,
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            searchUseCase = searchUseCase,
            nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
            getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
            getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>(),
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
        val nodeUiItems = emptyList<NodeViewItem<TypedNode>>()

        whenever(getNodesByIdInChunkUseCase(nodeId)) doReturn flowOf(nodes to false)
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

        initViewModel()
        advanceUntilIdle()

        verify(getNodesByIdInChunkUseCase, times(1)).invoke(nodeId)
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that nodes are refreshed`() = runTest {
        val nodes = listOf<TypedNode>(mock())
        val nodeUiItems = emptyList<NodeViewItem<TypedNode>>()

        whenever(getFileBrowserNodeChildrenUseCase(nodeId.longValue)) doReturn nodes
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

    @ParameterizedTest
    @Nullable
    @ValueSource(longs = [5432, rootNodeHandle])
    fun `test that isRoot is updated`(
        rootId: Long?,
    ) = runTest {
        val nodeId = rootId?.let { NodeId(rootId) }

        whenever(getRootNodeIdUseCase()) doReturn nodeId

        initViewModel()

        viewModel.nodesExplorerUiState.test {
            assertThat(awaitItem().isRoot).also {
                if (rootId == null || rootId == rootNodeHandle) {
                    it.isTrue()
                } else {
                    it.isFalse()
                }
            }
        }
    }

    @Test
    fun `test that searchItems exposes the mapped search results for a query`() = runTest {
        val nodes = listOf<TypedNode>(mock())
        val items = listOf<NodeViewItem<TypedNode>>(mock())
        whenever(nodeSourceTypeToSearchTargetMapper(any())) doReturn SearchTarget.ROOT_NODES
        wheneverBlocking { searchUseCase(any(), any(), any()) } doReturn nodes
        wheneverBlocking {
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

    companion object {
        private const val rootNodeHandle = 1234L
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
