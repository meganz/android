package mega.privacy.android.core.nodecomponents.sheet.options

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.core.nodecomponents.mapper.NodeAccessPermissionIconMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistry
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class NodeOptionsBottomSheetViewModelTest {

    private lateinit var viewModel: NodeOptionsBottomSheetViewModel
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val nodeAccessPermissionIconMapper: NodeAccessPermissionIconMapper = mock()
    private val nodeBottomSheetActionMapper = mock<NodeBottomSheetActionMapper>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase = mock()

    private val sampleFileNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
        on { name } doReturn "test_file.txt"
        on { isIncomingShare } doReturn false
    }

    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val snackbarEventQueue: SnackbarEventQueue = mock()
    private val nodeMenuProviderRegistry = mock<NodeMenuProviderRegistry>()

    @BeforeEach
    fun initViewModel() {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
        whenever(nodeMenuProviderRegistry.getBottomSheetOptions(any())).thenReturn(emptySet())

        viewModel = NodeOptionsBottomSheetViewModel(
            nodeBottomSheetActionMapper = nodeBottomSheetActionMapper,
            getNodeAccessPermission = getNodeAccessPermission,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            nodeUiItemMapper = nodeUiItemMapper,
            snackbarEventQueue = snackbarEventQueue,
            nodeMenuProviderRegistry = nodeMenuProviderRegistry,
            isNodeDeletedFromBackupsUseCase = isNodeDeletedFromBackupsUseCase
        )
    }

    @Test
    fun `test that get bottom sheet option invokes getNodeByIdUseCase`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        val mockNodeUi = mock<NodeUiItem<TypedNode>>()
        whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            emptyList()
        )

        viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

        verify(getNodeByIdUseCase).invoke(sampleFileNode.id)
        verify(isNodeInRubbishBinUseCase).invoke(sampleFileNode.id)
        verify(isNodeInBackupsUseCase).invoke(sampleFileNode.id.longValue)
        verify(getNodeAccessPermission).invoke(sampleFileNode.id)
    }

    @Test
    fun `test that getBottomSheetOptions updates state with node information when successful`() =
        runTest {
            val mockActions = listOf(
                NodeActionModeMenuItem(1, 1, mock()),
                NodeActionModeMenuItem(1, 2, mock())
            )

            whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
            val mockNodeUi = mock<NodeUiItem<TypedNode>>()
            whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
            whenever(
                nodeBottomSheetActionMapper(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(mockActions)
            whenever(nodeAccessPermissionIconMapper(any())).thenReturn(123)

            viewModel.uiState.test {
                // Initial state
                val initialState = awaitItem()
                assertThat(initialState.node).isNull()
                assertThat(initialState.actions).isEmpty()

                // Trigger the action
                viewModel.getBottomSheetOptions(
                    sampleFileNode.id.longValue,
                    NodeSourceType.CLOUD_DRIVE
                )

                // Wait for state update
                val updatedState = awaitItem()
                assertThat(updatedState.node).isEqualTo(mockNodeUi)
                assertThat(updatedState.actions).isNotEmpty()
            }
        }

    @Test
    fun `test that getBottomSheetOptions handles exceptions gracefully`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenThrow(RuntimeException("Network error"))

        viewModel.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertThat(initialState.node).isNull()
            assertThat(initialState.actions).isEmpty()

            // Trigger the action
            viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

            // Wait for state update
            val updatedState = awaitItem()
            assertThat(updatedState.node).isNull()
            assertThat(updatedState.actions).isEmpty()
        }
    }

    @Test
    fun `test that onConsumeErrorState consumes error`() = runTest {
        // First trigger an error by setting up a scenario that would cause an error
        whenever(getNodeByIdUseCase(any())).thenReturn(null)

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Trigger error
            viewModel.getBottomSheetOptions(999L, NodeSourceType.CLOUD_DRIVE)
            awaitItem() // Wait for error state

            // Consume error
            viewModel.onConsumeErrorState()

            // Verify error state is updated
            val finalState = awaitItem()
            assertThat(finalState).isNotNull()
        }
    }

    @Test
    fun `test that getBottomSheetOptions resets state before processing`() = runTest {
        // First set some state
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        val mockNodeUi = mock<NodeUiItem<TypedNode>>()
        whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any())).thenReturn(
            listOf(mock())
        )

        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // First call
            viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

            // Verify state is set
            val firstState = awaitItem()
            assertThat(firstState.node).isNotNull()
            assertThat(firstState.actions).isNotEmpty()

            // Now call with a different node
            val differentNode = mock<TypedFileNode>().stub {
                on { id } doReturn NodeId(999)
                on { name } doReturn "different_file.txt"
                on { isIncomingShare } doReturn false
            }
            val mockNodeUi = mock<NodeUiItem<TypedNode>>()
            whenever(nodeUiItemMapper(listOf(differentNode))).thenReturn(listOf(mockNodeUi))
            whenever(getNodeByIdUseCase(NodeId(999))).thenReturn(differentNode)

            viewModel.getBottomSheetOptions(999L, NodeSourceType.CLOUD_DRIVE)

            // First we get the reset state (node = null, actions = empty)
            val resetState = awaitItem()
            assertThat(resetState.node).isNull()
            assertThat(resetState.actions).isEmpty()

            // Then we get the updated state with the new node
            val secondState = awaitItem()
            assertThat(secondState.node).isEqualTo(mockNodeUi)
        }
    }

    @Test
    fun `test that on show snackbar should call use case`() = runTest {
        val snackbarAttributes = mock<SnackbarAttributes>()
        viewModel.showSnackbar(snackbarAttributes)

        verify(snackbarEventQueue).queueMessage(snackbarAttributes)
    }
}
