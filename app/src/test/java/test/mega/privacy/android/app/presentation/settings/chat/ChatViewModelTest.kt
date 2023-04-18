package test.mega.privacy.android.app.presentation.settings.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.ChatViewModel
import mega.privacy.android.app.usecase.call.EndCallUseCase
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCall
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCall
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRinging
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ChatViewModelTest {

    private lateinit var underTest: ChatViewModel

    //Mocks
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val startChatCall: StartChatCall = mock()
    private val chatApiGateway: MegaChatApiGateway = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val answerChatCall: AnswerChatCall = mock()
    private val passcodeManagement: PasscodeManagement = mock()
    private val cameraGateway: CameraGateway = mock()
    private val chatManagement: ChatManagement = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val startChatCallNoRinging: StartChatCallNoRinging = mock()
    private val openOrStartCall: OpenOrStartCall = mock()
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat = mock()
    private val getChatCall: GetChatCall = mock()
    private val monitorChatCallUpdates: MonitorChatCallUpdates = mock()
    private val endCallUseCase: EndCallUseCase = mock()
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase = mock()
    private val monitorUpdatePushNotificationSettingsUseCase =
        mock<MonitorUpdatePushNotificationSettingsUseCase> {
            onBlocking { invoke() }.thenReturn(flowOf(true))
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = ChatViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            startChatCall = startChatCall,
            chatApiGateway = chatApiGateway,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            answerChatCall = answerChatCall,
            passcodeManagement = passcodeManagement,
            cameraGateway = cameraGateway,
            chatManagement = chatManagement,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            startChatCallNoRinging = startChatCallNoRinging,
            openOrStartCall = openOrStartCall,
            megaChatApiGateway = megaChatApiGateway,
            getScheduledMeetingByChat = getScheduledMeetingByChat,
            getChatCall = getChatCall,
            monitorChatCallUpdates = monitorChatCallUpdates,
            endCallUseCase = endCallUseCase,
            sendStatisticsMeetingsUseCase = sendStatisticsMeetingsUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @Test
    fun `test that when push notification settings is updated state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorUpdatePushNotificationSettingsUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
            }
        }

    @Test
    fun `test that when onConsumePushNotificationSettingsUpdateEvent is called then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorUpdatePushNotificationSettingsUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
                underTest.onConsumePushNotificationSettingsUpdateEvent()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.isPushNotificationSettingsUpdatedEvent).isFalse()
            }
        }
}