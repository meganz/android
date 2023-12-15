package test.mega.privacy.android.app.presentation.settings.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.ChatViewModel
import mega.privacy.android.app.presentation.chat.ContactInvitation
import mega.privacy.android.app.usecase.call.EndCallUseCase
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.LoadPendingMessagesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.MonitorJoinedSuccessfullyUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactLinkUseCase
import mega.privacy.android.domain.usecase.contact.IsContactRequestSentUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartCallUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatViewModelTest {

    private lateinit var underTest: ChatViewModel

    //Mocks
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val passcodeManagement: PasscodeManagement = mock()
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase = mock()
    private val chatManagement: ChatManagement = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase = mock()
    private val startCallUseCase: StartCallUseCase = mock()
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()
    private val getChatRoom: GetChatRoom = mock()
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock()
    private val endCallUseCase: EndCallUseCase = mock()
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase = mock()
    private val getContactLinkUseCase: GetContactLinkUseCase = mock()
    private val deviceGateway: DeviceGateway = mock()
    private val isContactRequestSentUseCase: IsContactRequestSentUseCase = mock()
    private val monitorUpdatePushNotificationSettingsUseCase =
        mock<MonitorUpdatePushNotificationSettingsUseCase>()
    private val monitorChatArchivedUseCase = mock<MonitorChatArchivedUseCase>()
    private val broadcastChatArchivedUseCase = mock<BroadcastChatArchivedUseCase>()
    private val monitorJoinedSuccessfullyUseCase = mock<MonitorJoinedSuccessfullyUseCase>()
    private val monitorLeaveChatUseCase = mock<MonitorLeaveChatUseCase>()
    private val monitorScheduledMeetingUpdatesUseCase =
        mock<MonitorScheduledMeetingUpdatesUseCase>()
    private val monitorChatRoomUpdates = mock<MonitorChatRoomUpdates>()
    private val startMeetingInWaitingRoomChatUseCase = mock<StartMeetingInWaitingRoomChatUseCase>()
    private val leaveChatUseCase = mock<LeaveChatUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val loadPendingMessagesUseCase = mock<LoadPendingMessagesUseCase>()
    private val monitorPausedTransfersUseCase = mock<MonitorPausedTransfersUseCase>()
    private val monitorChatSessionUpdatesUseCase = mock<MonitorChatSessionUpdatesUseCase>()
    private val hangChatCallUseCase = mock<HangChatCallUseCase>()
    private val broadcastCallRecordingConsentEventUseCase =
        mock<BroadcastCallRecordingConsentEventUseCase>()
    private val monitorCallRecordingConsentEventUseCase =
        mock<MonitorCallRecordingConsentEventUseCase>()
    private val monitorCallEndedUseCase = mock<MonitorCallEndedUseCase>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorStorageStateEventUseCase,
            monitorConnectivityUseCase,
            isConnectedToInternetUseCase,
            answerChatCallUseCase,
            passcodeManagement,
            setChatVideoInDeviceUseCase,
            chatManagement,
            rtcAudioManagerGateway,
            startChatCallNoRingingUseCase,
            startCallUseCase,
            getScheduledMeetingByChat,
            getChatCallUseCase,
            getChatRoom,
            monitorChatCallUpdatesUseCase,
            endCallUseCase,
            sendStatisticsMeetingsUseCase,
            getContactLinkUseCase,
            deviceGateway,
            isContactRequestSentUseCase,
            startMeetingInWaitingRoomChatUseCase,
            leaveChatUseCase,
            getFeatureFlagValueUseCase,
            hangChatCallUseCase,
            broadcastCallRecordingConsentEventUseCase,
        )
        wheneverBlocking { monitorPausedTransfersUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorUpdatePushNotificationSettingsUseCase() }.thenReturn(flowOf(true))
        wheneverBlocking { monitorChatArchivedUseCase() }.thenReturn(flowOf("Chat Title"))
        wheneverBlocking { monitorJoinedSuccessfullyUseCase() }.thenReturn(flowOf(true))
        wheneverBlocking { monitorLeaveChatUseCase() }.thenReturn(flowOf(1234L))
        wheneverBlocking { monitorScheduledMeetingUpdatesUseCase() }.thenReturn(flowOf())
        wheneverBlocking { monitorChatRoomUpdates(any()) }.thenReturn(flowOf())
        wheneverBlocking { loadPendingMessagesUseCase(any()) }.thenReturn(flowOf())
        wheneverBlocking { monitorChatSessionUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorCallRecordingConsentEventUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorCallEndedUseCase() }.thenReturn(emptyFlow())
        initTestClass()
    }

    private fun initTestClass() {
        underTest = ChatViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            startCallUseCase = startCallUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            answerChatCallUseCase = answerChatCallUseCase,
            passcodeManagement = passcodeManagement,
            setChatVideoInDeviceUseCase = setChatVideoInDeviceUseCase,
            deviceGateway = deviceGateway,
            chatManagement = chatManagement,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            startChatCallNoRingingUseCase = startChatCallNoRingingUseCase,
            getScheduledMeetingByChat = getScheduledMeetingByChat,
            getChatCallUseCase = getChatCallUseCase,
            getChatRoom = getChatRoom,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            endCallUseCase = endCallUseCase,
            sendStatisticsMeetingsUseCase = sendStatisticsMeetingsUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            monitorChatArchivedUseCase = monitorChatArchivedUseCase,
            broadcastChatArchivedUseCase = broadcastChatArchivedUseCase,
            monitorJoinedSuccessfullyUseCase = monitorJoinedSuccessfullyUseCase,
            monitorLeaveChatUseCase = monitorLeaveChatUseCase,
            leaveChatUseCase = leaveChatUseCase,
            getContactLinkUseCase = getContactLinkUseCase,
            isContactRequestSentUseCase = isContactRequestSentUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            loadPendingMessagesUseCase = loadPendingMessagesUseCase,
            monitorScheduledMeetingUpdates = monitorScheduledMeetingUpdatesUseCase,
            monitorChatRoomUpdates = monitorChatRoomUpdates,
            startMeetingInWaitingRoomChatUseCase = startMeetingInWaitingRoomChatUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            monitorChatSessionUpdatesUseCase = monitorChatSessionUpdatesUseCase,
            hangChatCallUseCase = hangChatCallUseCase,
            broadcastCallRecordingConsentEventUseCase = broadcastCallRecordingConsentEventUseCase,
            monitorCallRecordingConsentEventUseCase = monitorCallRecordingConsentEventUseCase,
            monitorCallEndedUseCase = monitorCallEndedUseCase,
        )
    }

    @Test
    fun `test that when push notification settings is updated state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
            }
        }

    @Test
    fun `test that when onConsumePushNotificationSettingsUpdateEvent is called then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
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
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.titleChatArchivedEvent).isNotNull()
            }
        }

    @Test
    fun `test that when onChatArchivedEventConsumed is called then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
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
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isJoiningOrLeaving).isFalse()
            }
        }

    @Test
    fun `test that the state is updated when leaving a chat`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(leaveChatUseCase).invoke(1234L)
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

    @Test
    fun `test that chatRoomUpdated updates state correctly`() = runTest {
        val isWaitingRoom = true
        val isHost = true

        underTest.chatRoomUpdated(isWaitingRoom, isHost)

        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.isWaitingRoom).isEqualTo(isWaitingRoom)
            Truth.assertThat(state.isHost).isEqualTo(isHost)
        }
    }

    @Test
    fun `test that setOpenWaitingRoomConsumed updates state correctly`() = runTest {
        underTest.setOpenWaitingRoomConsumed()

        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.openWaitingRoomScreen).isFalse()
        }
    }

    @Test
    fun `test that areTransfersPaused emits a new value when monitorPausedTransfersUseCase emits a value`() =
        runTest {
            whenever(monitorPausedTransfersUseCase()).thenReturn(
                flowOf(false, true, false)
            )
            underTest.areTransfersPausedFlow.test {
                Truth.assertThat(awaitItem()).isFalse()
                Truth.assertThat(awaitItem()).isTrue()
                Truth.assertThat(awaitItem()).isFalse()
                this.cancelAndIgnoreRemainingEvents()
            }
        }
}
