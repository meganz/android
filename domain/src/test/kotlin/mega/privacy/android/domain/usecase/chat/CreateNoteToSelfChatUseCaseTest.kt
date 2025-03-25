package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class CreateNoteToSelfChatUseCaseTest {
    private val chatRepository = mock<ChatRepository>()
    private val underTest = CreateNoteToSelfChatUseCase(chatRepository)
    private val chatId = 123L

    @Test
    fun `test that use case returns the chat handle`() = runTest {
        whenever(chatRepository.createChat(isGroup = false, userHandles = null)).thenReturn(chatId)
        val actual = underTest()
        Truth.assertThat(actual).isEqualTo(123L)
    }
}