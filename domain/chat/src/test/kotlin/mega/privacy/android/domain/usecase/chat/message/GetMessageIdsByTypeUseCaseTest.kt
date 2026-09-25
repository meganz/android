package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMessageIdsByTypeUseCaseTest {
    private lateinit var underTest: GetMessageIdsByTypeUseCase
    private val repository: ChatMessageRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = GetMessageIdsByTypeUseCase(repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that use case return correctly`() = runTest {
        val type = ChatMessageType.NODE_ATTACHMENT
        val chatId = 1L
        whenever(
            repository.getMessageIdsByType(
                chatId,
                type
            )
        ).thenReturn(listOf(1L, 2L, 3L))
        Truth.assertThat(underTest(chatId, type)).isEqualTo(listOf(1L, 2L, 3L))
    }
}