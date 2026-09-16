package mega.privacy.android.feature.chat.list.menu

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.feature.chat.list.mapper.ChatRoomActionPluralMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTitleMapper
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatRoomActionsTest {

    private val archiveChatUseCase = mock<ArchiveChatUseCase>()
    private val clearChatHistoryUseCase = mock<ClearChatHistoryUseCase>()
    private val chatRoomTitleMapper = mock<ChatRoomTitleMapper>()
    private val chatRoomActionPluralMapper = mock<ChatRoomActionPluralMapper>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    private lateinit var underTest: ChatRoomActions

    @BeforeEach
    fun setUp() {
        reset(
            archiveChatUseCase,
            clearChatHistoryUseCase,
            chatRoomTitleMapper,
            chatRoomActionPluralMapper,
            snackbarEventQueue,
        )
        underTest = ChatRoomActions(
            chatRoomMenuItems = mapOf(
                0 to ClearHistoryChatRoomMenuItem(clearChatHistoryUseCase),
                2 to ArchiveChatRoomMenuItem(archiveChatUseCase, chatRoomTitleMapper),
                3 to UnarchiveChatRoomMenuItem(archiveChatUseCase, chatRoomTitleMapper),
            ),
            chatRoomActionPluralMapper = chatRoomActionPluralMapper,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @Test
    fun `test that resolve returns the eligible actions in their contributed order`() = runTest {
        val actual = underTest.resolve(listOf(chatRoom(chatId = 1L, hasPermissions = true)))

        assertThat(actual.map { it.testTag }).containsExactly(
            CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG,
            CHAT_ROOM_ACTIONS_ARCHIVE_TAG,
        ).inOrder()
    }

    @Test
    fun `test that resolve returns unarchive for an archived chat`() = runTest {
        val actual = underTest.resolve(listOf(chatRoom(chatId = 1L, isArchived = true)))

        assertThat(actual.map { it.testTag }).containsExactly(CHAT_ROOM_ACTIONS_UNARCHIVE_TAG)
    }

    @Test
    fun `test that resolve returns empty when there is no chat room`() = runTest {
        assertThat(underTest.resolve(emptyList())).isEmpty()
    }

    @Test
    fun `test that run does nothing when there is no chat room`() = runTest {
        underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, emptyList())

        verifyNoInteractions(archiveChatUseCase)
        verifyNoInteractions(snackbarEventQueue)
    }

    @Test
    fun `test that run does nothing when the action is unknown`() = runTest {
        underTest.run("unknown", listOf(chatRoom(chatId = 1L)))

        verifyNoInteractions(archiveChatUseCase)
        verifyNoInteractions(snackbarEventQueue)
    }

    @Test
    fun `test that run queues the success message when archiving a single chat succeeds`() =
        runTest {
            val room = chatRoom(chatId = 1L)
            chatRoomTitleMapper.stub { on { invoke(room) } doReturn "Alice" }

            underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, listOf(room))

            verify(archiveChatUseCase)(1L, true)
            verify(snackbarEventQueue).queueMessage(sharedR.string.success_archive_chat, "Alice")
        }

    @Test
    fun `test that run queues the count message when archiving several chats succeeds`() = runTest {
        val rooms = listOf(chatRoom(chatId = 1L), chatRoom(chatId = 2L), chatRoom(chatId = 3L))
        val message = ChatRoomActionMessage.Plural(
            messageId = sharedR.plurals.chat_list_chats_archived_message,
            quantity = 3,
        )
        chatRoomActionPluralMapper.stub { on { invoke(message) } doReturn "3 chats were archived" }

        underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, rooms)

        verify(chatRoomActionPluralMapper)(message)
        verify(snackbarEventQueue).queueMessage("3 chats were archived")
    }

    @Test
    fun `test that run queues the failure message when archiving a single chat fails`() = runTest {
        val room = chatRoom(chatId = 1L)
        chatRoomTitleMapper.stub { on { invoke(room) } doReturn "Alice" }
        archiveChatUseCase.stub { onBlocking { invoke(any(), any()) } doAnswer { throw RuntimeException("boom") } }

        underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, listOf(room))

        verify(snackbarEventQueue).queueMessage(sharedR.string.error_archive_chat, "Alice")
    }

    @Test
    fun `test that run archives the chats it can reach when archiving one of them fails`() =
        runTest {
            val rooms = listOf(chatRoom(chatId = 1L), chatRoom(chatId = 2L), chatRoom(chatId = 3L))
            chatRoomTitleMapper.stub { on { invoke(rooms[1]) } doReturn "Bob" }
            archiveChatUseCase.stub {
                onBlocking { invoke(eq(2L), any()) } doAnswer { throw RuntimeException("boom") }
            }

            underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, rooms)

            verify(archiveChatUseCase)(1L, true)
            verify(archiveChatUseCase)(3L, true)
            verify(snackbarEventQueue).queueMessage(sharedR.string.error_archive_chat, "Bob")
        }

    @Test
    fun `test that run queues the generic failure message when archiving several chats fails`() =
        runTest {
            val rooms = listOf(chatRoom(chatId = 1L), chatRoom(chatId = 2L), chatRoom(chatId = 3L))
            archiveChatUseCase.stub {
                onBlocking { invoke(eq(2L), any()) } doAnswer { throw RuntimeException("boom") }
                onBlocking { invoke(eq(3L), any()) } doAnswer { throw RuntimeException("boom") }
            }

            underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, rooms)

            verify(archiveChatUseCase)(1L, true)
            verify(snackbarEventQueue)
                .queueMessage(sharedR.string.general_request_failed_message)
        }

    @Test
    fun `test that run queues the count message when unarchiving several chats succeeds`() =
        runTest {
            val rooms = listOf(
                chatRoom(chatId = 1L, isArchived = true),
                chatRoom(chatId = 2L, isArchived = true),
                chatRoom(chatId = 3L, isArchived = true),
            )
            val message = ChatRoomActionMessage.Plural(
                messageId = sharedR.plurals.chat_list_chats_unarchived_message,
                quantity = 3,
            )
            chatRoomActionPluralMapper.stub {
                on { invoke(message) } doReturn "3 chats were unarchived"
            }

            underTest.run(CHAT_ROOM_ACTIONS_UNARCHIVE_TAG, rooms)

            verify(chatRoomActionPluralMapper)(message)
            verify(snackbarEventQueue).queueMessage("3 chats were unarchived")
        }

    @Test
    fun `test that run queues the success message when unarchiving succeeds`() = runTest {
        val room = chatRoom(chatId = 1L, isArchived = true)
        chatRoomTitleMapper.stub { on { invoke(room) } doReturn "Alice" }

        underTest.run(CHAT_ROOM_ACTIONS_UNARCHIVE_TAG, listOf(room))

        verify(archiveChatUseCase)(1L, false)
        verify(snackbarEventQueue).queueMessage(sharedR.string.success_unarchive_chat, "Alice")
    }

    @Test
    fun `test that run queues the failure message when unarchiving fails`() = runTest {
        val room = chatRoom(chatId = 1L, isArchived = true)
        chatRoomTitleMapper.stub { on { invoke(room) } doReturn "Alice" }
        archiveChatUseCase.stub { onBlocking { invoke(any(), any()) } doAnswer { throw RuntimeException("boom") } }

        underTest.run(CHAT_ROOM_ACTIONS_UNARCHIVE_TAG, listOf(room))

        verify(snackbarEventQueue).queueMessage(sharedR.string.error_unarchive_chat, "Alice")
    }

    @Test
    fun `test that run rethrows cancellation instead of reporting it as a failed chat when the coroutine running it is cancelled`() =
        runTest {
            val rooms = listOf(chatRoom(chatId = 1L), chatRoom(chatId = 2L), chatRoom(chatId = 3L))
            lateinit var running: Job
            var thrown: Throwable? = null
            archiveChatUseCase.stub {
                onBlocking { invoke(eq(2L), any()) } doAnswer {
                    running.cancel()
                    throw CancellationException("cancelled")
                }
            }

            running = launch {
                thrown = runCatching { underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, rooms) }
                    .exceptionOrNull()
            }
            advanceUntilIdle()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
            verify(archiveChatUseCase, never())(eq(3L), any())
            verifyNoInteractions(snackbarEventQueue)
        }

    @Test
    fun `test that run rethrows cancellation when the coroutine running it is cancelled while the message is queued`() =
        runTest {
            val room = chatRoom(chatId = 1L)
            chatRoomTitleMapper.stub { on { invoke(room) } doReturn "Alice" }
            lateinit var running: Job
            var thrown: Throwable? = null
            snackbarEventQueue.stub {
                onBlocking {
                    queueMessage(eq(sharedR.string.success_archive_chat), any())
                } doAnswer {
                    running.cancel()
                    throw CancellationException("cancelled")
                }
            }

            running = launch {
                thrown =
                    runCatching { underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, listOf(room)) }
                        .exceptionOrNull()
            }
            advanceUntilIdle()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }

    @Test
    fun `test that run reports a timed out chat as failed and archives the rest of the batch`() =
        runTest {
            val rooms = listOf(chatRoom(chatId = 1L), chatRoom(chatId = 2L), chatRoom(chatId = 3L))
            chatRoomTitleMapper.stub { on { invoke(rooms[1]) } doReturn "Bob" }
            archiveChatUseCase.stub {
                onBlocking { invoke(eq(2L), any()) } doSuspendableAnswer {
                    withTimeout(timeMillis = 1L) { awaitCancellation() }
                }
            }

            underTest.run(CHAT_ROOM_ACTIONS_ARCHIVE_TAG, rooms)

            verify(archiveChatUseCase)(1L, true)
            verify(archiveChatUseCase)(3L, true)
            verify(snackbarEventQueue).queueMessage(sharedR.string.error_archive_chat, "Bob")
        }

    @Test
    fun `test that run queues the success message when clearing history succeeds`() = runTest {
        underTest.run(CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG, listOf(chatRoom(chatId = 1L)))

        verify(snackbarEventQueue).queueMessage(sharedR.string.clear_history_success)
    }

    @Test
    fun `test that run queues the failure message when clearing history fails`() = runTest {
        clearChatHistoryUseCase.stub {
            onBlocking { invoke(any()) } doAnswer { throw RuntimeException("boom") }
        }

        underTest.run(CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG, listOf(chatRoom(chatId = 1L)))

        verify(snackbarEventQueue).queueMessage(sharedR.string.clear_history_error)
    }

    private fun chatRoom(
        chatId: Long,
        hasPermissions: Boolean = false,
        isArchived: Boolean = false,
    ) = ChatRoomItem.IndividualChatRoomItem(
        chatId = chatId,
        title = "Chat $chatId",
        hasPermissions = hasPermissions,
        isArchived = isArchived,
    )
}
