package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorNoteToSelfChatIsEmptyUseCaseTest {

    private lateinit var underTest: MonitorNoteToSelfChatIsEmptyUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorNoteToSelfChatIsEmptyUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that monitor chat list item update returns true when chat is empty because last message is invalid`() =
        runTest {
            val chatId = 123L
            val isChatEmpty = true
            val chatListItemUpdate = ChatListItem(
                chatId = chatId,
                changes = ChatListItemChanges.LastMessage,
                lastMessageType = ChatRoomLastMessage.Invalid
            )

            whenever(chatRepository.getChatListItem(chatId)).thenReturn(chatListItemUpdate)
            whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(
                flowOf(chatListItemUpdate)
            )

            val result = underTest.invoke(chatId).last()
            assertThat(result).isEqualTo(isChatEmpty)
        }

    @Test
    fun `test that monitor chat list item update returns true when chat is empty because last message is unknown`() =
        runTest {
            val chatId = 123L
            val isChatEmpty = true
            val chatListItemUpdate = ChatListItem(
                chatId = chatId,
                changes = ChatListItemChanges.LastMessage,
                lastMessageType = ChatRoomLastMessage.Unknown
            )

            whenever(chatRepository.getChatListItem(chatId)).thenReturn(chatListItemUpdate)
            whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(
                flowOf(chatListItemUpdate)
            )

            val result = underTest.invoke(chatId).last()
            assertThat(result).isEqualTo(isChatEmpty)
        }

    @Test
    fun `test that monitor chat list item update returns true when chat is not empty`() =
        runTest {
            val chatId = 123L
            val isChatEmpty = false
            val chatListItemUpdate = ChatListItem(
                chatId = chatId,
                changes = ChatListItemChanges.LastMessage,
                lastMessageType = ChatRoomLastMessage.ChatTitle
            )

            whenever(chatRepository.getChatListItem(chatId)).thenReturn(chatListItemUpdate)
            whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(
                flowOf(chatListItemUpdate)
            )

            val result = underTest.invoke(chatId).last()
            assertThat(result).isEqualTo(isChatEmpty)
        }
}