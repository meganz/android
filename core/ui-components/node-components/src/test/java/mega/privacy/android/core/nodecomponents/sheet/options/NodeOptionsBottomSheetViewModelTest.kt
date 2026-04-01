package mega.privacy.android.core.nodecomponents.sheet.options

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.core.nodecomponents.mapper.OfflineTypedNodeMapper
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistry
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetPublicNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeUiItem
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
    private val getPublicNodeByIdUseCase = mock<GetPublicNodeByIdUseCase>()
    private val nodeBottomSheetActionMapper = mock<NodeBottomSheetActionMapper>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase = mock()
    private val getOfflineFileInformationByIdUseCase = mock<GetOfflineFileInformationByIdUseCase>()
    private val offlineTypedNodeMapper = mock<OfflineTypedNodeMapper>()

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
            getPublicNodeByIdUseCase = getPublicNodeByIdUseCase,
            nodeUiItemMapper = nodeUiItemMapper,
            offlineTypedNodeMapper = offlineTypedNodeMapper,
            getOfflineFileInformationByIdUseCase = getOfflineFileInformationByIdUseCase,
            snackbarEventQueue = snackbarEventQueue,
            nodeMenuProviderRegistry = nodeMenuProviderRegistry,
            isNodeDeletedFromBackupsUseCase = isNodeDeletedFromBackupsUseCase,
        )
    }

    @Test
    fun `test that getBottomSheetOptions invokes getNodeByIdUseCase for non folder link source`() =
        runTest {
            whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
            val mockNodeUi = mock<NodeUiItem<TypedNode>>()
            whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
            whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyList())

            viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

            verify(getNodeByIdUseCase).invoke(sampleFileNode.id)
            verify(isNodeInRubbishBinUseCase).invoke(sampleFileNode.id)
            verify(isNodeInBackupsUseCase).invoke(sampleFileNode.id.longValue)
            verify(getNodeAccessPermission).invoke(sampleFileNode.id)
        }

    @Test
    fun `test that getBottomSheetOptions invokes getFolderLinkNodeByIdUseCase for FOLDER_LINK source`() =
        runTest {
            whenever(getPublicNodeByIdUseCase(any())).thenReturn(sampleFileNode)
            val mockNodeUi = mock<NodeUiItem<TypedNode>>()
            whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
            whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyList())

            viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.FOLDER_LINK)

            verify(getPublicNodeByIdUseCase).invoke(sampleFileNode.id)
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
            whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockActions)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertThat(initialState.node).isNull()
                assertThat(initialState.actions).isEmpty()

                viewModel.getBottomSheetOptions(
                    sampleFileNode.id.longValue,
                    NodeSourceType.CLOUD_DRIVE,
                )

                val updatedState = awaitItem()
                assertThat(updatedState.node).isEqualTo(mockNodeUi)
                assertThat(updatedState.actions).isNotEmpty()
            }
        }

    @Test
    fun `test that getBottomSheetOptions handles exceptions gracefully`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenThrow(RuntimeException("Network error"))

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.node).isNull()
            assertThat(initialState.actions).isEmpty()

            viewModel.getBottomSheetOptions(sampleFileNode.id.longValue, NodeSourceType.CLOUD_DRIVE)

            val updatedState = awaitItem()
            assertThat(updatedState.node).isNull()
            assertThat(updatedState.actions).isEmpty()
        }
    }

    @Test
    fun `test that onConsumeErrorState consumes error`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(null)

        viewModel.uiState.test {
            awaitItem()

            viewModel.getBottomSheetOptions(999L, NodeSourceType.CLOUD_DRIVE)
            awaitItem()

            viewModel.onConsumeErrorState()

            val finalState = awaitItem()
            assertThat(finalState).isNotNull()
        }
    }

    @Test
    fun `test that on show snackbar should call use case`() = runTest {
        val snackbarAttributes = mock<SnackbarAttributes>()
        viewModel.showSnackbar(snackbarAttributes)

        verify(snackbarEventQueue).queueMessage(snackbarAttributes)
    }
}
