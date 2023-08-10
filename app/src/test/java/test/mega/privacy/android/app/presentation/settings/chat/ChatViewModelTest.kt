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
import mega.privacy.android.app.presentation.chat.ContactInvitation
import mega.privacy.android.app.usecase.call.EndCallUseCase
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.LoadPendingMessagesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.MonitorJoinedSuccessfullyUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactLinkUseCase
import mega.privacy.android.domain.usecase.contact.IsContactRequestSentUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCall
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ChatViewModelTest {

    private lateinit var underTest: ChatViewModel

    //Mocks
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val startChatCall: StartChatCall = mock()
    private val chatApiGateway: MegaChatApiGateway = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val passcodeManagement: PasscodeManagement = mock()
    private val cameraGateway: CameraGateway = mock()
    private val chatManagement: ChatManagement = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase = mock()
    private val openOrStartCall: OpenOrStartCall = mock()
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat = mock()
    private val getChatCall: GetChatCall = mock()
    private val monitorChatCallUpdates: MonitorChatCallUpdates = mock()
    private val endCallUseCase: EndCallUseCase = mock()
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase = mock()
    private val getContactLinkUseCase: GetContactLinkUseCase = mock()
    private val deviceGateway: DeviceGateway = mock()
    private val isContactRequestSentUseCase: IsContactRequestSentUseCase = mock()
    private val monitorUpdatePushNotificationSettingsUseCase =
        mock<MonitorUpdatePushNotificationSettingsUseCase> {
            onBlocking { invoke() }.thenReturn(flowOf(true))
        }
    private val monitorChatArchivedUseCase = mock<MonitorChatArchivedUseCase> {
        onBlocking { invoke() }.thenReturn(flowOf("Chat Title"))
    }
    private val broadcastChatArchivedUseCase = mock<BroadcastChatArchivedUseCase>()
    private val monitorJoinedSuccessfullyUseCase = mock<MonitorJoinedSuccessfullyUseCase> {
        onBlocking { invoke() }.thenReturn(flowOf(true))
    }
    private val monitorLeaveChatUseCase = mock<MonitorLeaveChatUseCase> {
        onBlocking { invoke() }.thenReturn(flowOf(1234L))
    }
    private val monitorScheduledMeetingUpdates = mock<MonitorScheduledMeetingUpdates> {
        onBlocking { invoke() }.thenReturn(flowOf())
    }
    private val leaveChat = mock<LeaveChat>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val loadPendingMessagesUseCase = mock<LoadPendingMessagesUseCase> {
        onBlocking { invoke(any()) }.thenReturn(flowOf())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = ChatViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            startChatCall = startChatCall,
            chatApiGateway = chatApiGateway,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            answerChatCallUseCase = answerChatCallUseCase,
            passcodeManagement = passcodeManagement,
            cameraGateway = cameraGateway,
            deviceGateway = deviceGateway,
            chatManagement = chatManagement,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            startChatCallNoRingingUseCase = startChatCallNoRingingUseCase,
            openOrStartCall = openOrStartCall,
            megaChatApiGateway = megaChatApiGateway,
            getScheduledMeetingByChat = getScheduledMeetingByChat,
            getChatCall = getChatCall,
            monitorChatCallUpdates = monitorChatCallUpdates,
            endCallUseCase = endCallUseCase,
            sendStatisticsMeetingsUseCase = sendStatisticsMeetingsUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            monitorChatArchivedUseCase = monitorChatArchivedUseCase,
            broadcastChatArchivedUseCase = broadcastChatArchivedUseCase,
            monitorJoinedSuccessfullyUseCase = monitorJoinedSuccessfullyUseCase,
            monitorLeaveChatUseCase = monitorLeaveChatUseCase,
            leaveChat = leaveChat,
            getContactLinkUseCase = getContactLinkUseCase,
            isContactRequestSentUseCase = isContactRequestSentUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            loadPendingMessagesUseCase = loadPendingMessagesUseCase,
            monitorScheduledMeetingUpdates = monitorScheduledMeetingUpdates
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

    @Test
    fun `test that when a chat is archived state is updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorChatArchivedUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.titleChatArchivedEvent).isNotNull()
            }
        }

    @Test
    fun `test that when onChatArchivedEventConsumed is called then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorChatArchivedUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.titleChatArchivedEvent).isNotNull()
                underTest.onChatArchivedEventConsumed()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.titleChatArchivedEvent).isNull()
            }
        }

    @Test
    fun `test that when joined successfully to a chat then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorJoinedSuccessfullyUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isJoiningOrLeaving).isFalse()
            }
        }

    @Test
    fun `test that the state is updated when leaving a chat`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(leaveChat).invoke(1234L)
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isJoiningOrLeaving).isFalse()
                Truth.assertThat(state.joiningOrLeavingAction).isNull()
            }
        }

    @Test
    fun `test that callback invoke when call getContactLinkByHandle success`() = runTest {
        val contactLink = mock<ContactLink>()
        val userHandler = 1L
        whenever(getContactLinkUseCase(userHandler)).thenReturn(contactLink)
        underTest.getContactLinkByHandle(userHandler) {
            Truth.assertThat(it).isEqualTo(contactLink)
        }
    }

    @Test
    fun `test that contactInvitation updates correctly when call isContactRequestSent returns true`() =
        runTest {
            val myEmail = "myEmail"
            val myName = "myName"
            whenever((isContactRequestSentUseCase(myEmail))).thenReturn(true)
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                Truth.assertThat(awaitItem().contactInvitation).isNull()
                underTest.checkContactRequestSent(email = myEmail, name = myName)
                Truth.assertThat(awaitItem().contactInvitation)
                    .isEqualTo(ContactInvitation(email = myEmail, name = myName, isSent = true))
            }
        }

    @Test
    fun `test that contactInvitation updates correctly when call isContactRequestSent returns false`() =
        runTest {
            val myEmail = "myEmail"
            val myName = "myName"
            whenever((isContactRequestSentUseCase(myEmail))).thenReturn(false)
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                Truth.assertThat(awaitItem().contactInvitation).isNull()
                underTest.checkContactRequestSent(email = myEmail, name = myName)
                Truth.assertThat(awaitItem().contactInvitation)
                    .isEqualTo(ContactInvitation(email = myEmail, name = myName, isSent = false))
            }
        }
}