package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetNumUnreadChatsUseCaseTest {
    private val chatRepository: ChatRepository = Mockito.mock(ChatRepository::class.java)

    private lateinit var underTest: GetNumUnreadChatsUseCase

    @Before
    fun setup() {
        underTest = GetNumUnreadChatsUseCase(chatRepository)
    }

    @Test
    fun `test that initial unread count is emitted`() = runTest {
        val initialUnreadCount = 5
        whenever(chatRepository.getNumUnreadChats()).thenReturn(initialUnreadCount)

        val result = underTest.invoke().first()

        assertThat(result).isEqualTo(initialUnreadCount)
    }

    @Test
    fun `test that updated unread count is emitted when chat list item changes`() = runTest {
        val initialUnreadCount = 5
        val updatedUnreadCount = 7
        val chatListItemUpdate = ChatListItem(
            chatId = -1L,
            changes = ChatListItemChanges.UnreadCount
        )

        whenever(chatRepository.getNumUnreadChats())
            .thenReturn(initialUnreadCount)
            .thenReturn(updatedUnreadCount)

        whenever(chatRepository.monitorChatListItemUpdates())
            .thenReturn(flowOf(chatListItemUpdate))

        val result = underTest.invoke().last()

        assertThat(result).isEqualTo(updatedUnreadCount)
    }
}
