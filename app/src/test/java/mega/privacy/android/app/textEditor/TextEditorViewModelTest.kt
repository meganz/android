package mega.privacy.android.app.textEditor

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorViewModelTest {
    private lateinit var underTest: TextEditorViewModel

    private val checkNodesNameCollisionWithActionUseCase =
        mock<CheckNodesNameCollisionWithActionUseCase>()
    private val checkChatNodesNameCollisionAndCopyUseCase =
        mock<CheckChatNodesNameCollisionAndCopyUseCase>()
    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase> {
        on { invoke() }.thenReturn(monitorNodeUpdatesFakeFlow)
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>() {
        onBlocking {
            invoke(any())
        }.thenReturn(false)
    }
    private val savedStateHandle = mock<SavedStateHandle>()

    @BeforeEach
    fun setUp() {
        underTest = TextEditorViewModel(
            megaApi = mock(),
            megaApiFolder = mock(),
            megaChatApi = mock(),
            checkFileNameCollisionsUseCase = mock(),
            checkNodesNameCollisionWithActionUseCase = checkNodesNameCollisionWithActionUseCase,
            checkChatNodesNameCollisionAndCopyUseCase = checkChatNodesNameCollisionAndCopyUseCase,
            downloadBackgroundFile = mock(),
            ioDispatcher = mock(),
            getNodeByIdUseCase = mock(),
            getChatFileUseCase = getChatFileUseCase,
            getPublicChildNodeFromIdUseCase = mock(),
            getPublicNodeFromSerializedDataUseCase = mock(),
            updateNodeSensitiveUseCase = mock(),
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(checkNodesNameCollisionWithActionUseCase, checkChatNodesNameCollisionAndCopyUseCase)
    }

    @Test
    internal fun `test that copy complete snack bar message is shown when node is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )

            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()

            underTest.onSnackBarMessage().test().assertValue(R.string.context_correctly_copied)
        }

    @Test
    internal fun `test that copy error snack bar message is shown when node is not copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )

            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()

            underTest.onSnackBarMessage().test().assertValue(R.string.context_no_copied)
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ).thenThrow(runtimeException)
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test that move complete snack bar message is shown when node is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_correctly_moved)
            }
        }

    @Test
    internal fun `test that move error snack bar message is shown when node is not moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_no_moved)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ).thenThrow(runtimeException)

            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test that copy complete snack bar message is shown when chat node is imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentNode = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_correctly_copied)
            }
        }


    @Test
    internal fun `test that copy error snack bar message is shown when chat node is not imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentNode = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_no_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when import failed`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L

            val runtimeException = RuntimeException("Import node failed")
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ).thenThrow(runtimeException)

            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentNode = newParentNode,
            )
            advanceUntilIdle()

            underTest.onExceptionThrown().observeOnce {
                assertThat(it).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test that snackbar message is shown when chat file is already available offline`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(true)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.onSnackBarMessage().test().assertValue(R.string.file_already_exists)
        }

    @Test
    internal fun `test that startChatFileOfflineDownload event is triggered when chat file is not available offline`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(false)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem()
                val event = actual.transferEvent
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                val content = (event as StateEventWithContentTriggered).content
                assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
            }
        }

    @Test
    internal fun `test that exception is handled correctly when chat file is not found`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(null)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.onExceptionThrown().test().assertValue {
                it is IllegalStateException
            }
        }


    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}