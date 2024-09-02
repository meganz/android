package mega.privacy.android.app.presentation.chat

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.chat.model.MediaPlayerOpenedErrorState
import mega.privacy.android.app.presentation.copynode.CopyRequestState
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaChatMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAttachmentHistoryViewModelTest {
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val checkChatNodesNameCollisionAndCopyUseCase: CheckChatNodesNameCollisionAndCopyUseCase =
        mock()
    private val getNodeContentUriByHandleUseCase: GetNodeContentUriByHandleUseCase = mock()
    private val megaNavigator: MegaNavigator = mock()

    private lateinit var viewModel: NodeAttachmentHistoryViewModel

    @BeforeEach
    fun setup() {
        viewModel = NodeAttachmentHistoryViewModel(
            checkChatNodesNameCollisionAndCopyUseCase,
            monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase,
            getNodeContentUriByHandleUseCase,
            mock(),
            megaNavigator
        )
    }

    @AfterEach
    fun resetMocks() = reset(
        monitorStorageStateEventUseCase,
        isConnectedToInternetUseCase,
        checkChatNodesNameCollisionAndCopyUseCase,
        getNodeContentUriByHandleUseCase,
        megaNavigator
    )

    @Test
    fun `test that _copyResultFlow is updated when import chat nodes is successful and a node is copied`() =
        runTest {
            val chatId = 123L
            val messageIds = mutableListOf(456L, 789L)
            val newNodeParent = 321L
            val result = NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                MoveRequestResult.Copy(1, 0)
            )
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase.invoke(
                    any(),
                    any(),
                    NodeId(any())
                )
            ).thenReturn(result)

            viewModel.importChatNodes(chatId, messageIds, newNodeParent)

            val expectedState =
                CopyRequestState(result = result.moveRequestResult?.toCopyRequestResult())
            assertThat(expectedState).isEqualTo(viewModel.copyResultFlow.value)
        }

    @Test
    fun `test that _copyResultFlow is updated when import chat nodes is failed`() = runTest {
        val chatId = 123L
        val messageIds = mutableListOf(456L, 789L)
        val newNodeParent = 321L
        whenever(checkChatNodesNameCollisionAndCopyUseCase.invoke(any(), any(), NodeId(any())))
            .thenThrow(ForeignNodeException())

        viewModel.importChatNodes(chatId, messageIds, newNodeParent)

        assertThat(viewModel.copyResultFlow.value?.error).isInstanceOf(ForeignNodeException::class.java)
    }

    @Test
    fun `test that _collisionsFlow is updated when import chat nodes is successful and a collision is detected`() =
        runTest {
            val chatId = 123L
            val messageIds = mutableListOf(456L, 789L)
            val newNodeParent = 321L
            val result = NodeNameCollisionWithActionResult(
                collisionResult = NodeNameCollisionsResult(
                    conflictNodes = mapOf(
                        1L to NodeNameCollision.Chat(
                            collisionHandle = 1L,
                            nodeHandle = 2L,
                            name = "name",
                            size = 3L,
                            childFolderCount = 4,
                            childFileCount = 5,
                            lastModified = 6L,
                            parentHandle = 7L,
                            isFile = true,
                            chatId = 8L,
                            messageId = 9L
                        )
                    ),
                    noConflictNodes = emptyMap(),
                    type = NodeNameCollisionType.COPY
                ),
                moveRequestResult = null
            )
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase.invoke(
                    any(),
                    any(),
                    NodeId(any())
                )
            ).thenReturn(result)

            viewModel.importChatNodes(chatId, messageIds, newNodeParent)

            assertThat(viewModel.collisionsFlow.value).hasSize(1)
        }

    @Test
    fun `test that getNodeContentUriByHandleUseCase and openMediaPlayerActivityFromChat functions are invoked as expected`() =
        runTest {
            val paramHandle = 1L
            val paramMessage = mock<MegaChatMessage> {
                on { msgId }.thenReturn(2L)
            }
            val paramChatId = 3L
            val paramName = "name"
            val paramContext = mock<Context>()
            val paramPosition = 1
            val nodeContentUri = NodeContentUri.RemoteContentUri("uri", false)

            whenever(getNodeContentUriByHandleUseCase(any())).thenReturn(nodeContentUri)
            viewModel.openMediaPlayer(
                context = paramContext,
                handle = paramHandle,
                message = paramMessage,
                chatId = paramChatId,
                name = paramName,
                position = paramPosition
            )
            verify(getNodeContentUriByHandleUseCase).invoke(paramHandle)
            verify(megaNavigator).openMediaPlayerActivityFromChat(
                paramContext,
                nodeContentUri,
                paramHandle,
                paramMessage.msgId,
                paramChatId,
                paramName
            )
        }

    @Test
    fun `test that state updates correctly when an exception is thrown`() =
        runTest {
            val paramHandle = 1L
            val paramMessage = mock<MegaChatMessage> {
                on { msgId }.thenReturn(2L)
            }
            val paramChatId = 3L
            val paramName = "name"
            val paramContext = mock<Context>()
            val paramPosition = 1
            whenever(getNodeContentUriByHandleUseCase(any())).thenThrow(IllegalStateException())
            viewModel.openMediaPlayer(
                context = paramContext,
                handle = paramHandle,
                message = paramMessage,
                chatId = paramChatId,
                name = paramName,
                position = paramPosition
            )
            viewModel.mediaPlayerOpenedErrorFlow.test {
                val actual = awaitItem()
                assertThat(actual?.message).isEqualTo(paramMessage)
                assertThat(actual?.position).isEqualTo(paramPosition)
                assertThat(actual?.error).isInstanceOf(IllegalStateException::class.java)
            }
        }

    @Test
    fun `test that MediaPlayerOpenedErrorFlow is updated correctly `() =
        runTest {
            val expectedErrorState = mock<MediaPlayerOpenedErrorState>()
            viewModel.updateMediaPlayerOpenedError(expectedErrorState)
            viewModel.mediaPlayerOpenedErrorFlow.test {
                assertThat(awaitItem()).isEqualTo(expectedErrorState)
                viewModel.updateMediaPlayerOpenedError(null)
                assertThat(awaitItem()).isNull()
            }
        }
}