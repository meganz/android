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
class GetChatRoomByUserUseCaseTest {
    private val chatRepository = mock<ChatRepository>()
    private val underTest = GetChatRoomByUserUseCase(chatRepository)
    private val testHandle = 123456L
    private val invalidHandle = -1L
    private val chatRoom: ChatRoom = mock {
        on { chatId }.thenReturn(123L)
    }

    @Test
    fun `test that use case returns chatroom when valid user handle is passed`() = runTest {
        whenever(chatRepository.getChatRoomByUser(testHandle)).thenReturn(chatRoom)
        val actual = underTest(testHandle)
        Truth.assertThat(actual?.chatId).isEqualTo(123L)
    }


    @Test
    fun `test that use case returns null when invalid user handle is passed`() = runTest {
        whenever(chatRepository.getChatRoomByUser(invalidHandle)).thenReturn(null)
        val actual = underTest(invalidHandle)
        Truth.assertThat(actual?.chatId).isNull()
    }
}