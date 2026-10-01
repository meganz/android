package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatListItemUseCaseTest {

    private lateinit var underTest: GetChatListItemUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = GetChatListItemUseCase(
            chatRepository = chatRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }

    @Test
    fun `test that invoke returns the chat list item from the repository`() = runTest {
        val chatId = 123L
        val chatListItem = ChatListItem(
            chatId = chatId,
            title = "Chat title",
            lastMessage = "Last message",
        )
        whenever(chatRepository.getChatListItem(chatId)) doReturn chatListItem

        val actual = underTest(chatId)

        assertThat(actual).isEqualTo(chatListItem)
    }

    @Test
    fun `test that invoke returns null when the chat room does not exist`() = runTest {
        val chatId = 123L
        whenever(chatRepository.getChatListItem(chatId)) doReturn null

        val actual = underTest(chatId)

        assertThat(actual).isNull()
    }
}
