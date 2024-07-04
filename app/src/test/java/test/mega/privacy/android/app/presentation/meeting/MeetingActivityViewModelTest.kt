package test.mega.privacy.android.app.presentation.meeting

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivityRepository
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.presentation.mapper.GetPluralStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.mapper.ChatParticipantMapper
import mega.privacy.android.app.usecase.call.GetCallUseCase
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.chat.UpdateChatPermissionsUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.login.MonitorFinishActivityUseCase
import mega.privacy.android.domain.usecase.meeting.AllowUsersJoinCallUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableAudioUseCase
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableVideoUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MuteAllPeersUseCase
import mega.privacy.android.domain.usecase.meeting.MutePeersUseCase
import mega.privacy.android.domain.usecase.meeting.RingIndividualInACallUseCase
import mega.privacy.android.domain.usecase.meeting.StartVideoDeviceUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension

@ExperimentalCoroutinesApi
@ExtendWith(value = [CoroutineMainDispatcherExtension::class, InstantTaskExecutorExtension::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MeetingActivityViewModelTest {

    private lateinit var underTest: MeetingActivityViewModel

    private val scheduler = Schedulers.trampoline()
    private val meetingActivityRepository: MeetingActivityRepository = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val getCallUseCase: GetCallUseCase = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val chatManagement: ChatManagement = mock()
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase = mock()
    private val checkChatLink: CheckChatLinkUseCase = mock()
    private val getChatParticipants: GetChatParticipants = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val logoutUseCase: LogoutUseCase = mock()
    private val monitorFinishActivityUseCase: MonitorFinishActivityUseCase = mock()
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock()
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase = mock()
    private val getChatRoomUseCase: GetChatRoomUseCase = mock()
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase = mock()
    private val queryChatLink: QueryChatLink = mock()
    private val setOpenInvite: SetOpenInvite = mock()
    private val chatParticipantMapper: ChatParticipantMapper = mock()
    private val isEphemeralPlusPlusUseCase: IsEphemeralPlusPlusUseCase = mock()
    private val createChatLink: CreateChatLink = mock()
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase = mock()
    private val updateChatPermissionsUseCase: UpdateChatPermissionsUseCase = mock()
    private val removeFromChaUseCase: RemoveFromChat = mock()
    private val startConversationUseCase: StartConversationUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val hangChatCallUseCase: HangChatCallUseCase = mock()
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase = mock()
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat = mock()
    private val getMyFullNameUseCase: GetMyFullNameUseCase = mock()
    private val monitorUserUpdates: MonitorUserUpdates = mock()
    private val monitorScheduledMeetingUpdatesUseCase: MonitorScheduledMeetingUpdatesUseCase =
        mock()
    private val deviceGateway: DeviceGateway = mock()
    private val ringIndividualInACallUseCase: RingIndividualInACallUseCase = mock()
    private val allowUsersJoinCallUseCase: AllowUsersJoinCallUseCase = mock()
    private val mutePeersUseCase: MutePeersUseCase = mock()
    private val muteAllPeersUseCase: MuteAllPeersUseCase = mock()
    private val getStringFromStringResMapper: GetStringFromStringResMapper = mock()
    private val getPluralStringFromStringResMapper: GetPluralStringFromStringResMapper = mock()
    private val getCurrentSubscriptionPlanUseCase: GetCurrentSubscriptionPlanUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()
    private val startVideoDeviceUseCase: StartVideoDeviceUseCase = mock()
    private val monitorCallEndedUseCase: MonitorCallEndedUseCase = mock()
    private val enableOrDisableVideoUseCase: EnableOrDisableVideoUseCase = mock()
    private val enableOrDisableAudioUseCase: EnableOrDisableAudioUseCase = mock()

    private val context: Context = mock()

    private val chatId = 123L

    @BeforeAll
    fun initialise() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { scheduler }
    }

    @BeforeEach
    fun setUp() {
        reset(
            meetingActivityRepository,
            answerChatCallUseCase,
            getCallUseCase,
            getChatCallUseCase,
            rtcAudioManagerGateway,
            chatManagement,
            setChatVideoInDeviceUseCase,
            checkChatLink,
            context,
            getChatParticipants,
            monitorConnectivityUseCase,
            logoutUseCase,
            monitorFinishActivityUseCase,
            monitorChatCallUpdatesUseCase,
            monitorChatSessionUpdatesUseCase,
            getChatRoomUseCase,
            monitorChatRoomUpdatesUseCase,
            queryChatLink,
            setOpenInvite,
            chatParticipantMapper,
            isEphemeralPlusPlusUseCase,
            createChatLink,
            inviteContactWithHandleUseCase,
            updateChatPermissionsUseCase,
            removeFromChaUseCase,
            startConversationUseCase,
            isConnectedToInternetUseCase,
            monitorStorageStateEventUseCase,
            hangChatCallUseCase,
            broadcastCallEndedUseCase,
            getScheduledMeetingByChat,
            getMyFullNameUseCase,
            monitorUserUpdates,
            monitorScheduledMeetingUpdatesUseCase,
            deviceGateway,
            ringIndividualInACallUseCase,
            allowUsersJoinCallUseCase,
            mutePeersUseCase,
            muteAllPeersUseCase,
            getStringFromStringResMapper,
            getPluralStringFromStringResMapper,
            getCurrentSubscriptionPlanUseCase,
            getFeatureFlagValueUseCase,
            getMyUserHandleUseCase,
            startVideoDeviceUseCase,
            monitorCallEndedUseCase,
            enableOrDisableAudioUseCase,
            enableOrDisableVideoUseCase,
            savedStateHandle
        )
    }

    private fun initUnderTest() {
        stubCommon()
        underTest = MeetingActivityViewModel(
            meetingActivityRepository = meetingActivityRepository,
            answerChatCallUseCase = answerChatCallUseCase,
            getCallUseCase = getCallUseCase,
            getChatCallUseCase = getChatCallUseCase,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            chatManagement = chatManagement,
            setChatVideoInDeviceUseCase = setChatVideoInDeviceUseCase,
            checkChatLink = checkChatLink,
            context = context,
            getChatParticipants = getChatParticipants,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            logoutUseCase = logoutUseCase,
            monitorFinishActivityUseCase = monitorFinishActivityUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            monitorChatSessionUpdatesUseCase = monitorChatSessionUpdatesUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            monitorChatRoomUpdatesUseCase = monitorChatRoomUpdatesUseCase,
            queryChatLink = queryChatLink,
            setOpenInvite = setOpenInvite,
            chatParticipantMapper = chatParticipantMapper,
            isEphemeralPlusPlusUseCase = isEphemeralPlusPlusUseCase,
            createChatLink = createChatLink,
            inviteContactWithHandleUseCase = inviteContactWithHandleUseCase,
            updateChatPermissionsUseCase = updateChatPermissionsUseCase,
            removeFromChaUseCase = removeFromChaUseCase,
            startConversationUseCase = startConversationUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            hangChatCallUseCase = hangChatCallUseCase,
            broadcastCallEndedUseCase = broadcastCallEndedUseCase,
            getScheduledMeetingByChat = getScheduledMeetingByChat,
            getMyFullNameUseCase = getMyFullNameUseCase,
            monitorUserUpdates = monitorUserUpdates,
            monitorScheduledMeetingUpdatesUseCase = monitorScheduledMeetingUpdatesUseCase,
            deviceGateway = deviceGateway,
            ringIndividualInACallUseCase = ringIndividualInACallUseCase,
            allowUsersJoinCallUseCase = allowUsersJoinCallUseCase,
            mutePeersUseCase = mutePeersUseCase,
            muteAllPeersUseCase = muteAllPeersUseCase,
            getStringFromStringResMapper = getStringFromStringResMapper,
            getPluralStringFromStringResMapper = getPluralStringFromStringResMapper,
            getCurrentSubscriptionPlanUseCase = getCurrentSubscriptionPlanUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
            savedStateHandle = savedStateHandle,
            enableOrDisableAudioUseCase = enableOrDisableAudioUseCase,
            enableOrDisableVideoUseCase = enableOrDisableVideoUseCase,
            startVideoDeviceUseCase = startVideoDeviceUseCase,
            monitorCallEndedUseCase = monitorCallEndedUseCase
        )
    }

    private fun stubCommon() {
        whenever(savedStateHandle.get<Long>(MeetingActivity.MEETING_CHAT_ID)).thenReturn(chatId)
    }
}
