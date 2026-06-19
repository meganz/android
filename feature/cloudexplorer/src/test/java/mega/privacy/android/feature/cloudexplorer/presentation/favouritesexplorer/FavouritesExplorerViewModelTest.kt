package mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
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
class FavouritesExplorerViewModelTest {

    private lateinit var viewModel: FavouritesExplorerViewModel

    private val monitorNodeUpdatesByIdUseCase = mock<MonitorNodeUpdatesByIdUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val nodeViewItemMapper = mock<NodeViewItemMapper>()
    private val getAllFavoritesUseCase = mock<GetAllFavoritesUseCase>()
    private val searchUseCase = mock<SearchUseCase>()
    private val nodeSourceTypeToSearchTargetMapper = mock<NodeSourceTypeToSearchTargetMapper>()
    private val getNodeNavigationStackUseCase = mock<GetNodeNavigationStackUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeViewItemMapper,
            getAllFavoritesUseCase,
            searchUseCase,
            nodeSourceTypeToSearchTargetMapper,
        )
        whenever(monitorStorageStateUseCase()) doReturn emptyFlow()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn emptyFlow()
        whenever(monitorShowHiddenItemsUseCase()) doReturn emptyFlow()
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())) doReturn emptyFlow()
        whenever(monitorConnectivityUseCase()) doReturn emptyFlow()
        whenever(getAllFavoritesUseCase()) doReturn emptyFlow()
        wheneverBlocking {
            nodeViewItemMapper(
                nodeList = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                isHiddenNodesEnabled = true,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        } doReturn emptyList()
    }

    private fun initViewModel(showFiles: Boolean = false) {
        viewModel = FavouritesExplorerViewModel(
            monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            nodeViewItemMapper = nodeViewItemMapper,
            getAllFavoritesUseCase = getAllFavoritesUseCase,
            searchUseCase = searchUseCase,
            nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
            getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>(),
            args = FavouritesExplorerViewModel.Args(showFiles),
        )
    }

    private fun nodeUiItem(node: TypedNode): NodeViewItem<TypedNode> =
        NodeViewItem(node = node)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that initial state reflects showFiles from args`(
        showFiles: Boolean,
    ) = runTest {
        initViewModel(showFiles = showFiles)
        advanceUntilIdle()

        assertThat(viewModel.favouritesExplorerUiState.value.showFiles).isEqualTo(showFiles)
    }

    @Test
    fun `test that loadNodes passes only folder favourites as items when showFiles is false`() =
        runTest {
            val folder = mock<TypedFolderNode>()
            val file = mock<TypedFileNode>()
            val nodes = listOf<TypedNode>(folder, file)
            val foldersOnly = listOf<TypedNode>(folder)
            val nodeUiItems = listOf(nodeUiItem(folder))
            whenever(getAllFavoritesUseCase()) doReturn flowOf(nodes)
            whenever(
                nodeViewItemMapper(
                    nodeList = foldersOnly,
                    nodeSourceType = NodeSourceType.FAVOURITES,
                    highlightedNodeId = null,
                    isHiddenNodesEnabled = false,
                    highlightedNames = null,
                    isContactVerificationOn = false,
                )
            ) doReturn nodeUiItems

            initViewModel(showFiles = false)
            advanceUntilIdle()

            assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
        }

    @Test
    fun `test that loadNodes passes all favourites as items when showFiles is true`() = runTest {
        val folder = mock<TypedFolderNode>()
        val file = mock<TypedFileNode>()
        val nodes = listOf<TypedNode>(folder, file)
        val nodeUiItems = listOf(nodeUiItem(folder), nodeUiItem(file))
        whenever(getAllFavoritesUseCase()) doReturn flowOf(nodes)
        whenever(
            nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                isHiddenNodesEnabled = false,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ) doReturn nodeUiItems

        initViewModel(showFiles = true)
        advanceUntilIdle()

        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that nodes are refreshed`() = runTest {
        val folder = mock<TypedFolderNode>()
        val nodes = listOf<TypedNode>(folder)
        val nodeUiItems = listOf(nodeUiItem(folder))
        whenever(getAllFavoritesUseCase()) doReturn flowOf(nodes)
        whenever(
            nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                isHiddenNodesEnabled = false,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ) doReturn nodeUiItems
        whenever(
            nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                isHiddenNodesEnabled = true,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.refreshNodes()
        advanceUntilIdle()

        verify(getAllFavoritesUseCase, times(2)).invoke()
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that node updates are monitored`() = runTest {
        val folder = mock<TypedFolderNode>()
        val nodes = listOf<TypedNode>(folder)
        val nodeUiItems = listOf(nodeUiItem(folder))
        whenever(getAllFavoritesUseCase()) doReturn flowOf(nodes)
        whenever(
            nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                isHiddenNodesEnabled = false,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ) doReturn nodeUiItems
        whenever(
            nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                isHiddenNodesEnabled = false,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.monitorNodeUpdates()
        advanceUntilIdle()

        verify(getAllFavoritesUseCase, times(2)).invoke()
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that searchItems exposes the recursive favourites search results`() = runTest {
        val match = mock<TypedFileNode> { on { id } doReturn NodeId(2L) }
        val results = listOf<TypedNode>(match)
        val searchedItems = listOf(nodeUiItem(match))
        whenever(nodeSourceTypeToSearchTargetMapper(any())) doReturn SearchTarget.ROOT_NODES
        wheneverBlocking { searchUseCase(any(), any(), any()) } doReturn results
        whenever(
            nodeViewItemMapper(
                nodeList = results,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                isHiddenNodesEnabled = false,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ) doReturn searchedItems

        initViewModel(showFiles = true)
        advanceUntilIdle()
        viewModel.onSearchQuery("spec")
        advanceUntilIdle()

        assertThat(viewModel.nodeExplorerSharedUiState.value.searchItems.map { it.node })
            .containsExactly(match)
    }

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
