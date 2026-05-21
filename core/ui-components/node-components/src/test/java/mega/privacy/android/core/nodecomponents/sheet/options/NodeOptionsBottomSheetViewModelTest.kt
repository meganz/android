package mega.privacy.android.core.nodecomponents.sheet.options

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.core.nodecomponents.mapper.OfflineTypedNodeMapper
import mega.privacy.android.core.nodecomponents.mapper.ZipFileTypedNodeMapper
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistry
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.ZipFileTypedNode
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetPublicNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapTypedNodeToPublicLinkUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeUiItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class NodeOptionsBottomSheetViewModelTest {

    private lateinit var viewModel: NodeOptionsBottomSheetViewModel
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getPublicNodeByIdUseCase = mock<GetPublicNodeByIdUseCase>()
    private val getPublicNodeUseCase = mock<GetPublicNodeUseCase>()
    private val mapTypedNodeToPublicLinkUseCase = mock<MapTypedNodeToPublicLinkUseCase>()
    private val nodeBottomSheetActionMapper = mock<NodeBottomSheetActionMapper>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase = mock()
    private val getOfflineFileInformationByIdUseCase = mock<GetOfflineFileInformationByIdUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val monitorNodeUpdatesById = mock<MonitorNodeUpdatesById>()
    private val offlineTypedNodeMapper = mock<OfflineTypedNodeMapper>()
    private val zipFileTypedNodeMapper = mock<ZipFileTypedNodeMapper>()
    private val getFileByPathUseCase = mock<GetFileByPathUseCase>()

    private val sampleFileNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
        on { name } doReturn "test_file.txt"
        on { isIncomingShare } doReturn false
    }

    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val snackbarEventQueue: SnackbarEventQueue = mock()
    private val nodeMenuProviderRegistry = mock<NodeMenuProviderRegistry>()

    @BeforeEach
    fun commonStubs() {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))
        whenever(monitorNodeUpdatesById(any())).thenReturn(emptyFlow())
        whenever(nodeMenuProviderRegistry.getBottomSheetOptions(any())).thenReturn(emptySet())
    }

    private fun initViewModel(
        nodeId: Long = sampleFileNode.id.longValue,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
        partiallyExpand: Boolean = true,
        publicLinkUrl: String? = null,
        localFilePath: String? = null,
    ) {
        viewModel = NodeOptionsBottomSheetViewModel(
            nodeBottomSheetActionMapper = nodeBottomSheetActionMapper,
            getNodeAccessPermission = getNodeAccessPermission,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getPublicNodeByIdUseCase = getPublicNodeByIdUseCase,
            getPublicNodeUseCase = getPublicNodeUseCase,
            mapTypedNodeToPublicLinkUseCase = mapTypedNodeToPublicLinkUseCase,
            nodeUiItemMapper = nodeUiItemMapper,
            offlineTypedNodeMapper = offlineTypedNodeMapper,
            zipFileTypedNodeMapper = zipFileTypedNodeMapper,
            getOfflineFileInformationByIdUseCase = getOfflineFileInformationByIdUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorNodeUpdatesById = monitorNodeUpdatesById,
            snackbarEventQueue = snackbarEventQueue,
            nodeMenuProviderRegistry = nodeMenuProviderRegistry,
            isNodeDeletedFromBackupsUseCase = isNodeDeletedFromBackupsUseCase,
            getFileByPathUseCase = getFileByPathUseCase,
            nodeId = nodeId,
            nodeSourceType = nodeSourceType,
            partiallyExpand = partiallyExpand,
            publicLinkUrl = publicLinkUrl,
            localFilePath = localFilePath,
        )
    }

    @Test
    fun `test that init invokes getNodeByIdUseCase for non folder link source`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        val mockNodeUi = mock<NodeUiItem<TypedNode>>()
        whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(emptyList())

        initViewModel(
            nodeId = sampleFileNode.id.longValue,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        verify(getNodeByIdUseCase).invoke(sampleFileNode.id)
        verify(isNodeInRubbishBinUseCase).invoke(sampleFileNode.id)
        verify(isNodeInBackupsUseCase).invoke(sampleFileNode.id.longValue)
        verify(getNodeAccessPermission).invoke(sampleFileNode.id)
    }

    @Test
    fun `test that init invokes getPublicNodeByIdUseCase for FOLDER_LINK source`() = runTest {
        whenever(getPublicNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        val mockNodeUi = mock<NodeUiItem<TypedNode>>()
        whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(emptyList())

        initViewModel(
            nodeId = sampleFileNode.id.longValue,
            nodeSourceType = NodeSourceType.FOLDER_LINK,
        )

        verify(getPublicNodeByIdUseCase).invoke(sampleFileNode.id)
    }

    @Test
    fun `test that init updates state with node information when successful`() = runTest {
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

        initViewModel(
            nodeId = sampleFileNode.id.longValue,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.node).isEqualTo(mockNodeUi)
            assertThat(state.actions).isNotEmpty()
        }
    }

    @Test
    fun `test that init handles exceptions gracefully`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenThrow(RuntimeException("Network error"))

        initViewModel(
            nodeId = sampleFileNode.id.longValue,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.node).isNull()
            assertThat(state.actions).isEmpty()
        }
    }

    @Test
    fun `test that onConsumeErrorState consumes error`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(null)

        initViewModel(
            nodeId = 999L,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.onConsumeErrorState()

            val finalState = awaitItem()
            assertThat(finalState).isNotNull()
        }
    }

    @Test
    fun `test that on show snackbar should call use case`() = runTest {
        initViewModel()

        val snackbarAttributes = mock<SnackbarAttributes>()
        viewModel.showSnackbar(snackbarAttributes)

        verify(snackbarEventQueue).queueMessage(snackbarAttributes)
    }

    @Test
    fun `test that init starts offline monitoring when source type is OFFLINE`() = runTest {
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))

        initViewModel(
            nodeId = 123L,
            nodeSourceType = NodeSourceType.OFFLINE,
        )

        verify(monitorOfflineNodeUpdatesUseCase).invoke()
    }

    @Test
    fun `test that init does not start offline monitoring when source type is not OFFLINE`() =
        runTest {
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
                    anyOrNull(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(emptyList())

            initViewModel(
                nodeId = 123L,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )

            verify(monitorOfflineNodeUpdatesUseCase, never()).invoke()
        }

    @Test
    fun `test that FILE_LINK source fetches via getPublicNodeUseCase and wraps as PublicLinkFile`() =
        runTest {
            val publicLink = "https://mega.nz/file/abc#xyz"
            val publicFileNode = mock<TypedFileNode>().stub {
                on { id } doReturn NodeId(456)
                on { name } doReturn "public_file.txt"
                on { isIncomingShare } doReturn false
                on { previewPath } doReturn null
            }
            val wrappedPublicNode = mock<PublicLinkFile>().stub {
                on { id } doReturn NodeId(456)
                on { name } doReturn "public_file.txt"
                on { isIncomingShare } doReturn false
                on { previewPath } doReturn null
            }
            val expectedNodeUi = mock<NodeUiItem<TypedNode>>()
            whenever(getPublicNodeUseCase(publicLink)).thenReturn(publicFileNode)
            whenever(mapTypedNodeToPublicLinkUseCase(publicFileNode)).thenReturn(wrappedPublicNode)
            whenever(nodeUiItemMapper(listOf(wrappedPublicNode))).thenReturn(listOf(expectedNodeUi))
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
            whenever(
                nodeBottomSheetActionMapper(
                    any(),
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(emptyList())

            initViewModel(
                nodeId = publicFileNode.id.longValue,
                nodeSourceType = NodeSourceType.FILE_LINK,
                publicLinkUrl = publicLink,
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.node).isEqualTo(expectedNodeUi)
                verify(mapTypedNodeToPublicLinkUseCase).invoke(publicFileNode)
            }
        }

    @Test
    fun `test that FILE_LINK source falls back to getNodeByIdUseCase when publicLinkUrl is null`() =
        runTest {
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
                    anyOrNull(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(emptyList())

            initViewModel(
                nodeId = sampleFileNode.id.longValue,
                nodeSourceType = NodeSourceType.FILE_LINK,
                publicLinkUrl = null,
            )

            verify(getNodeByIdUseCase).invoke(sampleFileNode.id)
            verify(getPublicNodeUseCase, never()).invoke(any())
            verify(mapTypedNodeToPublicLinkUseCase, never()).invoke(any(), anyOrNull())
        }

    @Test
    fun `test that FILE_LINK source falls back to getNodeByIdUseCase when publicLinkUrl is blank`() =
        runTest {
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
                    anyOrNull(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(emptyList())

            initViewModel(
                nodeId = sampleFileNode.id.longValue,
                nodeSourceType = NodeSourceType.FILE_LINK,
                publicLinkUrl = "   ",
            )

            verify(getNodeByIdUseCase).invoke(sampleFileNode.id)
            verify(getPublicNodeUseCase, never()).invoke(any())
            verify(mapTypedNodeToPublicLinkUseCase, never()).invoke(any(), anyOrNull())
        }

    @Test
    fun `test that init starts monitoring node updates for non folder link non public link source`() =
        runTest {
            whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
            whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mock()))
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
            whenever(
                nodeBottomSheetActionMapper(
                    any(),
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(emptyList())

            initViewModel(
                nodeId = sampleFileNode.id.longValue,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )

            verify(monitorNodeUpdatesById).invoke(sampleFileNode.id)
        }

    @Test
    fun `test that init does not monitor node updates for FOLDER_LINK source`() = runTest {
        whenever(getPublicNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        val mockNodeUi = mock<NodeUiItem<TypedNode>>()
        whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(emptyList())

        initViewModel(
            nodeId = sampleFileNode.id.longValue,
            nodeSourceType = NodeSourceType.FOLDER_LINK,
        )

        verify(monitorNodeUpdatesById, never()).invoke(any())
    }

    @Test
    fun `test that init does not monitor node updates for FILE_LINK source`() = runTest {
        whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
        val mockNodeUi = mock<NodeUiItem<TypedNode>>()
        whenever(nodeUiItemMapper(listOf(sampleFileNode))).thenReturn(listOf(mockNodeUi))
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(emptyList())

        initViewModel(
            nodeId = sampleFileNode.id.longValue,
            nodeSourceType = NodeSourceType.FILE_LINK,
        )

        verify(monitorNodeUpdatesById, never()).invoke(any())
    }

    @Test
    fun `test that options are refreshed when node update is received`() = runTest {
        val nodeUpdatesFlow = MutableSharedFlow<List<NodeChanges>>()
        whenever(monitorNodeUpdatesById(any())).thenReturn(nodeUpdatesFlow)
        val initialNode = mock<TypedFileNode>().stub {
            on { id } doReturn NodeId(123)
            on { name } doReturn "test_file.txt"
            on { isIncomingShare } doReturn false
        }
        val updatedNode = mock<TypedFileNode>().stub {
            on { id } doReturn NodeId(123)
            on { name } doReturn "test_file.txt"
            on { isIncomingShare } doReturn false
        }
        val initialNodeUi = mock<NodeUiItem<TypedNode>>()
        val updatedNodeUi = mock<NodeUiItem<TypedNode>>()
        whenever(getNodeByIdUseCase(any()))
            .thenReturn(initialNode)
            .thenReturn(updatedNode)
        whenever(nodeUiItemMapper(listOf(initialNode))).thenReturn(listOf(initialNodeUi))
        whenever(nodeUiItemMapper(listOf(updatedNode))).thenReturn(listOf(updatedNodeUi))
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
        whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(emptyList())

        initViewModel(
            nodeId = sampleFileNode.id.longValue,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.node == null) state = awaitItem()
            assertThat(state.node).isEqualTo(initialNodeUi)
            nodeUpdatesFlow.emit(listOf(NodeChanges.Sensitive))
            var updatedState = awaitItem()
            while (updatedState.node != updatedNodeUi) updatedState = awaitItem()
            assertThat(updatedState.node).isEqualTo(updatedNodeUi)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that init does not invoke getPublicNodeUseCase when publicLinkUrl is blank`() =
        runTest {
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
                    anyOrNull(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(emptyList())

            initViewModel(
                nodeId = sampleFileNode.id.longValue,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                publicLinkUrl = "   ",
            )

            verify(getPublicNodeUseCase, never()).invoke(any())
            verify(getNodeByIdUseCase).invoke(sampleFileNode.id)
        }

    @Test
    fun `test that init does not monitor node updates for VIDEO_PLAYER_ZIP_FILE source`() =
        runTest {
            val localPath = "/data/app/test.mp4"
            val file = File(localPath)
            val zipNode = ZipFileTypedNode(file)
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getFileByPathUseCase(localPath)).thenReturn(file)
            whenever(zipFileTypedNodeMapper(file)).thenReturn(zipNode)
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(
                nodeBottomSheetActionMapper(
                    any(),
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(emptyList())
            whenever(
                nodeUiItemMapper(
                    any(), anyOrNull(), any(), any(), any(), anyOrNull(), anyOrNull(), any()
                )
            ).thenReturn(listOf(mock()))

            initViewModel(
                nodeSourceType = NodeSourceType.VIDEO_PLAYER_ZIP_FILE,
                localFilePath = localPath,
            )

            verify(monitorNodeUpdatesById, never()).invoke(any())
        }

    @Test
    fun `test that init loads ZipFileTypedNode when source type is VIDEO_PLAYER_ZIP_FILE and localFilePath is provided`() =
        runTest {
            val localPath = "/data/app/test.mp4"
            val file = File(localPath)
            val zipNode = ZipFileTypedNode(file)
            val expectedNodeUi = mock<NodeUiItem<TypedNode>>()
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getFileByPathUseCase(localPath)).thenReturn(file)
            whenever(zipFileTypedNodeMapper(file)).thenReturn(zipNode)
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(
                nodeBottomSheetActionMapper(
                    any(),
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any()
                )
            )
                .thenReturn(emptyList())
            whenever(
                nodeUiItemMapper(
                    any(), anyOrNull(), any(), any(), any(), anyOrNull(), anyOrNull(), any()
                )
            ).thenReturn(listOf(expectedNodeUi))

            initViewModel(
                nodeSourceType = NodeSourceType.VIDEO_PLAYER_ZIP_FILE,
                localFilePath = localPath,
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.node).isEqualTo(expectedNodeUi)
            }
        }

    @Test
    fun `test that init does not load ZipFileTypedNode when source type is VIDEO_PLAYER_ZIP_FILE and localFilePath is null`() =
        runTest {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)

            initViewModel(
                nodeSourceType = NodeSourceType.VIDEO_PLAYER_ZIP_FILE,
                localFilePath = null,
            )

            verify(zipFileTypedNodeMapper, never()).invoke(any())
        }

    @Test
    fun `test that init does not load ZipFileTypedNode when getFileByPathUseCase returns null for VIDEO_PLAYER_ZIP_FILE source`() =
        runTest {
            val localPath = "/data/app/test.mp4"
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getFileByPathUseCase(localPath)).thenReturn(null)

            initViewModel(
                nodeSourceType = NodeSourceType.VIDEO_PLAYER_ZIP_FILE,
                localFilePath = localPath,
            )

            verify(zipFileTypedNodeMapper, never()).invoke(any())
        }

    @Test
    fun `test that state actions are grouped by group ascending and sorted by orderInGroup within each group`() =
        runTest {
            val groupTwoOrderOne = NodeActionModeMenuItem(2, 1, mock())
            val groupOneOrderTwo = NodeActionModeMenuItem(1, 2, mock())
            val groupOneOrderOne = NodeActionModeMenuItem(1, 1, mock())
            val groupTwoOrderTwo = NodeActionModeMenuItem(2, 2, mock())
            val groupZeroOrderFive = NodeActionModeMenuItem(0, 5, mock())
            whenever(getNodeByIdUseCase(any())).thenReturn(sampleFileNode)
            whenever(nodeUiItemMapper(listOf(sampleFileNode)))
                .thenReturn(listOf(mock<NodeUiItem<TypedNode>>()))
            whenever(isNodeInRubbishBinUseCase(any())).thenReturn(false)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(getNodeAccessPermission(any())).thenReturn(AccessPermission.FULL)
            whenever(nodeBottomSheetActionMapper(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(
                    listOf(
                        groupTwoOrderOne,
                        groupOneOrderTwo,
                        groupOneOrderOne,
                        groupTwoOrderTwo,
                        groupZeroOrderFive,
                    )
                )

            initViewModel(
                nodeId = sampleFileNode.id.longValue,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.actions).containsExactly(
                    listOf(groupZeroOrderFive),
                    listOf(groupOneOrderOne, groupOneOrderTwo),
                    listOf(groupTwoOrderOne, groupTwoOrderTwo),
                ).inOrder()
            }
        }
}
