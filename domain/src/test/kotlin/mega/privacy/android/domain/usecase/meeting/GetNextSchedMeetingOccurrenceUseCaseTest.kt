package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.repository.CallRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetNextSchedMeetingOccurrenceUseCaseTest {

    private lateinit var underTest: GetNextSchedMeetingOccurrenceUseCase

    private val callRepository = mock<CallRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = GetNextSchedMeetingOccurrenceUseCase(callRepository)
    }

    @Test
    fun `test that getNextSchedMeetingOccurrence returns null when no occurrences are found`() =
        runTest {
            val chatId = 123L
            val pastTimestamp = Instant.now().minusSeconds(43200).epochSecond

            whenever(
                callRepository.fetchScheduledMeetingOccurrencesByChat(chatId, pastTimestamp)
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
                callRepository.fetchScheduledMeetingOccurrencesByChat(
                    chatId,
                    now.minusSeconds(43200).epochSecond
                )
            ).thenReturn(listOf(occurrence))

            val result = underTest.invoke(chatId)

            assertThat(result).isEqualTo(occurrence)
        }
    }
}
