package mega.privacy.android.domain.usecase.chat.link

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.*
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinPublicChatUseCaseTest {
    private lateinit var underTest: JoinPublicChatUseCase

    private val chatRepository = mock<ChatRepository>()


    @BeforeAll
    internal fun initialise() {
        underTest = JoinPublicChatUseCase(
            chatRepository = chatRepository,
        )
    }

    @BeforeEach
    fun reset() {
        reset(chatRepository)
    }

    @Test
    fun `test that autorejoinPublicChat is called when chat room is exist`() =
        runTest {
            val chatId = 1L
            val chatPublicHandle = 2L

            underTest.invoke(chatId, chatPublicHandle, true)

            verify(chatRepository).autorejoinPublicChat(chatId, chatPublicHandle)
            verify(chatRepository).setLastPublicHandle(chatId)
        }

    @Test
    fun `test that autojoinPublicChat is called when chat room is not exist`() =
        runTest {
            val chatId = 1L
            val chatPublicHandle = 2L

            underTest.invoke(chatId, chatPublicHandle, false)

            verify(chatRepository).autojoinPublicChat(chatId)
            verify(chatRepository).setLastPublicHandle(chatId)
        }
}