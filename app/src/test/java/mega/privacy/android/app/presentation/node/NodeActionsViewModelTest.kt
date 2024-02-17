package mega.privacy.android.app.presentation.node

import app.cash.turbine.test
import com.google.common.truth.Truth
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.chat.mapper.ChatRequestMessageMapper
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.model.mapper.NodeAccessPermissionIconMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.presentation.versions.mapper.VersionHistoryRemoveMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.ChatRequestResult
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeActionsViewModelTest {
    private lateinit var viewModel: NodeActionsViewModel

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
    private val nodeAccessPermissionIconMapper: NodeAccessPermissionIconMapper = mock()
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper = mock()
    private val sampleNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
    }
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

    private fun initViewModel() {
        viewModel = NodeActionsViewModel(
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            moveRequestMessageMapper = moveRequestMessageMapper,
            versionHistoryRemoveMessageMapper = versionHistoryRemoveMessageMapper,
            snackBarHandler = snackBarHandler,
            checkBackupNodeTypeByHandleUseCase = checkBackupNodeTypeByHandleUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            chatRequestMessageMapper = chatRequestMessageMapper,
            nodeAccessPermissionIconMapper = nodeAccessPermissionIconMapper,
            listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
            applicationScope = applicationScope
        )
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
                Truth.assertThat(state.showForeignNodeDialog)
                    .isInstanceOf(StateEvent.Triggered::class.java)
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
            Truth.assertThat(stateOne.nodeNameCollisionResult).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )
        }
        viewModel.markHandleNodeNameCollisionResult()
        viewModel.state.test {
            val stateTwo = awaitItem()
            Truth.assertThat(stateTwo.nodeNameCollisionResult).isInstanceOf(
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
                Truth.assertThat(state.showForeignNodeDialog)
                    .isInstanceOf(StateEvent.Triggered::class.java)
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
            viewModel.contactSelectedForShareFolder(
                listOf("sample@mega.co.nz", "test@mega.co.nz"),
                listOf(1234L, 346L)
            )
            viewModel.state.test {
                val state = awaitItem()
                Truth.assertThat(state.contactsData)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
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
                    longArrayOf(1234L)
                )
            ).thenReturn(request)
            whenever(chatRequestMessageMapper(request)).thenReturn("Some value")

            viewModel.attachNodeToChats(nodeHandles = nodeIds, chatIds = chatIds)

            verify(attachMultipleNodesUseCase).invoke(
                listOf(sampleNode.id),
                longArrayOf(1234L)
            )
            verify(chatRequestMessageMapper).invoke(request)
            verify(snackBarHandler).postSnackbarMessage("Some value")
        }
}

