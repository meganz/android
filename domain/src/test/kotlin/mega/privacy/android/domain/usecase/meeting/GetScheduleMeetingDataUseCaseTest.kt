package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.GetChatRoom
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetScheduleMeetingDataUseCaseTest {

    private lateinit var underTest: GetScheduleMeetingDataUseCase

    private val getScheduledMeetingByChat = mock<GetScheduledMeetingByChat>()
    private val callRepository = mock<CallRepository>()
    private val getChatRoom = mock<GetChatRoom>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        underTest = GetScheduleMeetingDataUseCase(
            getScheduledMeetingByChat,
            GetNextSchedMeetingOccurrenceUseCase(callRepository),
            getChatRoom,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `test that getMeetingScheduleData return null`() =
        runTest {
            val chatId = 123L
            whenever(getScheduledMeetingByChat(chatId)).thenReturn(null)

            val result = underTest.invoke(chatId) { _, _ -> "" }

            assertThat(result).isNull()
        }

    @Test
    fun `test that getChatRoom is called accordingly`() =
        runTest {
            val chatId = 123L
            val chat = mock<ChatRoom> {
                on { chatId }.thenReturn(chatId)
            }
            whenever(getChatRoom(chatId)).thenReturn(chat)
            whenever(getScheduledMeetingByChat(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            underTest.invoke(chatId) { _, _ -> "" }

            verify(getChatRoom).invoke(chatId)
        }

    @Test
    fun `test that getChatRoom return error accordingly`() =
        runTest {
            val chatId = 123L
            whenever(getChatRoom(chatId)).thenReturn(null)
            whenever(getScheduledMeetingByChat(chatId)).thenReturn(
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
            val now = Instant.now().minus(1L, ChronoUnit.HALF_DAYS).epochSecond
            val meetingOccurrence = ChatScheduledMeetingOccurr(
                schedId = 456L,
                startDateTime = Instant.now().plusSeconds(60).epochSecond,
                endDateTime = Instant.now().plusSeconds(120).epochSecond,
                isCancelled = false
            )

            whenever(getChatRoom(chatId)).thenReturn(mock())
            whenever(getScheduledMeetingByChat(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            whenever(
                callRepository.fetchScheduledMeetingOccurrencesByChat(chatId, now)
            ).thenReturn(listOf(meetingOccurrence))

            underTest.invoke(chatId) { _, _ -> "" }

            verify(callRepository).fetchScheduledMeetingOccurrencesByChat(chatId, now)
        }
}
