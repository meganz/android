package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.meeting.MeetingParticipantsResult
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingResult
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.GetMeetingsRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GetMeetingsImplTest {

    private lateinit var underTest: GetMeetings

    private val dispatcher = UnconfinedTestDispatcher()
    private val meetingRoomMapper = DefaultMeetingRoomMapper()
    private val getMeetingsRepository = mock<GetMeetingsRepository>()
    private val chatRepository = mock<ChatRepository>()
    private val monitorChatCallUpdates = mock<MonitorChatCallUpdates>()
    private val monitorScheduledMeetingUpdates = mock<MonitorScheduledMeetingUpdates>()

    private val now = Instant.now()
    private val chatRooms = generateChatRooms()
    private val schedMeetingRooms = generateSchedMeetingsRooms()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        underTest = GetMeetingsImpl(
            chatRepository = chatRepository,
            getMeetingsRepository = getMeetingsRepository,
            meetingRoomMapper = meetingRoomMapper,
            monitorChatCallUpdates = monitorChatCallUpdates,
            monitorScheduledMeetingUpdates = monitorScheduledMeetingUpdates,
            dispatcher = dispatcher
        )

        runBlocking {
            whenever(chatRepository.isChatNotifiable(any())).thenReturn(Random.nextBoolean())
            whenever(chatRepository.isChatLastMessageGeolocation(any())).thenReturn(Random.nextBoolean())
            whenever(chatRepository.monitorMutedChats()).thenReturn(emptyFlow())
            whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(emptyFlow())
            whenever(chatRepository.getMeetingChatRooms()).thenReturn(chatRooms)
            whenever(monitorChatCallUpdates()).thenReturn(emptyFlow())
            whenever(monitorScheduledMeetingUpdates()).thenReturn(emptyFlow())
            whenever(getMeetingsRepository.getMeetingParticipants(any())).thenReturn(MeetingParticipantsResult())
            whenever(getMeetingsRepository.getScheduledMeetingStatus(any())).thenReturn(ScheduledMeetingStatus.NotStarted)
            whenever(getMeetingsRepository.getMeetingScheduleData(any())).thenAnswer {
                schedMeetingRooms.firstOrNull { item -> item.schedId == it.arguments[0] as Long }
            }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that first meeting before retrieving sched meetings is sorted as expected`() =
        runTest {
            val firstResult = underTest.invoke().first()

            assertThat(firstResult.first().chatId).isEqualTo(1L)
        }

    @Test
    fun `test that first meeting is sorted as expected`() = runTest {
        val result = underTest.invoke().take(10).last()

        assertThat(result.first().chatId).isEqualTo(2L)
    }

    @Test
    fun `test that last meeting is sorted as expected`() = runTest {
        val result = underTest.invoke().take(10).last()

        assertThat(result.last().chatId).isEqualTo(4L)
    }

    @Test
    fun `test that first non sched meeting is sorted as expected`() = runTest {
        val result = underTest.invoke().take(10).last()

        assertThat(result[3].chatId).isEqualTo(3L)
    }

    private fun generateChatRooms(): List<CombinedChatRoom> =
        listOf(
            // Non Sched
            CombinedChatRoom(
                chatId = 1L,
                title = "Chat room #1",
                lastTimestamp = now.minusSeconds(1 * 3600).epochSecond,
                isArchived = false,
                isActive = true,
            ),
            // Sched
            CombinedChatRoom(
                chatId = 2L,
                title = "Chat room #2",
                lastTimestamp = now.minusSeconds(2 * 3600).epochSecond,
                isArchived = false,
                isActive = true,
            ),
            // Sched Non Pending
            CombinedChatRoom(
                chatId = 3L,
                title = "Chat room #3",
                lastTimestamp = now.minusSeconds(3 * 3600).epochSecond,
                isArchived = false,
                isActive = true,
            ),
            // Non Sched
            CombinedChatRoom(
                chatId = 4L,
                title = "Chat room #4",
                lastTimestamp = now.minusSeconds(4 * 3600).epochSecond,
                isArchived = false,
                isActive = true,
            ),
            // Sched
            CombinedChatRoom(
                chatId = 5L,
                title = "Chat room #5",
                lastTimestamp = now.minusSeconds(5 * 3600).epochSecond,
                isArchived = false,
                isActive = true,
            ),
        ).shuffled()

    private fun generateSchedMeetingsRooms(): List<ScheduledMeetingResult> =
        listOf(
            ScheduledMeetingResult(
                schedId = 2L,
                isPending = true,
                scheduledStartTimestamp = now.plusSeconds(2 * 3600).epochSecond,
                scheduledEndTimestamp = now.plusSeconds(2 * 7200).epochSecond,
            ),
            ScheduledMeetingResult(
                schedId = 3L,
                isPending = false,
                scheduledStartTimestamp = now.plusSeconds(3 * 3600).epochSecond,
                scheduledEndTimestamp = now.plusSeconds(3 * 7200).epochSecond,
            ),
            ScheduledMeetingResult(
                schedId = 5L,
                isPending = true,
                scheduledStartTimestamp = now.plusSeconds(5 * 3600).epochSecond,
                scheduledEndTimestamp = now.plusSeconds(5 * 7200).epochSecond,
            ),
        )
}
