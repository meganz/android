package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchScheduledMeetingOccurrencesByChatUseCaseTest {

    private val callRepository: CallRepository = mock()

    private lateinit var underTest: FetchScheduledMeetingOccurrencesByChatUseCase

    @BeforeEach
    fun setUp() {
        underTest = FetchScheduledMeetingOccurrencesByChatUseCase(
            callRepository = callRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(callRepository)
    }

    @Test
    fun `test that the list scheduled meeting occurrences by chat is successfully fetched with the correct arguments`() =
        runTest {
            val chatId = 123L
            val timeUpdateFactor = 789L
            val now = System.currentTimeMillis()
            val since = now - timeUpdateFactor
            val response = listOf(
                ChatScheduledMeetingOccurr(
                    schedId = 456L,
                    startDateTime = now + timeUpdateFactor,
                    endDateTime = now + timeUpdateFactor + 100L,
                    isCancelled = false
                )
            )
            whenever(
                callRepository.fetchScheduledMeetingOccurrencesByChat(
                    chatId = chatId,
                    since = since
                )
            ) doReturn response

            val actual = underTest(chatId = chatId, since = since)

            assertThat(actual).isEqualTo(response)
            verify(callRepository).fetchScheduledMeetingOccurrencesByChat(
                chatId = chatId,
                since = since
            )
        }
}
