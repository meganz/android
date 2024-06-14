package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPendingMessageUseCaseTest {

    private lateinit var underTest: GetPendingMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    private val pendingMessageId = 123L

    @BeforeAll
    fun setup() {
        underTest = GetPendingMessageUseCase(
            chatMessageRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            chatMessageRepository,
        )
    }

    @Test
    fun `test that when repository returns null, use case also returns null`() = runTest {
        whenever(chatMessageRepository.getPendingMessage(pendingMessageId)).thenReturn(null)

        Truth.assertThat(underTest(pendingMessageId)).isNull()
    }

    @Test
    fun `test that when repository returns a pending message, use case also returns it`() =
        runTest {
            val pendingMessage = mock<PendingMessage>()

            whenever(chatMessageRepository.getPendingMessage(pendingMessageId))
                .thenReturn(pendingMessage)

            Truth.assertThat(underTest(pendingMessageId)).isEqualTo(pendingMessage)
        }
}