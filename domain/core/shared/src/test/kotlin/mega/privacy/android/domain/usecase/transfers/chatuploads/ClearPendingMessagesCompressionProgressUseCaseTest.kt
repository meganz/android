package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearPendingMessagesCompressionProgressUseCaseTest {
    private lateinit var underTest: ClearPendingMessagesCompressionProgressUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeAll
    fun setup() {
        underTest = ClearPendingMessagesCompressionProgressUseCase(
            chatMessageRepository
        )
    }

    @BeforeEach
    fun resetMocks() = reset(chatMessageRepository)

    @Test
    fun `test that use case calls the correct method in chat message repository`() = runTest {
        underTest()
        verify(chatMessageRepository).clearPendingMessagesCompressionProgress()
    }
}