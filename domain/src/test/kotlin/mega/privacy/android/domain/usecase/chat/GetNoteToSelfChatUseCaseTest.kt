package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetNoteToSelfChatUseCaseTest {
    private val chatRepository = mock<ChatRepository>()
    private val underTest = GetNoteToSelfChatUseCase(chatRepository)
    private val chatRoom: ChatRoom = mock {
        on { chatId }.thenReturn(123L)
    }

    @Test
    fun `test that use case returns the note to self chat`() = runTest {
        whenever(chatRepository.getNoteToSelfChat()).thenReturn(chatRoom)
        val actual = underTest()
        Truth.assertThat(actual?.chatId).isEqualTo(123L)
    }


    @Test
    fun `test that use case returns null`() = runTest {
        whenever(chatRepository.getNoteToSelfChat()).thenReturn(null)
        val actual = underTest()
        Truth.assertThat(actual?.chatId).isNull()
    }
}