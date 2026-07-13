package mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingSharesChildrenNodeUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.shared.nodes.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeViewItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IncomingSharesExplorerViewModelTest {

    private lateinit var viewModel: IncomingSharesExplorerViewModel

    private val monitorNodeUpdatesByIdUseCase = mock<MonitorNodeUpdatesByIdUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val nodeViewItemMapper = mock<NodeViewItemMapper>()
    private val getIncomingSharesChildrenNodeUseCase = mock<GetIncomingSharesChildrenNodeUseCase>()
    private val searchUseCase = mock<SearchUseCase>()
    private val nodeSourceTypeToSearchTargetMapper = mock<NodeSourceTypeToSearchTargetMapper>()
    private val getNodeNavigationStackUseCase = mock<GetNodeNavigationStackUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            nodeViewItemMapper,
            getIncomingSharesChildrenNodeUseCase,
            searchUseCase,
            nodeSourceTypeToSearchTargetMapper,
        )
        whenever(monitorStorageStateUseCase()) doReturn emptyFlow()
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn emptyFlow()
        whenever(monitorShowHiddenItemsUseCase()) doReturn emptyFlow()
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())) doReturn emptyFlow()
        whenever { getIncomingSharesChildrenNodeUseCase(any(), anyOrNull()) } doReturn emptyList()
        whenever {
            nodeViewItemMapper(any(), any(), anyOrNull(), any(), anyOrNull(), any())
        } doReturn emptyList()
    }

    private fun initViewModel() {
        viewModel = IncomingSharesExplorerViewModel(
            monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            nodeViewItemMapper = nodeViewItemMapper,
            getIncomingSharesChildrenNodeUseCase = getIncomingSharesChildrenNodeUseCase,
            searchUseCase = searchUseCase,
            nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
            getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
            getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>(),
        )
    }

    @Test
    fun `test that nodes are loaded`() = runTest {
        val nodeUiItems = listOf<NodeViewItem<TypedNode>>(mock())
        whenever { getIncomingSharesChildrenNodeUseCase(-1) } doReturn emptyList()
        whenever(
            nodeViewItemMapper(
                nodeList = emptyList(),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                highlightedNodeId = null,
                isHiddenNodesEnabled = false,
                highlightedNames = null,
                isContactVerificationOn = false,
            )
        ) doReturn nodeUiItems

        initViewModel()

        viewModel.uiState.test {
            val actual = awaitDataUntil { it.items.isNotEmpty() }
            assertThat(actual.items).isEqualTo(nodeUiItems)
            cancelAndIgnoreRemainingEvents()
        }
        verify(getIncomingSharesChildrenNodeUseCase).invoke(-1)
    }

    @Test
    fun `test that nodes are refreshed`() = runTest {
        whenever { getIncomingSharesChildrenNodeUseCase(-1) } doReturn emptyList()

        initViewModel()

        viewModel.uiState.test {
            awaitDataUntil { it.nodesLoadingState == NodesLoadingState.FullyLoaded }
            viewModel.refreshNodes()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        verify(getIncomingSharesChildrenNodeUseCase, times(2)).invoke(-1)
    }

    private suspend fun ReceiveTurbine<NodeExplorerUiState>.awaitDataUntil(
        predicate: (NodeExplorerUiState.Data) -> Boolean,
    ): NodeExplorerUiState.Data {
        while (true) {
            val item = awaitItem()
            if (item is NodeExplorerUiState.Data && predicate(item)) return item
        }
    }

}
