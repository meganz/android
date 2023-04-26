package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class StartConversationUseCaseTest {

    private val chatRepository = mock<ChatRepository>()
    private val underTest = StartConversationUseCase(chatRepository)
    private val existingChatRoomId = Random.nextLong()
    private val newChatRoomId = Random.nextLong()
    private val userHandle = Random.nextLong()

    @Test
    fun `test that start conversation use-case creates new chat if chatroom does not exist`() =
        runTest {
            whenever(chatRepository.getChatRoomByUser(userHandle)).thenReturn(null)
            whenever(
                chatRepository.createChat(
                    isGroup = false,
                    userHandles = listOf(userHandle)
                )
            ).thenReturn(
                newChatRoomId
            )
            val actual = underTest.invoke(isGroup = false, userHandles = listOf(userHandle))
            verify(chatRepository, times(1)).createChat(
                isGroup = false,
                userHandles = listOf(userHandle)
            )
            Truth.assertThat(actual).isEqualTo(newChatRoomId)
        }

    @Test
    fun `test that start conversation use-case returns existing room id if chatRoom exists`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { chatId }.thenReturn(existingChatRoomId)
            }
            whenever(chatRepository.getChatRoomByUser(userHandle)).thenReturn(chatRoom)
            val actual = underTest.invoke(isGroup = false, userHandles = listOf(userHandle))
            verify(chatRepository, times(0)).createChat(
                isGroup = false,
                userHandles = listOf(existingChatRoomId)
            )
            Truth.assertThat(actual).isEqualTo(existingChatRoomId)
        }
}