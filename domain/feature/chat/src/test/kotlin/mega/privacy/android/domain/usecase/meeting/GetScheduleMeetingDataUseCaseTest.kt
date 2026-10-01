package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
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
internal class GetScheduleMeetingDataUseCaseTest {

    private lateinit var underTest: GetScheduleMeetingDataUseCase

    private val getScheduledMeetingByChatUseCase = mock<GetScheduledMeetingByChatUseCase>()
    private val fetchScheduledMeetingOccurrencesByChatUseCase =
        mock<FetchScheduledMeetingOccurrencesByChatUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetScheduleMeetingDataUseCase(
            getScheduledMeetingByChatUseCase,
            GetNextSchedMeetingOccurrenceUseCase(fetchScheduledMeetingOccurrencesByChatUseCase),
            getChatRoomUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getScheduledMeetingByChatUseCase,
            fetchScheduledMeetingOccurrencesByChatUseCase,
            getChatRoomUseCase,
        )
    }

    @Test
    fun `test that invoke throws IllegalStateException when scheduled meeting does not exist`() =
        runTest {
            val chatId = 123L
            whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(null)

            assertThrows<IllegalStateException> {
                underTest.invoke(chatId) { _, _ -> "" }
            }
        }

    @Test
    fun `test that getChatRoom is called accordingly`() =
        runTest {
            val chatId = 123L
            whenever(getChatRoomUseCase(chatId)).thenReturn(mock())
            whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            underTest.invoke(chatId) { _, _ -> "" }

            verify(getChatRoomUseCase).invoke(chatId)
        }

    @Test
    fun `test that getChatRoom return error accordingly`() =
        runTest {
            val chatId = 123L
            whenever(getChatRoomUseCase(chatId)).thenReturn(null)
            whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            assertThrows<IllegalStateException> {
                underTest.invoke(chatId) { _, _ -> "" }
            }
        }

    @Test
    fun `test that getNextSchedMeetingOccurrence is called accordingly`() =
        runTest {
            val chatId = 123L
            val meetingOccurrence = ChatScheduledMeetingOccurr(
                schedId = 456L,
                startDateTime = Instant.now().plusSeconds(60).epochSecond,
                endDateTime = Instant.now().plusSeconds(120).epochSecond,
                isCancelled = false
            )

            whenever(getChatRoomUseCase(chatId)).thenReturn(mock())
            whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )
            whenever(
                fetchScheduledMeetingOccurrencesByChatUseCase(eq(chatId), any())
            ).thenReturn(listOf(meetingOccurrence))

            val lowerBound = Instant.now().minus(1L, ChronoUnit.HALF_DAYS).epochSecond
            underTest.invoke(chatId) { _, _ -> "" }
            val upperBound = Instant.now().minus(1L, ChronoUnit.HALF_DAYS).epochSecond

            val sinceCaptor = argumentCaptor<Long>()
            verify(fetchScheduledMeetingOccurrencesByChatUseCase)
                .invoke(eq(chatId), sinceCaptor.capture())
            assertThat(sinceCaptor.firstValue).isAtLeast(lowerBound)
            assertThat(sinceCaptor.firstValue).isAtMost(upperBound)
        }
}
