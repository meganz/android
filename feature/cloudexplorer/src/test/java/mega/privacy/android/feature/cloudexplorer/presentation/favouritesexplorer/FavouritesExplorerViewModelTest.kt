package mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
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
    private val nodeUiItemMapper = mock<NodeUiItemMapper>()
    private val getAllFavoritesUseCase = mock<GetAllFavoritesUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeUiItemMapper,
            getAllFavoritesUseCase
        )
        whenever(monitorStorageStateUseCase()) doReturn emptyFlow()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn emptyFlow()
        whenever(monitorShowHiddenItemsUseCase()) doReturn emptyFlow()
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())) doReturn emptyFlow()
        whenever(getAllFavoritesUseCase()) doReturn emptyFlow()
        wheneverBlocking {
            nodeUiItemMapper(
                nodeList = emptyList(),
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
            )
        } doReturn emptyList()
    }

    private fun initViewModel(showFiles: Boolean = false) {
        viewModel = FavouritesExplorerViewModel(
            monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeUiItemMapper,
            getAllFavoritesUseCase,
            args = FavouritesExplorerViewModel.Args(showFiles),
        )
    }

    private fun nodeUiItem(node: TypedNode): NodeUiItem<TypedNode> =
        NodeUiItem(node = node, isSelected = false)

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
                nodeUiItemMapper(
                    nodeList = foldersOnly,
                    existingItems = emptyList(),
                    nodeSourceType = NodeSourceType.FAVOURITES,
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
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
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
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
            )
        ) doReturn nodeUiItems
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = nodeUiItems,
                nodeSourceType = NodeSourceType.FAVOURITES,
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
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
            )
        ) doReturn nodeUiItems
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = nodeUiItems,
                nodeSourceType = NodeSourceType.FAVOURITES,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.monitorNodeUpdates()
        advanceUntilIdle()

        verify(getAllFavoritesUseCase, times(2)).invoke()
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
