package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.Instant

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
            val pastTimestamp = Instant.now().minusSeconds(43200).epochSecond

            whenever(
                fetchScheduledMeetingOccurrencesByChatUseCase(chatId, pastTimestamp)
            ).thenReturn(emptyList())

            val result = underTest.invoke(chatId)

            assertThat(result).isNull()
        }

    @Test
    fun `test that getNextSchedMeetingOccurrence returns the correct occurrence`() {
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
                fetchScheduledMeetingOccurrencesByChatUseCase(
                    chatId,
                    now.minusSeconds(43200).epochSecond
                )
            ).thenReturn(listOf(occurrence))

            val result = underTest.invoke(chatId)

            assertThat(result).isEqualTo(occurrence)
        }
    }
}
