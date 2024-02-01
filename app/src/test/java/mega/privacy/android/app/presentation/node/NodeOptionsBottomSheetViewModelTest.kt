package mega.privacy.android.app.presentation.node

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.chat.mapper.ChatRequestMessageMapper
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.model.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.presentation.versions.mapper.VersionHistoryRemoveMessageMapper
import mega.privacy.android.domain.entity.node.ChatRequestResult
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeOptionsBottomSheetViewModelTest {

    private lateinit var viewModel: NodeOptionsBottomSheetViewModel
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val checkNodesNameCollisionUseCase = mock<CheckNodesNameCollisionUseCase>()
    private val moveNodesUseCase = mock<MoveNodesUseCase>()
    private val copyNodesUseCase = mock<CopyNodesUseCase>()
    private val setCopyLatestTargetPathUseCase = mock<SetCopyLatestTargetPathUseCase>()
    private val setMoveLatestTargetPathUseCase = mock<SetMoveLatestTargetPathUseCase>()
    private val deleteNodeVersionsUseCase = mock<DeleteNodeVersionsUseCase>()
    private val moveRequestMessageMapper = mock<MoveRequestMessageMapper>()
    private val versionHistoryRemoveMessageMapper = mock<VersionHistoryRemoveMessageMapper>()
    private val snackBarHandler = mock<SnackBarHandler>()
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase = mock()
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase = mock()
    private val chatRequestMessageMapper: ChatRequestMessageMapper = mock()

    private val sampleNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
    }
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun initialize() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    private fun initViewModel() {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
        viewModel = NodeOptionsBottomSheetViewModel(
            nodeBottomSheetActionMapper = NodeBottomSheetActionMapper(),
            bottomSheetOptions = setOf(),
            getNodeAccessPermission = getNodeAccessPermission,
            isNodeInRubbish = isNodeInRubbish,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            snackBarHandler = snackBarHandler,
            moveRequestMessageMapper = moveRequestMessageMapper,
            versionHistoryRemoveMessageMapper = versionHistoryRemoveMessageMapper,
            applicationScope = applicationScope,
            checkBackupNodeTypeByHandleUseCase = checkBackupNodeTypeByHandleUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            chatRequestMessageMapper = chatRequestMessageMapper
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that get bottom sheet option invokes getNodeByIdUseCase`() = runTest {
        initViewModel()
        viewModel.getBottomSheetOptions(sampleNode.id.longValue)
        verify(getNodeByIdUseCase).invoke(sampleNode.id)
        verify(isNodeInRubbish).invoke(sampleNode.id.longValue)
        verify(isNodeInBackupsUseCase).invoke(sampleNode.id.longValue)
        verify(getNodeAccessPermission).invoke(sampleNode.id)
    }

    @Test
    fun `test that moveNodesUseCase is called when move node method is invoked`() =
        runTest {
            whenever(moveNodesUseCase(emptyMap())).thenThrow(ForeignNodeException())
            initViewModel()
            viewModel.moveNodes(emptyMap())
            verify(moveNodesUseCase).invoke(emptyMap())
            viewModel.state.test {
                val state = awaitItem()
                assertThat(state.showForeignNodeDialog).isInstanceOf(StateEvent.Triggered::class.java)
            }
        }

    @Test
    fun `test that node name collision results are updated properly in state`() = runTest {
        whenever(
            checkNodesNameCollisionUseCase(
                nodes = listOf(element = 1).associate { Pair(1, sampleNode.id.longValue) },
                type = NodeNameCollisionType.MOVE,
            ),
        ).thenReturn(
            NodeNameCollisionResult(
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
        viewModel.state.test {
            val stateOne = awaitItem()
            assertThat(stateOne.nodeNameCollisionResult).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )
        }
        viewModel.markHandleNodeNameCollisionResult()
        viewModel.state.test {
            val stateTwo = awaitItem()
            assertThat(stateTwo.nodeNameCollisionResult).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
        }
    }

    @Test
    fun `test that setMoveTargetPath is called when move node is success`() = runTest {
        whenever(moveNodesUseCase(mapOf(sampleNode.id.longValue to sampleNode.id.longValue)))
            .thenReturn(MoveRequestResult.GeneralMovement(0, 0))
        initViewModel()
        viewModel.moveNodes(mapOf(sampleNode.id.longValue to sampleNode.id.longValue))
        verify(setMoveLatestTargetPathUseCase).invoke(sampleNode.id.longValue)
    }

    @Test
    fun `test that deleteNodeVersionsUseCase is triggered when delete node history is called`() =
        runTest {
            whenever(deleteNodeVersionsUseCase(sampleNode.id)).thenReturn(Unit)
            whenever(versionHistoryRemoveMessageMapper(anyOrNull())).thenReturn("")
            initViewModel()
            viewModel.deleteVersionHistory(sampleNode.id.longValue)
            verify(deleteNodeVersionsUseCase).invoke(sampleNode.id)
            verify(versionHistoryRemoveMessageMapper).invoke(anyOrNull())
            verify(snackBarHandler).postSnackbarMessage("")
        }

    @Test
    fun `test that copyNodesUseCase is called when copy node method is invoked`() =
        runTest {
            whenever(copyNodesUseCase(emptyMap())).thenThrow(ForeignNodeException())
            initViewModel()
            viewModel.copyNodes(emptyMap())
            verify(copyNodesUseCase).invoke(emptyMap())
            viewModel.state.test {
                val state = awaitItem()
                assertThat(state.showForeignNodeDialog).isInstanceOf(StateEvent.Triggered::class.java)
            }
        }

    @Test
    fun `test that setCopyTargetPath is called when copy node is success`() = runTest {
        whenever(copyNodesUseCase(mapOf(sampleNode.id.longValue to sampleNode.id.longValue)))
            .thenReturn(MoveRequestResult.GeneralMovement(0, 0))
        initViewModel()
        viewModel.copyNodes(mapOf(sampleNode.id.longValue to sampleNode.id.longValue))
        verify(setCopyLatestTargetPathUseCase).invoke(sampleNode.id.longValue)
    }

    @Test
    fun `test that contactSelectedForShareFolder is called when contact list is selected`() =
        runTest {
            initViewModel()
            viewModel.contactSelectedForShareFolder(listOf("sample@mega.co.nz", "test@mega.co.nz"))
            viewModel.state.test {
                val state = awaitItem()
                assertThat(state.contactsData).isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    fun `test that chatRequestMessageMapper is called when chatIds is selected`() =
        runTest {
            initViewModel()
            val chatIds = longArrayOf(1234L)
            val request = ChatRequestResult.ChatRequestAttachNode(
                count = 1,
                errorCount = 0
            )

            viewModel.state.value.node?.let {
                whenever(
                    attachMultipleNodesUseCase(
                        listOf(it.id),
                        longArrayOf(1234L)
                    )
                ).thenReturn(request)
                whenever(chatRequestMessageMapper(request)).thenReturn("Some value")

                viewModel.attachNodeToChats(chatIds)

                verify(attachMultipleNodesUseCase).invoke(
                    listOf(it.id),
                    longArrayOf(1234L)
                )
                verify(chatRequestMessageMapper).invoke(request)
                verify(snackBarHandler).postSnackbarMessage("Some value")
            }
        }
}