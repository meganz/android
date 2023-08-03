package test.mega.privacy.android.app.presentation.chat.list

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.list.ChatTabsViewModel
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomTimestampMapper
import mega.privacy.android.app.usecase.chat.GetLastMessageUseCase
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetCurrentChatStatusUseCase
import mega.privacy.android.domain.usecase.chat.GetMeetingTooltipsUseCase
import mega.privacy.android.domain.usecase.chat.SetNextMeetingTooltipUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.CancelScheduledMeetingUseCase
import mega.privacy.android.domain.usecase.meeting.IsChatHistoryEmptyUseCase
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorSingleOccurrenceScheduledMeetingCancelledUseCase
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatTabsViewModelTest {
    private lateinit var underTest: ChatTabsViewModel
    private val archiveChatUseCase: ArchiveChatUseCase = mock()
    private val leaveChatUseCase: LeaveChat = mock()
    private val signalChatPresenceUseCase: SignalChatPresenceActivity = mock()
    private val getChatsUseCase: GetChatsUseCase = mock()
    private val getLastMessageUseCase: GetLastMessageUseCase = mock()
    private val chatRoomTimestampMapper: ChatRoomTimestampMapper = mock()
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase = mock()
    private val openOrStartCall: OpenOrStartCall = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val chatManagement: ChatManagement = mock()
    private val passcodeManagement: PasscodeManagement = mock()
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val getCurrentChatStatusUseCase: GetCurrentChatStatusUseCase = mock()
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase = mock()
    private val isChatHistoryEmptyUseCase: IsChatHistoryEmptyUseCase = mock()
    private val loadMessagesUseCase: LoadMessagesUseCase = mock()
    private val cancelScheduledMeetingUseCase: CancelScheduledMeetingUseCase = mock()
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase = mock()
    private val getMeetingTooltipsUseCase: GetMeetingTooltipsUseCase = mock()
    private val setNextMeetingTooltipUseCase: SetNextMeetingTooltipUseCase = mock()
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase = mock()
    private val monitorSingleOccurrenceScheduledMeetingCancelledUseCase: MonitorSingleOccurrenceScheduledMeetingCancelledUseCase =
        mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            archiveChatUseCase,
            leaveChatUseCase,
            signalChatPresenceUseCase,
            getChatsUseCase,
            getLastMessageUseCase,
            chatRoomTimestampMapper,
            startChatCallNoRingingUseCase,
            openOrStartCall,
            answerChatCallUseCase,
            chatManagement,
            passcodeManagement,
            megaChatApiGateway,
            rtcAudioManagerGateway,
            getCurrentChatStatusUseCase,
            clearChatHistoryUseCase,
            isChatHistoryEmptyUseCase,
            loadMessagesUseCase,
            cancelScheduledMeetingUseCase,
            isParticipatingInChatCallUseCase,
            getMeetingTooltipsUseCase,
            setNextMeetingTooltipUseCase,
            getFeatureFlagValue,
        )
    }

    private fun initTestClass() {
        underTest = ChatTabsViewModel(
            archiveChatUseCase,
            leaveChatUseCase,
            signalChatPresenceUseCase,
            getChatsUseCase,
            getLastMessageUseCase,
            chatRoomTimestampMapper,
            startChatCallNoRingingUseCase,
            openOrStartCall,
            answerChatCallUseCase,
            chatManagement,
            passcodeManagement,
            megaChatApiGateway,
            rtcAudioManagerGateway,
            getCurrentChatStatusUseCase,
            clearChatHistoryUseCase,
            isParticipatingInChatCallUseCase,
            getMeetingTooltipsUseCase,
            setNextMeetingTooltipUseCase,
            getFeatureFlagValue,
            monitorSingleOccurrenceScheduledMeetingCancelledUseCase,
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
}