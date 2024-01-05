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

    private val chatId = 1L


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
            val chatPublicHandle = 2L

            underTest.invoke(chatId, chatPublicHandle)

            verify(chatRepository).autorejoinPublicChat(chatId, chatPublicHandle)
            verify(chatRepository).setLastPublicHandle(chatId)
        }

    @Test
    fun `test that autojoinPublicChat is called when chat room is not exist`() =
        runTest {
            underTest.invoke(chatId)

            verify(chatRepository).autojoinPublicChat(chatId)
            verify(chatRepository).setLastPublicHandle(chatId)
        }
}