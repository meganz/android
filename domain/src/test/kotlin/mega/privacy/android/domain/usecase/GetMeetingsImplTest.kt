package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.GetMeetingsRepository
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

    private val meetingRoomMapper = DefaultMeetingRoomMapper()
    private val getMeetingsRepository = mock<GetMeetingsRepository>()
    private val callRepository = mock<CallRepository>()
    private val chatRepository = mock<ChatRepository>()
    private val mutex = Mutex()

    private val now = Instant.now()
    private val chatRooms = generateChatRooms()
    private val schedMeetingRooms = generateSchedMeetingsRooms()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = GetMeetingsImpl(
            chatRepository = chatRepository,
            callRepository = callRepository,
            getMeetingsRepository = getMeetingsRepository,
            meetingRoomMapper = meetingRoomMapper,
        )

        runBlocking {
            whenever(chatRepository.isChatNotifiable(any())).thenReturn(Random.nextBoolean())
            whenever(chatRepository.isChatLastMessageGeolocation(any())).thenReturn(Random.nextBoolean())
            whenever(chatRepository.monitorMutedChats()).thenReturn(emptyFlow())
            whenever(callRepository.monitorChatCallUpdates()).thenReturn(emptyFlow())
            whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(emptyFlow())
            whenever(callRepository.monitorScheduledMeetingUpdates()).thenReturn(emptyFlow())
            whenever(getMeetingsRepository.getUpdatedMeetingItems(any(), any())).thenReturn(emptyFlow())
            whenever(callRepository.getNextScheduledMeetingOccurrence(any())).thenReturn(null)
            whenever(chatRepository.getMeetingChatRooms()).thenReturn(chatRooms)
            whenever(callRepository.getScheduledMeetingsByChat(any())).thenAnswer {
                schedMeetingRooms.firstOrNull { item -> item.chatId == it.arguments[0] as Long }
                    ?.let(::listOf)
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
            val firstResult = underTest.invoke(mutex).first()

            assertThat(firstResult.first().chatId).isEqualTo(1L)
        }

    @Test
    fun `test that first meeting is sorted as expected`() = runTest {
        val result = underTest.invoke(mutex).take(10).last()

        assertThat(result.first().chatId).isEqualTo(2L)
    }

    @Test
    fun `test that last meeting is sorted as expected`() = runTest {
        val result = underTest.invoke(mutex).take(10).last()

        assertThat(result.last().chatId).isEqualTo(4L)
    }

    @Test
    fun `test that first non sched meeting is sorted as expected`() = runTest {
        val result = underTest.invoke(mutex).take(10).last()

        assertThat(result[3].chatId).isEqualTo(1L)
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
            CombinedChatRoom(
                chatId = 2L,
                title = "Chat room #2",
                lastTimestamp = now.minusSeconds(2 * 3600).epochSecond,
                isArchived = false,
                isActive = true,
            ),
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
            CombinedChatRoom(
                chatId = 5L,
                title = "Chat room #5",
                lastTimestamp = now.minusSeconds(5 * 3600).epochSecond,
                isArchived = false,
                isActive = true,
            ),
        ).shuffled()

    private fun generateSchedMeetingsRooms(): List<ChatScheduledMeeting> =
        listOf(
            ChatScheduledMeeting(
                chatId = 2L,
                schedId = Random.nextLong(),
                title = "Chat room #2",
                startDateTime = now.plusSeconds(2 * 3600).epochSecond,
                endDateTime = now.plusSeconds(2 * 7200).epochSecond,
                rules = generateRandomChatScheduledRules(),
                isCanceled = false,
            ),
            ChatScheduledMeeting(
                chatId = 3L,
                title = "Chat room #3",
                startDateTime = now.plusSeconds(3 * 3600).epochSecond,
                endDateTime = now.plusSeconds(3 * 7200).epochSecond,
                rules = generateRandomChatScheduledRules(),
                isCanceled = false,
            ),
            ChatScheduledMeeting(
                chatId = 5L,
                title = "Chat room #5",
                startDateTime = now.plusSeconds(5 * 3600).epochSecond,
                endDateTime = now.plusSeconds(5 * 7200).epochSecond,
                rules = generateRandomChatScheduledRules(),
                isCanceled = false,
            ),
        )

    private fun generateRandomChatScheduledRules(): ChatScheduledRules? {
        if (Random.nextBoolean()) return null

        val frequencyTypes = OccurrenceFrequencyType.values()
        val weekdays = Weekday.values()
        val until = Random.nextLong(now.plusSeconds(Random.nextLong(86000, 600000)).epochSecond)

        val weekDayList = mutableListOf<Weekday>().apply {
            for (i in 0 until Random.nextInt(weekdays.size - 1)) {
                add(weekdays[i])
            }
        }
        val monthDayList = mutableListOf<Int>().apply {
            for (i in 0 until Random.nextInt(30)) {
                add(i)
            }
        }
        val monthWeekDays = mutableListOf<MonthWeekDayItem>().apply {
            for (i in 0 until Random.nextInt(5)) {
                add(
                    MonthWeekDayItem(
                        weekOfMonth = WeekOfMonth.values()[Random.nextInt(WeekOfMonth.values().size - 1)],
                        weekDaysList = mutableListOf<Weekday>().apply {
                            for (x in 0 until Random.nextInt(weekdays.size - 1)) {
                                add(weekdays[x])
                            }
                        }
                    )
                )
            }
        }

        return ChatScheduledRules(
            freq = frequencyTypes[Random.nextInt(frequencyTypes.size - 1)],
            interval = Random.nextInt(5),
            until = if (Random.nextBoolean()) until else 0,
            weekDayList = if (Random.nextBoolean()) weekDayList else null,
            monthDayList = if (Random.nextBoolean()) monthDayList else null,
            monthWeekDayList = if (Random.nextBoolean()) monthWeekDays else null,
        )
    }
}
