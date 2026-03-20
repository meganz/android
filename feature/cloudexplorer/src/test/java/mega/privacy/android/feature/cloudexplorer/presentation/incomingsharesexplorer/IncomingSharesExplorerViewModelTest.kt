package mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingSharesChildrenNodeUseCase
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeUiItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IncomingSharesExplorerViewModelTest {

    private lateinit var viewModel: IncomingSharesExplorerViewModel

    private val monitorNodeUpdatesByIdUseCase = mock<MonitorNodeUpdatesByIdUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val nodeUiItemMapper = mock<NodeUiItemMapper>()
    private val getIncomingSharesChildrenNodeUseCase = mock<GetIncomingSharesChildrenNodeUseCase>()


    @BeforeEach
    fun setUp() {
        reset(
            monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeUiItemMapper,
            getIncomingSharesChildrenNodeUseCase,
        )
        whenever(monitorStorageStateUseCase()) doReturn emptyFlow()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn emptyFlow()
        whenever(monitorShowHiddenItemsUseCase()) doReturn emptyFlow()
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())) doReturn emptyFlow()
        wheneverBlocking { getIncomingSharesChildrenNodeUseCase(any()) } doReturn emptyList()
        wheneverBlocking {
            nodeUiItemMapper(
                nodeList = emptyList(),
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )
        } doReturn emptyList()

        initViewModel()
    }

    private fun initViewModel() {
        viewModel = IncomingSharesExplorerViewModel(
            monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeUiItemMapper,
            getIncomingSharesChildrenNodeUseCase,
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        viewModel.incomingSharesExplorerUiState.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `test that nodes are loaded`() = runTest {
        val nodes = emptyList<ShareNode>()
        val nodeUiItems = emptyList<NodeUiItem<TypedNode>>()

        whenever(getIncomingSharesChildrenNodeUseCase(any())) doReturn nodes
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )
        ) doReturn nodeUiItems

        viewModel.loadNodes()
        advanceUntilIdle()

        verify(getIncomingSharesChildrenNodeUseCase).invoke(-1L)
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    @Test
    fun `test that nodes are refreshed`() = runTest {
        val nodes = emptyList<ShareNode>()
        val nodeUiItems = emptyList<NodeUiItem<TypedNode>>()

        whenever(getIncomingSharesChildrenNodeUseCase(any())) doReturn nodes
        whenever(
            nodeUiItemMapper(
                nodeList = nodes,
                existingItems = emptyList(),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )
        ) doReturn nodeUiItems

        viewModel.refreshNodes()
        advanceUntilIdle()

        verify(getIncomingSharesChildrenNodeUseCase).invoke(-1L)
        assertThat(viewModel.nodeExplorerSharedUiState.value.items).isEqualTo(nodeUiItems)
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
