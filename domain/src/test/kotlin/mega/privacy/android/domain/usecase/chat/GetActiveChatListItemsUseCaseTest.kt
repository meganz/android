package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
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
class GetActiveChatListItemsUseCaseTest {

    private lateinit var underTest: GetActiveChatListItemsUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = GetActiveChatListItemsUseCase(
            chatRepository = chatRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }

    @Test
    fun `test that the correct list of active chat items is returned`() = runTest {
        val chatItems = listOf(ChatListItem(chatId = 123L))
        whenever(chatRepository.getActiveChatListItems()) doReturn chatItems

        val actual = underTest()

        Truth.assertThat(actual).isEqualTo(chatItems)
    }
}
