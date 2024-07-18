package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

class CreateGroupChatRoomUseCaseTest {
    private val chatRepository = mock<ChatRepository>()
    private val createChatLinkUseCase = mock<CreateChatLinkUseCase>()
    private val underTest = CreateGroupChatRoomUseCase(chatRepository, createChatLinkUseCase)
    private val userHandle = Random.nextLong()
    private val chatId = Random.nextLong()

    @AfterEach
    fun tearDown() {
        reset(chatRepository, createChatLinkUseCase)
    }

    @Test
    fun `test that create EKR chatroom use-case returns chat id`() = runTest {
        val emails = listOf("user1@example.com", "user2@example.com")
        val title = "Chat Title"
        val isEkr = true
        val addParticipants = true
        val chatLink = false

        whenever(chatRepository.getContactHandle(emails[0])).thenReturn(userHandle)
        whenever(chatRepository.getContactHandle(emails[1])).thenReturn(userHandle + 1)
        whenever(
            chatRepository.createGroupChat(
                userHandles = listOf(userHandle, userHandle + 1),
                title = title,
                speakRequest = false,
                waitingRoom = false,
                openInvite = addParticipants,
            )
        ).thenReturn(chatId)

        val actual = underTest.invoke(emails, title, isEkr, addParticipants, chatLink)

        assertThat(actual).isEqualTo(chatId)
    }

    @Test
    fun `test that create public chatroom use-case returns chat id`() = runTest {
        val emails = listOf("user1@example.com", "user2@example.com")
        val title = "Chat Title"
        val isEkr = false
        val addParticipants = true
        val chatLink = true

        whenever(chatRepository.getContactHandle(emails[0])).thenReturn(userHandle)
        whenever(chatRepository.getContactHandle(emails[1])).thenReturn(userHandle + 1)
        whenever(
            chatRepository.createPublicChat(
                userHandles = listOf(userHandle, userHandle + 1),
                title = title,
                speakRequest = false,
                waitingRoom = false,
                openInvite = addParticipants,
            )
        ).thenReturn(chatId)

        val actual = underTest.invoke(emails, title, isEkr, addParticipants, chatLink)

        verify(createChatLinkUseCase).invoke(chatId)
        assertThat(actual).isEqualTo(chatId)
    }
}