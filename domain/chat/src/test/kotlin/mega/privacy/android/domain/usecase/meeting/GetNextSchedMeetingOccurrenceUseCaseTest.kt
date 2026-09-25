package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetNextSchedMeetingOccurrenceUseCaseTest {

    private lateinit var underTest: GetNextSchedMeetingOccurrenceUseCase

    private val fetchScheduledMeetingOccurrencesByChatUseCase =
        mock<FetchScheduledMeetingOccurrencesByChatUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = GetNextSchedMeetingOccurrenceUseCase(
            fetchScheduledMeetingOccurrencesByChatUseCase = fetchScheduledMeetingOccurrencesByChatUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(fetchScheduledMeetingOccurrencesByChatUseCase)
    }

    @Test
    fun `test that getNextSchedMeetingOccurrence returns null when no occurrences are found`() =
        runTest {
            val chatId = 123L

            whenever(
                fetchScheduledMeetingOccurrencesByChatUseCase(eq(chatId), any())
            ).thenReturn(emptyList())

            val result = underTest.invoke(chatId)

            assertThat(result).isNull()
        }

    @Test
    fun `test that getNextSchedMeetingOccurrence returns the correct occurrence`() =
        runTest {
            val now = Instant.now()
            val chatId = 123L
            val occurrence = ChatScheduledMeetingOccurr(
                schedId = 456L,
                startDateTime = now.plusSeconds(3600).epochSecond,
                endDateTime = now.plusSeconds(7200).epochSecond,
                isCancelled = false
            )

            whenever(
                fetchScheduledMeetingOccurrencesByChatUseCase(eq(chatId), any())
            ).thenReturn(listOf(occurrence))

            val result = underTest.invoke(chatId)

            assertThat(result).isEqualTo(occurrence)
        }

    @Test
    fun `test that getNextSchedMeetingOccurrence fetches occurrences since half a day ago`() =
        runTest {
            val chatId = 123L

            whenever(
                fetchScheduledMeetingOccurrencesByChatUseCase(eq(chatId), any())
            ).thenReturn(emptyList())

            val lowerBound = Instant.now().minus(1L, ChronoUnit.HALF_DAYS).epochSecond
            underTest.invoke(chatId)
            val upperBound = Instant.now().minus(1L, ChronoUnit.HALF_DAYS).epochSecond

            val sinceCaptor = argumentCaptor<Long>()
            verify(fetchScheduledMeetingOccurrencesByChatUseCase)
                .invoke(eq(chatId), sinceCaptor.capture())
            assertThat(sinceCaptor.firstValue).isAtLeast(lowerBound)
            assertThat(sinceCaptor.firstValue).isAtMost(upperBound)
        }
}
