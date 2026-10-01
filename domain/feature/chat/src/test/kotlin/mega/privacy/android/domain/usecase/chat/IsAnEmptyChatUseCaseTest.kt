package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class IsAnEmptyChatUseCaseTest {
    private val chatRepository = mock<ChatRepository>()
    private val underTest = IsAnEmptyChatUseCase(chatRepository)
    private val chatId = 123L
    private val chatListItemLastMessageNormal: ChatListItem = mock {
        on { chatId }.thenReturn(123L)
        on { lastMessageType }.thenReturn(ChatRoomLastMessage.Normal)
    }

    private val chatListItemLastMessageInvalid: ChatListItem = mock {
        on { chatId }.thenReturn(123L)
        on { lastMessageType }.thenReturn(ChatRoomLastMessage.Invalid)
    }

    private val chatListItemLastMessageUnknown: ChatListItem = mock {
        on { chatId }.thenReturn(123L)
        on { lastMessageType }.thenReturn(ChatRoomLastMessage.Unknown)
    }

    @Test
    fun `test that use case returns empty chat because last message is Invalid`() = runTest {
        whenever(chatRepository.getChatListItem(chatId)).thenReturn(chatListItemLastMessageInvalid)
        val actual = underTest(chatId)
        Truth.assertThat(actual).isEqualTo(true)
    }

    @Test
    fun `test that use case returns empty chat because last message is Unknown`() = runTest {
        whenever(chatRepository.getChatListItem(chatId)).thenReturn(chatListItemLastMessageUnknown)
        val actual = underTest(chatId)
        Truth.assertThat(actual).isEqualTo(true)
    }

    @Test
    fun `test that use case does not return an empty chat`() = runTest {
        whenever(chatRepository.getChatListItem(chatId)).thenReturn(chatListItemLastMessageNormal)
        val actual = underTest(chatId)
        Truth.assertThat(actual).isEqualTo(false)
    }
}