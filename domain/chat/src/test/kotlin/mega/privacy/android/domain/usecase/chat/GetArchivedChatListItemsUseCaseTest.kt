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
class GetArchivedChatListItemsUseCaseTest {

    private lateinit var underTest: GetArchivedChatListItemsUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = GetArchivedChatListItemsUseCase(
            chatRepository = chatRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }

    @Test
    fun `test that the correct archived chat list items is returned and sorted`() = runTest {
        val chatItems = listOf(
            ChatListItem(chatId = 123L, isNoteToSelf = false),
            ChatListItem(chatId = 456L, isNoteToSelf = true),
            ChatListItem(chatId = 789L, isNoteToSelf = false)
        )
        val sortedList = chatItems.sortedByDescending { it.isNoteToSelf }
        whenever(chatRepository.getArchivedChatListItems()) doReturn chatItems

        val actual = underTest()

        assertThat(actual).isEqualTo(sortedList)
    }
}
