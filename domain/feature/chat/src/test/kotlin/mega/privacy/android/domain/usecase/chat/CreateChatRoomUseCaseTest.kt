package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class CreateChatRoomUseCaseTest {
    private val chatRepository = mock<ChatRepository>()
    private val underTest = CreateChatRoomUseCase(chatRepository)
    private val userHandle = Random.nextLong()
    private val chatId = Random.nextLong()

    @Test
    fun `test that create chatroom use-case returns chat id`() = runTest {
        whenever(
            chatRepository.createChat(
                isGroup = false,
                userHandles = listOf(userHandle)
            )
        ).thenReturn(
            chatId
        )
        val actual = underTest.invoke(isGroup = false, userHandles = listOf(userHandle))
        Truth.assertThat(actual).isEqualTo(chatId)
    }
}