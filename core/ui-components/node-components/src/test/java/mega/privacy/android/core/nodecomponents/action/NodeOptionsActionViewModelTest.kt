package mega.privacy.android.core.nodecomponents.action

import mega.privacy.android.core.nodecomponents.action.clickhandler.SingleNodeAction
import mega.privacy.android.core.nodecomponents.action.clickhandler.MultiNodeAction

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeSelectionModeActionMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeSendToChatMessageMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeVersionHistoryRemoveMessageMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.VersionsMenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSelectionModeMenuItem
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistry
import mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.ChatRequestResult
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.GetPathFromNodeContentUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.time.Duration

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeOptionsActionViewModelTest {
    private lateinit var viewModel: NodeOptionsActionViewModel

    private val checkNodesNameCollisionUseCase = mock<CheckNodesNameCollisionUseCase>()
    private val moveNodesUseCase = mock<MoveNodesUseCase>()
    private val copyNodesUseCase = mock<CopyNodesUseCase>()
    private val setCopyLatestTargetPathUseCase = mock<SetCopyLatestTargetPathUseCase>()
    private val setMoveLatestTargetPathUseCase = mock<SetMoveLatestTargetPathUseCase>()
    private val deleteNodeVersionsUseCase = mock<DeleteNodeVersionsUseCase>()
    private val moveRequestMessageMapper = mock<NodeMoveRequestMessageMapper>()
    private val nodeVersionHistoryRemoveMessageMapper =
        mock<NodeVersionHistoryRemoveMessageMapper>()

    private val snackBarHandler = mock<SnackBarHandler>()
    private val checkBackupNodeTypeUseCase: CheckBackupNodeTypeUseCase = mock()
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase = mock()
    private val nodeSendToChatMessageMapper: NodeSendToChatMessageMapper = mock()
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper = mock()
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper = mock()
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase = mock()
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase = mock()
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase = mock()
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase = mock()
    private val sampleNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
    }
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()
    private val createShareKeyUseCase = mock<CreateShareKeyUseCase>()

    // Mock action handlers for testing
    private val mockSingleNodeActionHandler = mock<SingleNodeAction>()
    private val mockMultiNodeActionHandler = mock<MultiNodeAction>()
    private val singleNodeActionHandlers = setOf(mockSingleNodeActionHandler)
    private val multipleNodesActionHandlers = setOf(mockMultiNodeActionHandler)

    private val nodeSelectionModeActionMapper = mock<NodeSelectionModeActionMapper>()
    private val getRubbishNodeUseCase = mock<GetRubbishNodeUseCase>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val checkNodeCanBeMovedToTargetNode = mock<CheckNodeCanBeMovedToTargetNode>()
    private val nodeMenuProviderRegistry = mock<NodeMenuProviderRegistry>()
    private val mockRubbishNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(999L)
    }

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isTakenDown } doReturn false
    }

    private val mockFolderNode = mock<TypedFolderNode> {
        on { id } doReturn NodeId(456L)
        on { isTakenDown } doReturn false
    }

    private val mockNodeSelectionMenuItem = mock<NodeSelectionMenuItem<MenuActionWithIcon>>()
    private val mockNodeSelectionModeMenuItem = mock<NodeSelectionModeMenuItem>()

    private fun initViewModel() {
        viewModel = NodeOptionsActionViewModel(
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            moveRequestMessageMapper = moveRequestMessageMapper,
            versionHistoryRemoveMessageMapper = nodeVersionHistoryRemoveMessageMapper,
            snackBarHandler = snackBarHandler,
            checkBackupNodeTypeUseCase = checkBackupNodeTypeUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            nodeSendToChatMessageMapper = nodeSendToChatMessageMapper,
            nodeHandlesToJsonMapper = nodeHandlesToJsonMapper,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getPathFromNodeContentUseCase = getPathFromNodeContentUseCase,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            applicationScope = applicationScope,
            nodeMenuProviderRegistry = nodeMenuProviderRegistry,
            nodeSelectionModeActionMapper = nodeSelectionModeActionMapper,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
            singleNodeActionHandlers = singleNodeActionHandlers,
            multipleNodesActionHandlers = multipleNodesActionHandlers,
            createShareKeyUseCase = createShareKeyUseCase,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode,
        )
    }

    @BeforeEach
    fun setUpNodeSelectionModeTests() {
        whenever(nodeMenuProviderRegistry.getSelectionModeOptions(any())).thenReturn(setOf(mockNodeSelectionMenuItem))
        getRubbishNodeUseCase.stub {
            onBlocking { invoke() } doReturn mockRubbishNode
        }
        nodeSelectionModeActionMapper.stub {
            onBlocking {
                invoke(
                    options = any(),
                    hasNodeAccessPermission = any(),
                    selectedNodes = any(),
                    allNodeCanBeMovedToTarget = any(),
                    noNodeInBackups = any()
                )
            } doReturn listOf(mockNodeSelectionModeMenuItem)
        }
        isNodeInBackupsUseCase.stub { onBlocking { invoke(any()) } doReturn false }
        getNodeAccessPermission.stub { onBlocking { invoke(any()) } doReturn AccessPermission.FULL }
        checkNodeCanBeMovedToTargetNode.stub { onBlocking { invoke(any(), any()) } doReturn true }
    }

    @AfterEach
    fun resetAllMocks() {
        reset(
            checkNodesNameCollisionUseCase,
            moveNodesUseCase,
            copyNodesUseCase,
            setCopyLatestTargetPathUseCase,
            setMoveLatestTargetPathUseCase,
            deleteNodeVersionsUseCase,
            moveRequestMessageMapper,
            nodeVersionHistoryRemoveMessageMapper,
            snackBarHandler,
            checkBackupNodeTypeUseCase,
            attachMultipleNodesUseCase,
            nodeSendToChatMessageMapper,
            nodeHandlesToJsonMapper,
            nodeContentUriIntentMapper,
            getNodeContentUriUseCase,
            getPathFromNodeContentUseCase,
            getNodePreviewFileUseCase,
            updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase,
            get1On1ChatIdUseCase,
            getBusinessStatusUseCase,
            getFileTypeInfoByNameUseCase,
            createShareKeyUseCase,
            mockSingleNodeActionHandler,
            mockMultiNodeActionHandler,
            nodeSelectionModeActionMapper,
            getRubbishNodeUseCase,
            isNodeInBackupsUseCase,
            getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode,
            mockRubbishNode,
            mockFileNode,
            mockFolderNode,
            mockNodeSelectionMenuItem,
            mockNodeSelectionModeMenuItem,
        )
    }

    @Test
    fun `test that moveNodesUseCase is called when move node method is invoked`() =
        runTest {
            whenever(moveNodesUseCase(emptyMap())).thenThrow(ForeignNodeException())
            initViewModel()
            whenever(moveRequestMessageMapper(any())).thenReturn("Move successful")
            viewModel.moveNodes(emptyMap())
            verify(moveNodesUseCase).invoke(emptyMap())
            viewModel.uiState.test {
                val uiState = awaitItem()
                assertThat(uiState.showForeignNodeDialog)
                    .isInstanceOf(StateEvent.Triggered::class.java)
            }
        }

    @Test
    fun `test that node name collision results are updated properly in uiState`() = runTest {
        whenever(
            checkNodesNameCollisionUseCase(
                nodes = listOf(element = 1).associate { Pair(1, sampleNode.id.longValue) },
                type = NodeNameCollisionType.MOVE,
            ),
        ).thenReturn(
            NodeNameCollisionsResult(
                noConflictNodes = emptyMap(),
                conflictNodes = emptyMap(),
                type = NodeNameCollisionType.MOVE
            )
        )
        initViewModel()
        viewModel.checkNodesNameCollision(
            nodes = listOf(element = 1),
            targetNode = sampleNode.id.longValue,
            type = NodeNameCollisionType.MOVE
        )
        viewModel.uiState.test {
            val uiStateOne = awaitItem()
            assertThat(uiStateOne.nodeNameCollisionsResult).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )
        }
        viewModel.markHandleNodeNameCollisionResult()
        viewModel.uiState.test {
            val uiStateTwo = awaitItem()
            assertThat(uiStateTwo.nodeNameCollisionsResult).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
        }
    }

    @Test
    fun `test that setMoveTargetPath is called when move node is success`() = runTest {
        whenever(moveNodesUseCase(mapOf(sampleNode.id.longValue to sampleNode.id.longValue)))
            .thenReturn(MoveRequestResult.GeneralMovement(0, 0))
        initViewModel()
        whenever(moveRequestMessageMapper(any())).thenReturn("Move successful")
        viewModel.moveNodes(mapOf(sampleNode.id.longValue to sampleNode.id.longValue))
        verify(setMoveLatestTargetPathUseCase).invoke(sampleNode.id.longValue)
    }

    @Test
    fun `test that deleteNodeVersionsUseCase is triggered when delete node history is called`() =
        runTest {
            whenever(deleteNodeVersionsUseCase(sampleNode.id)).thenReturn(Unit)
            whenever(nodeVersionHistoryRemoveMessageMapper(anyOrNull())).thenReturn("")
            initViewModel()
            viewModel.deleteVersionHistory(sampleNode.id.longValue)
            verify(deleteNodeVersionsUseCase).invoke(sampleNode.id)
            verify(nodeVersionHistoryRemoveMessageMapper).invoke(anyOrNull())
            //verify(snackBarHandler).postSnackbarMessage("")
        }

    @Test
    fun `test that copyNodesUseCase is called when copy node method is invoked`() =
        runTest {
            whenever(copyNodesUseCase(emptyMap())).thenThrow(ForeignNodeException())
            initViewModel()
            whenever(moveRequestMessageMapper(any())).thenReturn("Move successful")
            viewModel.copyNodes(emptyMap())
            verify(copyNodesUseCase).invoke(emptyMap())
            viewModel.uiState.test {
                val uiState = awaitItem()
                assertThat(uiState.showForeignNodeDialog)
                    .isInstanceOf(StateEvent.Triggered::class.java)
            }
        }

    @Test
    fun `test that setCopyTargetPath is called when copy node is success`() = runTest {
        whenever(copyNodesUseCase(mapOf(sampleNode.id.longValue to sampleNode.id.longValue)))
            .thenReturn(MoveRequestResult.GeneralMovement(0, 0))
        initViewModel()
        whenever(moveRequestMessageMapper(any())).thenReturn("Move successful")
        viewModel.copyNodes(mapOf(sampleNode.id.longValue to sampleNode.id.longValue))
        verify(setCopyLatestTargetPathUseCase).invoke(sampleNode.id.longValue)
    }

    @Test
    fun `test that contactSelectedForShareFolder is called when contact list is selected`() =
        runTest {
            initViewModel()
            viewModel.contactSelectedForShareFolder(
                listOf("sample@mega.co.nz", "test@mega.co.nz"),
                listOf(1234L, 346L)
            )
            viewModel.uiState.test {
                val uiState = awaitItem()
                assertThat(uiState.contactsData)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
            }
        }

    @Test
    fun `test that chatRequestMessageMapper is called when chatIds is selected`() =
        runTest {
            initViewModel()
            val chatIds = longArrayOf(1234L)
            val nodeIds = longArrayOf(sampleNode.id.longValue)
            val request = ChatRequestResult.ChatRequestAttachNode(
                count = 1,
                errorCount = 0
            )

            whenever(
                attachMultipleNodesUseCase(
                    listOf(sampleNode.id),
                    listOf(1234L)
                )
            ).thenReturn(request)
            whenever(nodeSendToChatMessageMapper(request)).thenReturn("Some value")

            viewModel.attachNodeToChats(
                nodeHandles = nodeIds,
                chatIds = chatIds,
                userHandles = longArrayOf()
            )

            verify(attachMultipleNodesUseCase).invoke(
                listOf(sampleNode.id),
                listOf(1234L)
            )
            verify(nodeSendToChatMessageMapper).invoke(request)
            //verify(snackBarHandler).postSnackbarMessage("Some value")
        }

    @ParameterizedTest(name = "File type is {0}")
    @MethodSource("provideNodeType")
    fun `test that invoke is called when node is provided with different file types`(
        node: TypedFileNode,
        expected: FileNodeContent,
    ) =
        runTest {
            val content = NodeContentUri.LocalContentUri(File("path"))
            whenever(getNodeContentUriUseCase(node)).thenReturn(
                content
            )
            whenever(getNodePreviewFileUseCase(any())).thenReturn(File("path"))

            initViewModel()
            val actual = viewModel.handleFileNodeClicked(node)
            when (node.type) {
                is ImageFileTypeInfo -> {
                    verifyNoMoreInteractions(getNodeContentUriUseCase)
                }

                is TextFileTypeInfo -> {
                    verifyNoMoreInteractions(getNodeContentUriUseCase)
                }

                is UrlFileTypeInfo -> {
                    verify(getNodeContentUriUseCase).invoke(node)
                    verify(getPathFromNodeContentUseCase).invoke(content)
                }

                is VideoFileTypeInfo,
                is PdfFileTypeInfo,
                is AudioFileTypeInfo,
                    -> {
                    verify(getNodeContentUriUseCase).invoke(node)
                }

                else -> {
                    verify(getNodePreviewFileUseCase).invoke(node)
                }
            }
            assertThat(actual).isInstanceOf(expected::class.java)
        }

    @Test
    fun `test that isOnboarding should return true when isPaid is true`() = runTest {
        val accountType = mock<AccountType> {
            on { isPaid } doReturn true
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        initViewModel()
        val result = viewModel.isOnboarding()
        assertThat(result).isTrue()
    }

    @Test
    fun `test that isOnboarding should return false when accountType isPaid is false`() = runTest {
        val accountType = mock<AccountType> {
            on { isPaid } doReturn false
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        initViewModel()
        val result = viewModel.isOnboarding()
        assertThat(result).isFalse()
    }

    @Test
    fun `test that isOnboarding should return false when accountType is null`() = runTest {
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn null
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        initViewModel()
        val result = viewModel.isOnboarding()
        assertThat(result).isFalse()
    }

    @Test
    fun `test that isOnboarding should return false when levelDetail is null`() = runTest {
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn null
        }
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        initViewModel()
        val result = viewModel.isOnboarding()
        assertThat(result).isFalse()
    }

    @Test
    fun `test getTypeInfo returns the type from the mapper`() = runTest {
        initViewModel()
        val file = File("/folder/foo.txt")
        val expected = mock<RawFileTypeInfo>()
        whenever(getFileTypeInfoByNameUseCase(file.name)) doReturn expected
        val actual = viewModel.getTypeInfo(file)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test handleSingleNodeAction executes block when handler is found`() = runTest {
        initViewModel()
        val mockAction = mock<VersionsMenuAction>()
        var blockExecuted = false
        var capturedHandler: SingleNodeAction? = null

        whenever(mockSingleNodeActionHandler.canHandle(mockAction)).thenReturn(true)

        viewModel.handleSingleNodeAction(mockAction) { handler ->
            blockExecuted = true
            capturedHandler = handler
        }

        assertThat(blockExecuted).isTrue()
        assertThat(capturedHandler).isEqualTo(mockSingleNodeActionHandler)
    }

    @Test
    fun `test handleSingleNodeAction throws exception when no handler found`() = runTest {
        val mockAction = mock<VersionsMenuAction>()

        whenever(mockSingleNodeActionHandler.canHandle(mockAction)).thenReturn(false)

        initViewModel()

        assertThrows<IllegalArgumentException> {
            viewModel.handleSingleNodeAction(mockAction) { }
        }
    }

    @Test
    fun `test handleMultipleNodesAction executes block when handler is found`() = runTest {
        val mockAction = mock<MoveMenuAction>()
        var blockExecuted = false
        var capturedHandler: MultiNodeAction? = null

        // Mock the handler to return true for canHandle
        whenever(mockMultiNodeActionHandler.canHandle(mockAction)).thenReturn(true)

        initViewModel()

        viewModel.handleMultipleNodesAction(mockAction) { handler ->
            blockExecuted = true
            capturedHandler = handler
        }

        assertThat(blockExecuted).isTrue()
        assertThat(capturedHandler).isEqualTo(mockMultiNodeActionHandler)
    }

    @Test
    fun `test handleMultipleNodesAction throws exception when no handler found`() = runTest {
        initViewModel()
        val mockAction = mock<MoveMenuAction>()

        whenever(mockMultiNodeActionHandler.canHandle(mockAction)).thenReturn(false)

        assertThrows<IllegalArgumentException> {
            viewModel.handleMultipleNodesAction(mockAction) { }
        }
    }

    @Test
    fun `test handleSingleNodeAction with multiple handlers finds correct handler`() = runTest {
        // Create additional mock handlers
        val mockHandler1 = mock<SingleNodeAction>()
        val mockHandler2 = mock<SingleNodeAction>()
        val mockHandler3 = mock<SingleNodeAction>()

        val multipleHandlers = setOf(mockHandler1, mockHandler2, mockHandler3)
        val viewModelWithMultipleHandlers = NodeOptionsActionViewModel(
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            moveRequestMessageMapper = moveRequestMessageMapper,
            versionHistoryRemoveMessageMapper = nodeVersionHistoryRemoveMessageMapper,
            checkBackupNodeTypeUseCase = checkBackupNodeTypeUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            nodeSendToChatMessageMapper = nodeSendToChatMessageMapper,
            nodeHandlesToJsonMapper = nodeHandlesToJsonMapper,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getPathFromNodeContentUseCase = getPathFromNodeContentUseCase,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            applicationScope = applicationScope,
            nodeMenuProviderRegistry = nodeMenuProviderRegistry,
            nodeSelectionModeActionMapper = nodeSelectionModeActionMapper,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
            singleNodeActionHandlers = multipleHandlers,
            multipleNodesActionHandlers = multipleNodesActionHandlers,
            createShareKeyUseCase = createShareKeyUseCase,
            snackBarHandler = snackBarHandler,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode
        )

        val mockAction = mock<VersionsMenuAction>()
        var capturedHandler: SingleNodeAction? = null

        // Mock handlers 1 and 3 to return false, handler 2 to return true
        whenever(mockHandler1.canHandle(mockAction)).thenReturn(false)
        whenever(mockHandler2.canHandle(mockAction)).thenReturn(true)
        whenever(mockHandler3.canHandle(mockAction)).thenReturn(false)

        viewModelWithMultipleHandlers.handleSingleNodeAction(mockAction) { handler ->
            capturedHandler = handler
        }

        assertThat(capturedHandler).isEqualTo(mockHandler2)
    }

    @Test
    fun `test handleMultipleNodesAction with multiple handlers finds correct handler`() = runTest {
        // Create additional mock handlers
        val mockHandler1 = mock<MultiNodeAction>()
        val mockHandler2 = mock<MultiNodeAction>()
        val mockHandler3 = mock<MultiNodeAction>()

        val multipleHandlers = setOf(mockHandler1, mockHandler2, mockHandler3)
        val viewModelWithMultipleHandlers = NodeOptionsActionViewModel(
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            moveRequestMessageMapper = moveRequestMessageMapper,
            versionHistoryRemoveMessageMapper = nodeVersionHistoryRemoveMessageMapper,
            checkBackupNodeTypeUseCase = checkBackupNodeTypeUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            nodeSendToChatMessageMapper = nodeSendToChatMessageMapper,
            nodeHandlesToJsonMapper = nodeHandlesToJsonMapper,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getPathFromNodeContentUseCase = getPathFromNodeContentUseCase,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            applicationScope = applicationScope,
            nodeMenuProviderRegistry = nodeMenuProviderRegistry,
            nodeSelectionModeActionMapper = nodeSelectionModeActionMapper,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
            singleNodeActionHandlers = singleNodeActionHandlers,
            multipleNodesActionHandlers = multipleHandlers,
            createShareKeyUseCase = createShareKeyUseCase,
            snackBarHandler = snackBarHandler,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode
        )

        val mockAction = mock<MoveMenuAction>()
        var capturedHandler: MultiNodeAction? = null

        // Mock handlers 1 and 3 to return false, handler 2 to return true
        whenever(mockHandler1.canHandle(mockAction)).thenReturn(false)
        whenever(mockHandler2.canHandle(mockAction)).thenReturn(true)
        whenever(mockHandler3.canHandle(mockAction)).thenReturn(false)

        viewModelWithMultipleHandlers.handleMultipleNodesAction(mockAction) { handler ->
            capturedHandler = handler
        }

        assertThat(capturedHandler).isEqualTo(mockHandler2)
    }

    @Test
    fun `test handleSingleNodeAction with empty handlers set throws exception`() = runTest {
        val viewModelWithEmptyHandlers = NodeOptionsActionViewModel(
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            moveRequestMessageMapper = moveRequestMessageMapper,
            versionHistoryRemoveMessageMapper = nodeVersionHistoryRemoveMessageMapper,
            checkBackupNodeTypeUseCase = checkBackupNodeTypeUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            nodeSendToChatMessageMapper = nodeSendToChatMessageMapper,
            nodeHandlesToJsonMapper = nodeHandlesToJsonMapper,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getPathFromNodeContentUseCase = getPathFromNodeContentUseCase,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            applicationScope = applicationScope,
            nodeMenuProviderRegistry = nodeMenuProviderRegistry,
            nodeSelectionModeActionMapper = nodeSelectionModeActionMapper,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
            singleNodeActionHandlers = emptySet(),
            multipleNodesActionHandlers = multipleNodesActionHandlers,
            createShareKeyUseCase = createShareKeyUseCase,
            snackBarHandler = snackBarHandler,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode
        )

        val mockAction = mock<VersionsMenuAction>()

        assertThrows<IllegalArgumentException> {
            viewModelWithEmptyHandlers.handleSingleNodeAction(mockAction) { }
        }
    }

    @Test
    fun `test handleMultipleNodesAction with empty handlers set throws exception`() = runTest {
        val viewModelWithEmptyHandlers = NodeOptionsActionViewModel(
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            moveRequestMessageMapper = moveRequestMessageMapper,
            versionHistoryRemoveMessageMapper = nodeVersionHistoryRemoveMessageMapper,
            checkBackupNodeTypeUseCase = checkBackupNodeTypeUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            nodeSendToChatMessageMapper = nodeSendToChatMessageMapper,
            nodeHandlesToJsonMapper = nodeHandlesToJsonMapper,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getPathFromNodeContentUseCase = getPathFromNodeContentUseCase,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            applicationScope = applicationScope,
            nodeMenuProviderRegistry = nodeMenuProviderRegistry,
            nodeSelectionModeActionMapper = nodeSelectionModeActionMapper,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
            singleNodeActionHandlers = singleNodeActionHandlers,
            multipleNodesActionHandlers = emptySet(),
            createShareKeyUseCase = createShareKeyUseCase,
            snackBarHandler = snackBarHandler,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode
        )

        assertThrows<IllegalArgumentException> {
            viewModelWithEmptyHandlers.handleMultipleNodesAction(mock<MoveMenuAction>()) { }
        }
    }

    @Test
    fun `test handleRenameNodeRequest triggers renameNodeRequestEvent`() = runTest {
        initViewModel()
        val nodeId = NodeId(123L)

        viewModel.handleRenameNodeRequest(nodeId)

        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState.renameNodeRequestEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }

    @Test
    fun `test resetRenameNodeRequest consumes renameNodeRequestEvent`() = runTest {
        initViewModel()
        val nodeId = NodeId(123L)

        // First trigger the event
        viewModel.handleRenameNodeRequest(nodeId)

        // Then reset it
        viewModel.resetRenameNodeRequest()

        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState.renameNodeRequestEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test verifyShareFolderAction triggers shareFolderDialogEvent when node is TypedFolderNode and backup type is not NonBackupNode`() =
        runTest {
            val mockFolderNode = mock<TypedFolderNode>().stub {
                on { id } doReturn NodeId(123L)
            }

            whenever(createShareKeyUseCase(mockFolderNode)).thenReturn(Unit)
            whenever(checkBackupNodeTypeUseCase(mockFolderNode)).thenReturn(BackupNodeType.RootNode)

            initViewModel()

            viewModel.verifyShareFolderAction(mockFolderNode)

            viewModel.uiState.test {
                assertThat(awaitItem().shareFolderDialogEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
            }

            verify(createShareKeyUseCase).invoke(mockFolderNode)
            verify(checkBackupNodeTypeUseCase).invoke(mockFolderNode)
        }

    @Test
    fun `test verifyShareFolderAction triggers shareFolderEvent when node is TypedFolderNode and backup type is NonBackupNode`() =
        runTest {
            val mockFolderNode = mock<TypedFolderNode>().stub {
                on { id } doReturn NodeId(456L)
            }

            whenever(createShareKeyUseCase(mockFolderNode)).thenReturn(Unit)
            whenever(checkBackupNodeTypeUseCase(mockFolderNode)).thenReturn(BackupNodeType.NonBackupNode)

            initViewModel()

            viewModel.verifyShareFolderAction(mockFolderNode)

            viewModel.uiState.test {
                assertThat(awaitItem().shareFolderEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            }

            verify(createShareKeyUseCase).invoke(mockFolderNode)
            verify(checkBackupNodeTypeUseCase).invoke(mockFolderNode)
        }

    @Test
    fun `test verifyShareFolderAction handles createShareKeyUseCase failure gracefully`() =
        runTest {
            val mockFolderNode = mock<TypedFolderNode>().stub {
                on { id } doReturn NodeId(123L)
            }

            whenever(createShareKeyUseCase(mockFolderNode)).thenThrow(RuntimeException("Test exception"))
            whenever(checkBackupNodeTypeUseCase(mockFolderNode)).thenReturn(BackupNodeType.RootNode)

            initViewModel()

            viewModel.verifyShareFolderAction(mockFolderNode)

            viewModel.uiState.test {
                assertThat(awaitItem().shareFolderDialogEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
            }
        }

    @Test
    fun `test verifyShareFolderAction with multiple nodes triggers shareFolderDialogEvent when any node is backup node`() =
        runTest {
            val mockFolderNode1 = mock<TypedFolderNode>().stub {
                on { id } doReturn NodeId(123L)
            }
            val mockFolderNode2 = mock<TypedFolderNode>().stub {
                on { id } doReturn NodeId(456L)
            }
            val mockFileNode = mock<TypedFileNode>().stub {
                on { id } doReturn NodeId(789L)
            }

            whenever(createShareKeyUseCase(mockFolderNode1)).thenReturn(Unit)
            whenever(createShareKeyUseCase(mockFolderNode2)).thenReturn(Unit)
            whenever(checkBackupNodeTypeUseCase(mockFolderNode1)).thenReturn(BackupNodeType.NonBackupNode)
            whenever(checkBackupNodeTypeUseCase(mockFolderNode2)).thenReturn(BackupNodeType.RootNode)

            initViewModel()

            viewModel.verifyShareFolderAction(
                listOf(
                    mockFolderNode1,
                    mockFolderNode2,
                    mockFileNode
                )
            )

            viewModel.uiState.test {
                assertThat(awaitItem().shareFolderDialogEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
            }
        }

    @Test
    fun `test verifyShareFolderAction with multiple nodes triggers shareFolderEvent when no nodes are backup nodes`() =
        runTest {
            val mockFolderNode1 = mock<TypedFolderNode>().stub {
                on { id } doReturn NodeId(123L)
            }
            val mockFolderNode2 = mock<TypedFolderNode>().stub {
                on { id } doReturn NodeId(456L)
            }
            val mockFileNode = mock<TypedFileNode>().stub {
                on { id } doReturn NodeId(789L)
            }

            whenever(createShareKeyUseCase(mockFolderNode1)).thenReturn(Unit)
            whenever(createShareKeyUseCase(mockFolderNode2)).thenReturn(Unit)
            whenever(checkBackupNodeTypeUseCase(mockFolderNode1)).thenReturn(BackupNodeType.NonBackupNode)
            whenever(checkBackupNodeTypeUseCase(mockFolderNode2)).thenReturn(BackupNodeType.NonBackupNode)

            initViewModel()

            viewModel.verifyShareFolderAction(
                listOf(
                    mockFolderNode1,
                    mockFolderNode2,
                    mockFileNode
                )
            )

            viewModel.uiState.test {
                assertThat(awaitItem().shareFolderEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
            }
        }

    @Test
    fun `test that view model initializes with empty state`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().visibleActions).isEmpty()
        }
    }

    @Test
    fun `test that getRubbishBinNode handles failure case gracefully`() = runTest {
        whenever(getRubbishNodeUseCase()).thenThrow(RuntimeException("Test exception"))

        initViewModel()

        // Should not crash and should still initialize
        viewModel.uiState.test {
            awaitItem()
        }
    }

    @Test
    fun `test updateSelectionModeAvailableActions when node cannot be moved to rubbish bin`() =
        runTest {
            whenever(checkNodeCanBeMovedToTargetNode(any(), any())).thenReturn(false)

            initViewModel()

            val selectedNodes = setOf(mockFileNode)
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE

            viewModel.updateSelectionModeAvailableActions(selectedNodes, nodeSourceType)

            // Just ignore because we only want to verify the interactions
            viewModel.uiState.test {
                cancelAndIgnoreRemainingEvents()
            }

            verify(checkNodeCanBeMovedToTargetNode).invoke(mockFileNode.id, mockRubbishNode.id)
            verify(nodeSelectionModeActionMapper).invoke(
                options = setOf(mockNodeSelectionMenuItem),
                hasNodeAccessPermission = true,
                selectedNodes = selectedNodes.toList(),
                allNodeCanBeMovedToTarget = false, // Should be false when node cannot be moved to rubbish bin
                noNodeInBackups = true
            )
        }

    @Test
    fun `test updateSelectionModeAvailableActions when node is in backups sets noNodeInBackups to false`() =
        runTest {
            val expected = true
            whenever(isNodeInBackupsUseCase(any())).thenReturn(expected)

            initViewModel()

            val selectedNodes = setOf(mockFileNode)
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE

            viewModel.updateSelectionModeAvailableActions(selectedNodes, nodeSourceType)

            // Just ignore because we only want to verify the interactions
            viewModel.uiState.test {
                cancelAndIgnoreRemainingEvents()
            }

            verify(nodeSelectionModeActionMapper).invoke(
                options = setOf(mockNodeSelectionMenuItem),
                hasNodeAccessPermission = true,
                selectedNodes = selectedNodes.toList(),
                allNodeCanBeMovedToTarget = true,
                noNodeInBackups = false // Should be false when node is in backups
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test updateSelectionModeAvailableActions limits actions to 4 and adds More when there are more than 4 actions`() =
        runTest {
            // Create 5 mock actions to test the "More" functionality
            val (actions, items) = createMockActions(5)

            initViewModel()

            whenever(
                nodeSelectionModeActionMapper(
                    options = any(),
                    hasNodeAccessPermission = any(),
                    selectedNodes = any(),
                    allNodeCanBeMovedToTarget = any(),
                    noNodeInBackups = any()

                )
            ).thenReturn(items)

            val selectedNodes = setOf(mockFileNode)
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE

            viewModel.uiState.test {
                awaitItem()

                viewModel.updateSelectionModeAvailableActions(selectedNodes, nodeSourceType)

                val finalState = awaitItem()

                // Should have exactly 5 items: first 4 actions + More
                assertThat(finalState.visibleActions).hasSize(5)
                assertThat(finalState.visibleActions[0]).isEqualTo(actions[0])
                assertThat(finalState.visibleActions[1]).isEqualTo(actions[1])
                assertThat(finalState.visibleActions[2]).isEqualTo(actions[2])
                assertThat(finalState.visibleActions[3]).isEqualTo(actions[3])
                assertThat(finalState.visibleActions[4]).isEqualTo(NodeSelectionAction.More)
            }
        }

    @Test
    fun `test updateSelectionModeAvailableActions shows all actions when there are 4 or fewer actions`() =
        runTest {
            val (actions, items) = createMockActions(4)

            initViewModel()

            whenever(
                nodeSelectionModeActionMapper(
                    options = any(),
                    hasNodeAccessPermission = any(),
                    selectedNodes = any(),
                    allNodeCanBeMovedToTarget = any(),
                    noNodeInBackups = any()
                )
            ).thenReturn(items)

            val selectedNodes = setOf(mockFileNode)
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE

            viewModel.uiState.test {
                awaitItem()

                viewModel.updateSelectionModeAvailableActions(selectedNodes, nodeSourceType)

                val finalState = awaitItem()

                // Should have exactly 4 items: all actions, no More
                assertThat(finalState.visibleActions).hasSize(4)
                assertThat(finalState.visibleActions[0]).isEqualTo(actions[0])
                assertThat(finalState.visibleActions[1]).isEqualTo(actions[1])
                assertThat(finalState.visibleActions[2]).isEqualTo(actions[2])
                assertThat(finalState.visibleActions[3]).isEqualTo(actions[3])
                assertThat(finalState.visibleActions).doesNotContain(NodeSelectionAction.More)
            }
        }

    @Test
    fun `test updateSelectionModeAvailableActions shows all actions without more action when there are 3 actions`() =
        runTest {
            // Create 3 mock actions to test normal behavior
            val (actions, items) = createMockActions(3)

            initViewModel()

            whenever(
                nodeSelectionModeActionMapper(
                    options = any(),
                    hasNodeAccessPermission = any(),
                    selectedNodes = any(),
                    allNodeCanBeMovedToTarget = any(),
                    noNodeInBackups = any()
                )
            ).thenReturn(items)

            val selectedNodes = setOf(mockFileNode)
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE

            viewModel.uiState.test {
                awaitItem()

                viewModel.updateSelectionModeAvailableActions(selectedNodes, nodeSourceType)

                val finalState = awaitItem()

                // Should have exactly 3 items: all actions, no More
                assertThat(finalState.visibleActions).hasSize(3)
                assertThat(finalState.visibleActions[0]).isEqualTo(actions[0])
                assertThat(finalState.visibleActions[1]).isEqualTo(actions[1])
                assertThat(finalState.visibleActions[2]).isEqualTo(actions[2])
                assertThat(finalState.visibleActions).doesNotContain(NodeSelectionAction.More)
            }
        }

    @Test
    fun `test updateSelectionModeAvailableActions sets both availableActions and visibleActions correctly`() =
        runTest {
            // Create 5 mock actions to test the More functionality
            val (actions, items) = createMockActions(5)

            initViewModel()

            whenever(
                nodeSelectionModeActionMapper(
                    options = any(),
                    hasNodeAccessPermission = any(),
                    selectedNodes = any(),
                    allNodeCanBeMovedToTarget = any(),
                    noNodeInBackups = any()
                )
            ).thenReturn(items)

            val selectedNodes = setOf(mockFileNode)
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE

            viewModel.uiState.test {
                awaitItem()

                viewModel.updateSelectionModeAvailableActions(selectedNodes, nodeSourceType)

                val finalState = awaitItem()

                // Verify availableActions contains all 5 actions (without More)
                assertThat(finalState.availableActions).hasSize(5)
                assertThat(finalState.availableActions).containsExactly(
                    actions[0],
                    actions[1],
                    actions[2],
                    actions[3],
                    actions[4]
                )
                assertThat(finalState.availableActions).doesNotContain(NodeSelectionAction.More)

                // Verify visibleActions contains first 4 actions + More (since we have 5 > DEFAULT_MAX_VISIBLE_ITEMS)
                assertThat(finalState.visibleActions).hasSize(5)
                assertThat(finalState.visibleActions).containsExactly(
                    actions[0],
                    actions[1],
                    actions[2],
                    actions[3],
                    NodeSelectionAction.More
                )
            }
        }

    @Test
    fun `test updateSelectionModeAvailableActions sets availableActions and visibleActions to same values when actions are 4 or fewer`() =
        runTest {
            // Create 3 mock actions (less than DEFAULT_MAX_VISIBLE_ITEMS)
            val (actions, items) = createMockActions(3)

            initViewModel()

            whenever(
                nodeSelectionModeActionMapper(
                    options = any(),
                    hasNodeAccessPermission = any(),
                    selectedNodes = any(),
                    allNodeCanBeMovedToTarget = any(),
                    noNodeInBackups = any()
                )
            ).thenReturn(items)

            val selectedNodes = setOf(mockFileNode)
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE

            viewModel.uiState.test {
                awaitItem()

                viewModel.updateSelectionModeAvailableActions(selectedNodes, nodeSourceType)

                val finalState = awaitItem()

                // Verify availableActions contains all 3 actions
                assertThat(finalState.availableActions).hasSize(3)
                assertThat(finalState.availableActions).containsExactly(
                    actions[0],
                    actions[1],
                    actions[2]
                )

                // Verify visibleActions is the same as availableActions (no More action added)
                assertThat(finalState.visibleActions).hasSize(3)
                assertThat(finalState.visibleActions).containsExactly(
                    actions[0],
                    actions[1],
                    actions[2]
                )
                assertThat(finalState.visibleActions).doesNotContain(NodeSelectionAction.More)

                // Verify both lists are equal
                assertThat(finalState.visibleActions).isEqualTo(finalState.availableActions)
            }
        }

    /**
     * Creates mock actions and their corresponding menu items for testing
     * @param count Number of mock actions to create
     * @return Pair of (actions, items) where actions is List<MenuActionWithIcon> and items is List<NodeSelectionModeMenuItem>
     */
    private fun createMockActions(count: Int): Pair<List<MenuActionWithIcon>, List<NodeSelectionModeMenuItem>> {
        val maxCount = count.coerceIn(1, 5)
        val actions = listOf(
            mock<MoveMenuAction>(),
            mock<CopyMenuAction>(),
            mock<DownloadMenuAction>(),
            mock<TrashMenuAction>(),
            mock<HideMenuAction>(),
        ).take(maxCount)
        val items = actions
            .take(maxCount)
            .map { action ->
                mock<NodeSelectionModeMenuItem> { on { this.action } doReturn action }
            }

        return actions to items
    }

    private fun provideNodeType() = Stream.of(
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn PdfFileTypeInfo
            },
            mock<FileNodeContent.Pdf>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn VideoFileTypeInfo(
                    extension = "mp4",
                    mimeType = "video",
                    duration = Duration.INFINITE
                )
            },
            mock<FileNodeContent.AudioOrVideo>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn AudioFileTypeInfo(
                    extension = "mp3",
                    mimeType = "audio",
                    duration = Duration.INFINITE
                )
            },
            mock<FileNodeContent.AudioOrVideo>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn StaticImageFileTypeInfo(
                    extension = "jpeg",
                    mimeType = "image",
                )
            },
            mock<FileNodeContent.ImageForNode>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                whenever(it.type).thenReturn(
                    TextFileTypeInfo(
                        mimeType = "text/plain",
                        extension = "txt"
                    )
                )
            },
            mock<FileNodeContent.TextContent>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                whenever(it.type).thenReturn(
                    ZipFileTypeInfo(
                        mimeType = "zip",
                        extension = "zip"
                    )
                )
            }, mock<FileNodeContent.Other>()
        ), Arguments.of(
            mock<TypedFileNode>().stub {
                whenever(it.type).thenReturn(
                    UnknownFileTypeInfo(
                        mimeType = "abc",
                        extension = "abc"
                    )
                )
            }, mock<FileNodeContent.Other>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                whenever(it.type).thenReturn(
                    UrlFileTypeInfo
                )
            },
            mock<FileNodeContent.UrlContent>()
        )
    )
} 