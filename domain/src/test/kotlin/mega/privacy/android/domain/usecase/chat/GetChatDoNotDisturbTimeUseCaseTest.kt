package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatDoNotDisturbTimeUseCaseTest {
    private lateinit var underTest: GetChatDoNotDisturbTimeUseCase

    private val notificationsRepository = mock<NotificationsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetChatDoNotDisturbTimeUseCase(
            notificationsRepository = notificationsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationsRepository)
    }

    @Test
    fun `test that invoke returns the do not disturb time from repository`() = runTest {
        val chatId = 123L
        val expected = 1234567890L
        whenever(notificationsRepository.getChatDoNotDisturbTime(chatId)).thenReturn(expected)

        val actual = underTest(chatId)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke passes the chat id to repository`() = runTest {
        val chatId = 456L
        whenever(notificationsRepository.getChatDoNotDisturbTime(chatId)).thenReturn(0L)

        underTest(chatId)

        verify(notificationsRepository).getChatDoNotDisturbTime(chatId)
    }
}
