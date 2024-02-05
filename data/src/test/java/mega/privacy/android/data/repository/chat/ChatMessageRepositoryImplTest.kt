package mega.privacy.android.data.repository.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatMessageRepositoryImplTest {
    private lateinit var underTest: ChatMessageRepositoryImpl
    private val megaChatApiGateway: MegaChatApiGateway = mock()

    @BeforeAll
    fun setUp() {
        underTest = ChatMessageRepositoryImpl(
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaChatApiGateway
        )
    }

    @Test
    fun `test that setMessageSeen invoke correctly`() = runTest {
        val chatId = 1L
        val messageId = 2L
        underTest.setMessageSeen(chatId, messageId)
        verify(megaChatApiGateway).setMessageSeen(chatId, messageId)
    }

    @Test
    fun `test that getLastMessageSeenId invoke correctly`() = runTest {
        val chatId = 1L
        underTest.getLastMessageSeenId(chatId)
        verify(megaChatApiGateway).getLastMessageSeenId(chatId)
    }
}