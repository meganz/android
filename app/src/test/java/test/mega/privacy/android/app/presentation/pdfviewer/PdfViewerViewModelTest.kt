package test.mega.privacy.android.app.presentation.pdfviewer

import app.cash.turbine.test
import com.google.common.truth.Truth
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerViewModel
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.node.CopyChatNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension

@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Ignore the unstable test. Will add the tests back once stability issue is resolved.")
internal class PdfViewerViewModelTest {

    private lateinit var underTest: PdfViewerViewModel
    private lateinit var checkNameCollision: CheckNameCollision
    private lateinit var copyNodeUseCase: CopyNodeUseCase
    private lateinit var moveNodeUseCase: MoveNodeUseCase
    private lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase
    private lateinit var copyChatNodeUseCase: CopyChatNodeUseCase
    private lateinit var getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase
    private lateinit var updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        on {
            runBlocking { invoke() }
        }.thenReturn(false)
    }

    @BeforeAll
    fun initialise() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @BeforeEach
    fun setUp() {
        checkNameCollision = mock()
        copyNodeUseCase = mock()
        moveNodeUseCase = mock()
        checkNameCollisionUseCase = mock()
        copyChatNodeUseCase = mock()
        updateNodeSensitiveUseCase = mock()
        underTest = PdfViewerViewModel(
            moveNodeUseCase = moveNodeUseCase,
            checkNameCollision = checkNameCollision,
            copyNodeUseCase = copyNodeUseCase,
            copyChatNodeUseCase = copyChatNodeUseCase,
            checkNameCollisionUseCase = checkNameCollisionUseCase,
            getDataBytesFromUrlUseCase = getDataBytesFromUrlUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
        )
    }

    @AfterAll
    fun tearDown() {
        RxAndroidPlugins.reset()
    }

    @Test
    internal fun `test that copy complete snack bar is shown when file is imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            val nodeToImport = mock<MegaNode>()
            whenever(
                checkNameCollisionUseCase.check(
                    node = nodeToImport,
                    parentHandle = newParentNode.longValue,
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNode
                )
            ).thenReturn(NodeId(1234567890))
            underTest.importChatNode(
                node = nodeToImport,
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.snackBarMessage)
                    .isEqualTo(R.string.context_correctly_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when import failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            val nodeToImport = mock<MegaNode> {
                on { handle }.thenReturn(selectedNode)
            }
            val parentNode = mock<MegaNode>()
            whenever(
                checkNameCollisionUseCase.check(
                    node = nodeToImport,
                    parentHandle = newParentNode.longValue,
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            val runtimeException = RuntimeException("Import node failed")
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNode
                )
            ).thenThrow(runtimeException)
            underTest.importChatNode(
                node = nodeToImport,
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.nodeCopyError).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test move complete snack bar is shown when file is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenReturn(NodeId(selectedNode))
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.snackBarMessage).isEqualTo(R.string.context_correctly_moved)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.MOVE,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                moveNodeUseCase(
                    nodeToMove = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode)
                )
            ).thenThrow(IllegalStateException())
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.nodeMoveError)
                    .isInstanceOf(IllegalStateException::class.java)
            }
        }

    @Test
    internal fun `test copy complete snack bar is shown when file is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenReturn(NodeId(selectedNode))
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.snackBarMessage)
                    .isEqualTo(R.string.context_correctly_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNameCollision(
                    nodeHandle = NodeId(selectedNode),
                    parentHandle = NodeId(newParentNode),
                    type = NameCollisionType.COPY,
                )
            ).thenThrow(MegaNodeException.ChildDoesNotExistsException())
            whenever(
                copyNodeUseCase(
                    nodeToCopy = NodeId(selectedNode),
                    newNodeParent = NodeId(newParentNode), newNodeName = null
                )
            ).thenThrow(IllegalStateException())
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                Truth.assertThat(actual.nodeCopyError)
                    .isInstanceOf(IllegalStateException::class.java)
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}