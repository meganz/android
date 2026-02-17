package mega.privacy.android.app.presentation.chat.list

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomTimestampMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.usecase.chat.GetLastMessageUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.call.OpenOrStartCallUseCase
import mega.privacy.android.domain.usecase.call.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUnreadStatusUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetCurrentChatStatusUseCase
import mega.privacy.android.domain.usecase.chat.GetMeetingTooltipsUseCase
import mega.privacy.android.domain.usecase.chat.HasArchivedChatsUseCase
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.SetNextMeetingTooltipUseCase
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.CancelScheduledMeetingUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingCanceledUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatTabsViewModelTest {
    private lateinit var underTest: ChatTabsViewModel
    private val archiveChatUseCase: ArchiveChatUseCase = mock()
    private val leaveChatUseCase: LeaveChatUseCase = mock()
    private val signalChatPresenceUseCase: SignalChatPresenceActivity = mock()
    private val getChatsUseCase: GetChatsUseCase = mock()
    private val getLastMessageUseCase: GetLastMessageUseCase = mock()
    private val chatRoomTimestampMapper: ChatRoomTimestampMapper = mock()
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase = mock()
    private val openOrStartCallUseCase: OpenOrStartCallUseCase = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val chatManagement: ChatManagement = mock()
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val getCurrentChatStatusUseCase: GetCurrentChatStatusUseCase = mock()
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase = mock()
    private val loadMessagesUseCase: LoadMessagesUseCase = mock()
    private val cancelScheduledMeetingUseCase: CancelScheduledMeetingUseCase = mock()
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase = mock()
    private val getMeetingTooltipsUseCase: GetMeetingTooltipsUseCase = mock()
    private val setNextMeetingTooltipUseCase: SetNextMeetingTooltipUseCase = mock()
    private val getStringFromStringResMapper: GetStringFromStringResMapper = mock()
    private val monitorScheduledMeetingCanceledUseCase: MonitorScheduledMeetingCanceledUseCase =
        mock()
    private val getChatsUnreadStatusUseCase: GetChatsUnreadStatusUseCase = mock()
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase = mock()
    private val monitorLeaveChatUseCase: MonitorLeaveChatUseCase = mock()
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock()
    private val hasArchivedChatsUseCase: HasArchivedChatsUseCase = mock()
    private val monitorHasAnyContactUseCase: MonitorHasAnyContactUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val monitorChatArchivedUseCase = mock<MonitorChatArchivedUseCase> {
        onBlocking { invoke() }.thenReturn(flowOf("Chat Title"))
    }

    @BeforeEach
    fun resetMocks() {
        whenever(monitorScheduledMeetingCanceledUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { getMeetingTooltipsUseCase() }.thenReturn(MeetingTooltipItem.NONE)
        whenever(monitorHasAnyContactUseCase()).thenReturn(flowOf(true))
        reset(
            archiveChatUseCase,
            leaveChatUseCase,
            signalChatPresenceUseCase,
            getChatsUseCase,
            getLastMessageUseCase,
            chatRoomTimestampMapper,
            startChatCallNoRingingUseCase,
            openOrStartCallUseCase,
            answerChatCallUseCase,
            chatManagement,
            megaChatApiGateway,
            rtcAudioManagerGateway,
            getCurrentChatStatusUseCase,
            clearChatHistoryUseCase,
            loadMessagesUseCase,
            cancelScheduledMeetingUseCase,
            isParticipatingInChatCallUseCase,
            setNextMeetingTooltipUseCase,
            getChatsUnreadStatusUseCase,
            startMeetingInWaitingRoomChatUseCase,
            monitorChatCallUpdatesUseCase,
            getStringFromStringResMapper,
            getFeatureFlagValueUseCase,
            monitorChatArchivedUseCase,
        )
        wheneverBlocking { getFeatureFlagValueUseCase(AppFeatures.SingleActivity) }.thenReturn(false)
        whenever(monitorChatArchivedUseCase()).thenReturn(flowOf("Chat Title"))
    }

    private fun initTestClass() {
        underTest = ChatTabsViewModel(
            archiveChatUseCase = archiveChatUseCase,
            leaveChatUseCase = leaveChatUseCase,
            signalChatPresenceUseCase = signalChatPresenceUseCase,
            getChatsUseCase = getChatsUseCase,
            getLastMessageUseCase = getLastMessageUseCase,
            chatRoomTimestampMapper = chatRoomTimestampMapper,
            startChatCallNoRingingUseCase = startChatCallNoRingingUseCase,
            openOrStartCallUseCase = openOrStartCallUseCase,
            answerChatCallUseCase = answerChatCallUseCase,
            chatManagement = chatManagement,
            megaChatApiGateway = megaChatApiGateway,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            getCurrentChatStatusUseCase = getCurrentChatStatusUseCase,
            clearChatHistoryUseCase = clearChatHistoryUseCase,
            isParticipatingInChatCallUseCase = isParticipatingInChatCallUseCase,
            getMeetingTooltipsUseCase = getMeetingTooltipsUseCase,
            setNextMeetingTooltipUseCase = setNextMeetingTooltipUseCase,
            monitorScheduledMeetingCanceledUseCase = monitorScheduledMeetingCanceledUseCase,
            getChatsUnreadStatusUseCase = getChatsUnreadStatusUseCase,
            startMeetingInWaitingRoomChatUseCase = startMeetingInWaitingRoomChatUseCase,
            monitorLeaveChatUseCase = monitorLeaveChatUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            hasArchivedChatsUseCase = hasArchivedChatsUseCase,
            monitorHasAnyContactUseCase = monitorHasAnyContactUseCase,
            getStringFromStringResMapper = getStringFromStringResMapper,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorChatArchivedUseCase = monitorChatArchivedUseCase,
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isParticipatingInChatCallResult updated when calling checkParticipatingInChatCall`(
        isInCall: Boolean,
    ) = runTest {
        initTestClass()
        whenever(isParticipatingInChatCallUseCase()).thenReturn(isInCall)
        underTest.getState().test {
            val state = awaitItem()
            Truth.assertThat(state.isParticipatingInChatCallResult).isNull()
            underTest.checkParticipatingInChatCall()
            val updatedState = awaitItem()
            Truth.assertThat(updatedState.isParticipatingInChatCallResult).isEqualTo(isInCall)
        }
    }

    @Test
    fun `test that isParticipatingInChatCallResult updated when calling markHandleIsParticipatingInChatCall`() =
        runTest {
            initTestClass()
            whenever(isParticipatingInChatCallUseCase()).thenReturn(true)
            underTest.getState().test {
                val state = awaitItem()
                Truth.assertThat(state.isParticipatingInChatCallResult).isNull()
                underTest.checkParticipatingInChatCall()
                Truth.assertThat(awaitItem().isParticipatingInChatCallResult).isTrue()
                underTest.markHandleIsParticipatingInChatCall()
                Truth.assertThat(awaitItem().isParticipatingInChatCallResult).isNull()
            }
        }

    @Test
    fun `test that a chat is removed when event is collected in monitorLeaveChat`() =
        runTest {
            val chatId = 1L
            val flow = MutableSharedFlow<Long>()
            whenever(monitorLeaveChatUseCase()).thenReturn(flow)
            initTestClass()
            flow.emit(chatId)
            verify(chatManagement).addLeavingChatId(chatId)
            verify(leaveChatUseCase).invoke(chatId)
        }

    @Test
    fun `test that hasArchivedChats updated when calling checkHasArchivedChats`() = runTest {
        initTestClass()
        whenever(hasArchivedChatsUseCase()).thenReturn(true)
        underTest.getState().test {
            val state = awaitItem()
            Truth.assertThat(state.hasArchivedChats).isFalse()
            underTest.checkHasArchivedChats()
            val updatedState = awaitItem()
            Truth.assertThat(updatedState.hasArchivedChats).isTrue()
        }
    }

    @Test
    fun `test that when a chat is archived state is updated`() =
        runTest {
            initTestClass()
            testScheduler.advanceUntilIdle()
            verify(monitorChatArchivedUseCase).invoke()
            underTest.getState().test {
                val state = awaitItem()
                assertThat(state.titleChatArchivedEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            }
        }

    @Test
    fun `test that when onChatArchivedEventConsumed is called then state is also updated`() =
        runTest {
            initTestClass()
            testScheduler.advanceUntilIdle()
            verify(monitorChatArchivedUseCase).invoke()
            underTest.getState().test {
                val state = awaitItem()
                assertThat(state.titleChatArchivedEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                underTest.onTitleChatArchivedEventConsumed()
                val updatedState = awaitItem()
                assertThat(updatedState.titleChatArchivedEvent).isEqualTo(consumed())
            }
        }
}
