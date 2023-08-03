package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.data.mapper.chat.MegaChatPeerListMapper
import mega.privacy.android.data.mapper.meeting.ChatCallMapper
import mega.privacy.android.data.mapper.meeting.ChatScheduledMeetingMapper
import mega.privacy.android.data.mapper.meeting.ChatScheduledMeetingOccurrMapper
import mega.privacy.android.data.mapper.meeting.ChatSessionChangesMapper
import mega.privacy.android.data.mapper.meeting.ChatSessionMapper
import mega.privacy.android.data.mapper.meeting.ChatSessionStatusMapper
import mega.privacy.android.data.mapper.meeting.ChatSessionTermCodeMapper
import mega.privacy.android.data.mapper.meeting.MegaChatCallStatusMapper
import mega.privacy.android.data.mapper.meeting.MegaChatScheduledMeetingFlagsMapper
import mega.privacy.android.data.mapper.meeting.MegaChatScheduledMeetingRulesMapper
import mega.privacy.android.data.model.meeting.ChatCallUpdate
import mega.privacy.android.data.model.ScheduledMeetingUpdate
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ResultOccurrenceUpdate
import mega.privacy.android.domain.repository.CallRepository
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledMeetingOccurr
import nz.mega.sdk.MegaChatScheduledMeetingOccurrList
import nz.mega.sdk.MegaHandleList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random


@OptIn(ExperimentalCoroutinesApi::class)
class CallRepositoryImplTest {

    private lateinit var underTest: CallRepository
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val chatCallMapper = mock<ChatCallMapper>()
    private val megaChatRequest = mock<MegaChatRequest>()
    private val chatRequestMapper = mock<ChatRequestMapper>()
    private val megaChatCallStatusMapper = MegaChatCallStatusMapper()
    private val chatSessionChangesMapper = ChatSessionChangesMapper()
    private val chatSessionStatusMapper = ChatSessionStatusMapper()
    private val chatSessionTermCodeMapper = ChatSessionTermCodeMapper()
    private val chatSessionMapper = ChatSessionMapper(
        chatSessionChangesMapper, chatSessionStatusMapper, chatSessionTermCodeMapper,
    )
    private val handleListMapper = HandleListMapper()
    private val chatScheduledMeetingMapper = mock<ChatScheduledMeetingMapper>()
    private val chatScheduledMeetingOccurrMapper = mock<ChatScheduledMeetingOccurrMapper>()
    private val megaChatScheduledMeetingRulesMapper = mock<MegaChatScheduledMeetingRulesMapper>()
    private val megaChatScheduledMeetingFlagsMapper = mock<MegaChatScheduledMeetingFlagsMapper>()
    private val megaChatPeerListMapper = mock<MegaChatPeerListMapper>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val megaChatScheduledMeeting = mock<MegaChatScheduledMeeting>()
    private val chatScheduledMeeting = mock<ChatScheduledMeeting>()
    private val megaChatScheduledMeetingOccurr = mock<MegaChatScheduledMeetingOccurr>()
    private val chatScheduledMeetingOccurr = mock<ChatScheduledMeetingOccurr>()
    private val megaChatScheduledMeetingList = listOf(megaChatScheduledMeeting)
    private val chatScheduledMeetingList = listOf(chatScheduledMeeting)

    private val resultRequestOccurs = mock<MegaChatScheduledMeetingOccurrList>()

    private val chatId = Random.nextLong()
    private val megaChatRoom = mock<MegaChatRoom>()
    private val callId = Random.nextLong()
    private val megaChatCall = mock<MegaChatCall>()
    private val chatCall: ChatCall = mock()
    private val mockMegaHandleList = mock<MegaHandleList> {
        on { get(0) }.thenReturn(0)
        on { get(1) }.thenReturn(1)
        on { size() }.thenReturn(2)
    }

    private val schedId = Random.nextLong()
    private val chatRequest = mock<ChatRequest>()
    private val audio: Boolean = false
    private val video: Boolean = false
    private val since: Long = Random.nextLong()
    private val count: Int = 20
    private var lastTimeStamp = 0L

    private val megaChatErrorSuccess = mock<MegaChatError> {
        on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
    }

    private val megaChatRequestOccursSuccess = mock<MegaChatRequest> {
        on { megaChatScheduledMeetingOccurrList }.thenReturn(
            resultRequestOccurs
        )
    }

    private val appEventGateway = mock<AppEventGateway>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        underTest = CallRepositoryImpl(
            megaChatApiGateway = megaChatApiGateway,
            chatCallMapper = chatCallMapper,
            chatRequestMapper = chatRequestMapper,
            chatScheduledMeetingMapper = chatScheduledMeetingMapper,
            chatScheduledMeetingOccurrMapper = chatScheduledMeetingOccurrMapper,
            dispatcher = testDispatcher,
            megaChatCallStatusMapper = megaChatCallStatusMapper,
            handleListMapper = handleListMapper,
            chatSessionMapper = chatSessionMapper,
            megaChatScheduledMeetingFlagsMapper = megaChatScheduledMeetingFlagsMapper,
            megaChatScheduledMeetingRulesMapper = megaChatScheduledMeetingRulesMapper,
            megaChatPeerListMapper = megaChatPeerListMapper,
            appEventGateway = appEventGateway
        )

        whenever(megaChatRoom.chatId).thenReturn(chatId)
        whenever(megaChatCall.chatid).thenReturn(chatId)
        whenever(megaChatScheduledMeeting.chatId()).thenReturn(chatId)
        whenever(megaChatCall.callId).thenReturn(callId)
        whenever(megaChatScheduledMeeting.schedId()).thenReturn(schedId)
    }

    @Test
    fun `test that getChatCall invokes the right methods`() = runTest {
        whenever(megaChatApiGateway.getChatCall(any())).thenReturn(megaChatCall)
        whenever(chatCallMapper(megaChatCall)).thenReturn(chatCall)

        val result = underTest.getChatCall(chatId = chatId)

        verify(megaChatApiGateway).getChatCall(chatId = chatId)
        verify(chatCallMapper).invoke(megaChatCall)
        assertThat(result).isEqualTo(chatCall)
    }

    @Test
    fun `test that startCallRinging invokes the right methods`() = runTest {
        whenever(
            megaChatApiGateway.startChatCall(
                any(), any(), any(), any()
            )
        ).thenAnswer {
            ((it.arguments[3]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                megaChatRequest,
                megaChatErrorSuccess,
            )
        }

        whenever(chatRequestMapper(megaChatRequest)).thenReturn(chatRequest)

        val result = underTest.startCallRinging(
            chatId = chatId,
            enabledVideo = video,
            enabledAudio = audio,
        )

        verify(megaChatApiGateway).startChatCall(
            chatId = eq(chatId), enabledVideo = eq(video), enabledAudio = eq(audio), any()
        )
        verify(chatRequestMapper).invoke(megaChatRequest)

        assertThat(result).isEqualTo(chatRequest)
    }

    @Test
    fun `test that startCallNoRinging invokes the right methods`() = runTest {
        whenever(
            megaChatApiGateway.startChatCallNoRinging(
                any(), any(), any(), any(), any()
            )
        ).thenAnswer {
            ((it.arguments[4]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                megaChatRequest,
                megaChatErrorSuccess,
            )
        }

        whenever(chatRequestMapper(megaChatRequest)).thenReturn(chatRequest)

        val result = underTest.startCallNoRinging(
            chatId = chatId,
            schedId = schedId,
            enabledVideo = video,
            enabledAudio = audio,
        )

        verify(megaChatApiGateway).startChatCallNoRinging(
            chatId = eq(chatId),
            schedId = eq(schedId),
            enabledVideo = eq(video),
            enabledAudio = eq(audio),
            any()
        )
        verify(chatRequestMapper).invoke(megaChatRequest)

        assertThat(result).isEqualTo(chatRequest)
    }

    @Test
    fun `test that answerChatCall invokes the right methods`() = runTest {
        whenever(
            megaChatApiGateway.answerChatCall(
                any(), any(), any(), any()
            )
        ).thenAnswer {
            ((it.arguments[3]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                megaChatRequest,
                megaChatErrorSuccess,
            )
        }

        whenever(chatRequestMapper(megaChatRequest)).thenReturn(chatRequest)

        val result = underTest.answerChatCall(
            chatId = chatId,
            enabledVideo = video,
            enabledAudio = audio,
        )

        verify(megaChatApiGateway).answerChatCall(
            chatId = eq(chatId), enabledVideo = eq(video), enabledAudio = eq(audio), any()
        )
        verify(chatRequestMapper).invoke(megaChatRequest)

        assertThat(result).isEqualTo(chatRequest)
    }

    @Test
    fun `test that getAllScheduledMeetings invokes the right methods`() = runTest {
        whenever(megaChatApiGateway.getAllScheduledMeetings()).thenReturn(
            megaChatScheduledMeetingList
        )

        whenever(chatScheduledMeetingMapper(megaChatScheduledMeeting)).thenReturn(
            chatScheduledMeeting
        )

        val result = underTest.getAllScheduledMeetings()

        verify(
            megaChatApiGateway, times(megaChatScheduledMeetingList.size)
        ).getAllScheduledMeetings()
        verify(chatScheduledMeetingMapper, times(megaChatScheduledMeetingList.size)).invoke(
            megaChatScheduledMeeting
        )

        assertThat(result).isEqualTo(chatScheduledMeetingList)
    }

    @Test
    fun `test that getScheduledMeeting invokes the right methods`() = runTest {
        whenever(megaChatApiGateway.getScheduledMeeting(any(), any())).thenReturn(
            megaChatScheduledMeeting
        )

        whenever(chatScheduledMeetingMapper(megaChatScheduledMeeting)).thenReturn(
            chatScheduledMeeting
        )

        val result = underTest.getScheduledMeeting(chatId = chatId, scheduledMeetingId = schedId)

        verify(megaChatApiGateway).getScheduledMeeting(chatId = chatId, schedId = schedId)
        verify(chatScheduledMeetingMapper).invoke(megaChatScheduledMeeting)

        assertThat(result).isEqualTo(chatScheduledMeeting)
    }

    @Test
    fun `test that getScheduledMeetingsByChat invokes the right methods`() = runTest {
        whenever(megaChatApiGateway.getScheduledMeetingsByChat(any())).thenReturn(
            megaChatScheduledMeetingList
        )

        whenever(chatScheduledMeetingMapper(megaChatScheduledMeeting)).thenReturn(
            chatScheduledMeeting
        )

        val result = underTest.getScheduledMeetingsByChat(chatId = chatId)

        verify(
            megaChatApiGateway, times(megaChatScheduledMeetingList.size)
        ).getScheduledMeetingsByChat(chatId = chatId)
        verify(chatScheduledMeetingMapper, times(megaChatScheduledMeetingList.size)).invoke(
            megaChatScheduledMeeting
        )

        assertThat(result).isEqualTo(chatScheduledMeetingList)
    }

    @Test
    fun `test that fetchScheduledMeetingOccurrencesByChat invokes the right methods`() = runTest {
        val expectedResult: List<ChatScheduledMeetingOccurr> = emptyList()
        whenever(
            megaChatApiGateway.fetchScheduledMeetingOccurrencesByChat(
                chatId = any(), since = any(), any()
            )
        ).thenAnswer {
            (it.arguments[2] as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(), megaChatRequestOccursSuccess, megaChatErrorSuccess
            )
        }

        whenever(chatScheduledMeetingOccurrMapper(megaChatScheduledMeetingOccurr)).thenReturn(
            chatScheduledMeetingOccurr
        )

        val result =
            underTest.fetchScheduledMeetingOccurrencesByChat(chatId = chatId, since = since)

        verify(megaChatApiGateway).fetchScheduledMeetingOccurrencesByChat(
            chatId = eq(chatId), since = eq(since), any()
        )

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `test that fetchScheduledMeetingOccurrencesByChat with count invokes the right methods`() =
        runTest {
            val expectedResult: List<ChatScheduledMeetingOccurr> = emptyList()

            whenever(
                megaChatApiGateway.fetchScheduledMeetingOccurrencesByChat(
                    chatId = any(), since = any(), any()
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaChatRequestOccursSuccess,
                    megaChatErrorSuccess,
                )
            }

            whenever(chatScheduledMeetingOccurrMapper(megaChatScheduledMeetingOccurr)).thenReturn(
                chatScheduledMeetingOccurr
            )

            val result = underTest.fetchScheduledMeetingOccurrencesByChat(
                chatId = chatId, count = count
            )

            verify(megaChatApiGateway).fetchScheduledMeetingOccurrencesByChat(
                eq(chatId), eq(lastTimeStamp), any()
            )

            assertThat(result).isEqualTo(expectedResult)
        }

    @Test
    fun `test that getNextScheduledMeetingOccurrence invokes the right methods`() = runTest {
        val since =
            Instant.now().atZone(ZoneOffset.UTC).minus(1L, ChronoUnit.HALF_DAYS).toEpochSecond()
        whenever(
            megaChatApiGateway.fetchScheduledMeetingOccurrencesByChat(
                chatId = any(), since = any(), any()
            )
        ).thenAnswer {
            ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                megaChatRequestOccursSuccess,
                megaChatErrorSuccess,
            )
        }

        underTest.getNextScheduledMeetingOccurrence(chatId = chatId)

        verify(megaChatApiGateway).fetchScheduledMeetingOccurrencesByChat(
            eq(chatId), eq(since), any()
        )
    }

    @Test
    fun `test that chat call update is returned when OnChatCallUpdate is called with non null chat call value`() =
        runTest {
            val chatCallUpdate = ChatCallUpdate.OnChatCallUpdate(megaChatCall)
            whenever(megaChatApiGateway.chatCallUpdates).thenReturn(flowOf(chatCallUpdate))

            whenever(chatCallMapper(megaChatCall)).thenReturn(
                chatCall
            )

            whenever(chatCall.status).thenReturn(
                ChatCallStatus.InProgress
            )

            underTest.monitorChatCallUpdates().test {
                assertThat(awaitItem().status).isInstanceOf(ChatCallStatus::class.java)
                awaitComplete()
            }
        }

    @Test
    fun `test that scheduled meeting update is returned when scheduledMeetingUpdates is called with non null scheduled meeting value`() =
        runTest {
            val scheduledMeetingUpdate =
                ScheduledMeetingUpdate.OnChatSchedMeetingUpdate(megaChatScheduledMeeting)
            whenever(megaChatApiGateway.scheduledMeetingUpdates).thenReturn(
                flowOf(
                    scheduledMeetingUpdate
                )
            )

            whenever(chatScheduledMeetingMapper(megaChatScheduledMeeting)).thenReturn(
                chatScheduledMeeting
            )

            underTest.monitorScheduledMeetingUpdates().test {
                assertThat(awaitItem()).isInstanceOf(ChatScheduledMeeting::class.java)
                awaitComplete()
            }
        }

    @Test
    fun `test that scheduled meeting occurrences update is returned when OnSchedMeetingOccurrencesUpdate is called with non null scheduled meeting value`() =
        runTest {
            val scheduledMeetingOccurrencesUpdate =
                ScheduledMeetingUpdate.OnSchedMeetingOccurrencesUpdate(
                    chatId = chatId,
                    append = true
                )

            whenever(megaChatApiGateway.scheduledMeetingUpdates).thenReturn(
                flowOf(
                    scheduledMeetingOccurrencesUpdate
                )
            )

            underTest.monitorScheduledMeetingOccurrencesUpdates().test {
                assertThat(awaitItem()).isInstanceOf(ResultOccurrenceUpdate::class.java)
                awaitComplete()
            }
        }

    @Test
    fun `test that getCallHandleList returns empty list when state is unknown`() = runTest {
        val actual = underTest.getCallHandleList(ChatCallStatus.Unknown)
        assertThat(actual).isEqualTo(emptyList<Long>())
    }

    @Test
    fun `test that getCallHandleList returns list of long when state is initial`() = runTest {
        whenever(megaChatApiGateway.getChatCalls(megaChatCallStatusMapper(ChatCallStatus.Initial)))
            .thenReturn(mockMegaHandleList)
        val actual = underTest.getCallHandleList(ChatCallStatus.Initial)
        assertThat(actual.size).isEqualTo(2)
    }
}
