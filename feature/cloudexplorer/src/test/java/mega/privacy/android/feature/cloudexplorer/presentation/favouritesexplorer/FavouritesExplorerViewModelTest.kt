package mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeUiItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FavouritesExplorerViewModelTest {

    private lateinit var viewModel: FavouritesExplorerViewModel

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
    private val getAllFavoritesUseCase = mock<GetAllFavoritesUseCase>()

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
            getAllFavoritesUseCase
        )
        whenever(monitorViewTypeUseCase()) doReturn emptyFlow()
        whenever(monitorStorageStateUseCase()) doReturn emptyFlow()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn emptyFlow()
        whenever(monitorShowHiddenItemsUseCase()) doReturn emptyFlow()
        whenever(monitorSortCloudOrderUseCase()) doReturn emptyFlow()
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

    private fun initViewModel() {
        viewModel = FavouritesExplorerViewModel(
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
            getAllFavoritesUseCase,
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        initViewModel()
        advanceUntilIdle()

        viewModel.favouritesExplorerUiState.test {
            val actual = awaitItem()
            assertThat(actual).isNotNull()
        }
    }

    @Test
    fun `test that nodes are loaded`() = runTest {
        val nodes = emptyList<TypedNode>()
        val nodeUiItems = emptyList<NodeUiItem<TypedNode>>()
        whenever(getAllFavoritesUseCase()) doReturn flowOf(nodes)
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.loadNodes()
        advanceUntilIdle()

        verify(getAllFavoritesUseCase, atLeast(1)).invoke()
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that nodes are refreshed`() = runTest {
        val nodes = emptyList<TypedNode>()
        val nodeUiItems = emptyList<NodeUiItem<TypedNode>>()
        whenever(getAllFavoritesUseCase()) doReturn flowOf(nodes)
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.refreshNodes()
        advanceUntilIdle()

        verify(getAllFavoritesUseCase, atLeast(1)).invoke()
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that node updates are monitored`() = runTest {
        val nodes = emptyList<TypedNode>()
        val nodeUiItems = emptyList<NodeUiItem<TypedNode>>()
        whenever(getAllFavoritesUseCase()) doReturn flowOf(nodes)
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.FAVOURITES,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.monitorNodeUpdates()
        advanceUntilIdle()

        verify(getAllFavoritesUseCase, atLeast(1)).invoke()
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
