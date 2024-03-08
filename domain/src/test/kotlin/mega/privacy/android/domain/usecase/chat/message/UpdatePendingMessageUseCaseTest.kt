package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdatePendingMessageUseCaseTest() {
    private lateinit var underTest: UpdatePendingMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    fun setup() {
        underTest = UpdatePendingMessageUseCase(chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        Mockito.reset(chatMessageRepository)
    }

    @Test
    fun `test that the chat message repository is called with correct parameter`() = runTest {
        val updatePendingMessageRequest = mock<UpdatePendingMessageStateRequest>()

        underTest(updatePendingMessageRequest)

        verify(chatMessageRepository).updatePendingMessage(updatePendingMessageRequest)
    }
}