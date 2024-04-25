package test.mega.privacy.android.app.presentation.meeting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.presentation.mapper.GetPluralStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingViewModel
import mega.privacy.android.app.presentation.meeting.mapper.RecurrenceDialogOptionMapper
import mega.privacy.android.app.presentation.meeting.mapper.WeekDayMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveChatLink
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.chat.InviteParticipantToChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.RemoveParticipantFromChatUseCase
import mega.privacy.android.domain.usecase.chat.SetOpenInviteUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetContactItem
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.CreateChatroomAndSchedMeetingUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.SetWaitingRoomRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.SetWaitingRoomUseCase
import mega.privacy.android.domain.usecase.meeting.UpdateScheduledMeetingUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateScheduledMeetingViewModelTest {
    private lateinit var underTest: CreateScheduledMeetingViewModel

    private val accountDetailFlow = MutableStateFlow(AccountDetail())
    private val connectivityFlow = MutableStateFlow(false)
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock {
        onBlocking { invoke() }.thenReturn(accountDetailFlow)
    }
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock {
        onBlocking { invoke() }.thenReturn(connectivityFlow)
    }

    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase = mock()
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat = mock()
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase = mock()
    private val getContactItem: GetContactItem = mock()
    private val getChatRoomUseCase: GetChatRoomUseCase = mock()
    private val createChatroomAndSchedMeetingUseCase: CreateChatroomAndSchedMeetingUseCase = mock()
    private val updateScheduledMeetingUseCase: UpdateScheduledMeetingUseCase = mock()
    private val createChatLink: CreateChatLink = mock()
    private val removeChatLink: RemoveChatLink = mock()
    private val queryChatLink: QueryChatLink = mock()
    private val recurrenceDialogOptionMapper: RecurrenceDialogOptionMapper = mock()
    private val weekDayMapper: WeekDayMapper = mock()
    private val deviceGateway: DeviceGateway = mock()
    private val getStringFromStringResMapper: GetStringFromStringResMapper = mock()
    private val getPluralStringFromStringResMapper: GetPluralStringFromStringResMapper = mock()
    private val setOpenInviteUseCase: SetOpenInviteUseCase = mock()
    private val removeParticipantFromChat: RemoveParticipantFromChatUseCase = mock()
    private val inviteParticipantToChat: InviteParticipantToChatUseCase = mock()
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase = mock()
    private val setWaitingRoomUseCase: SetWaitingRoomUseCase = mock()
    private val setWaitingRoomRemindersUseCase: SetWaitingRoomRemindersUseCase = mock()
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase = mock()
    private val getCurrentSubscriptionPlanUseCase: GetCurrentSubscriptionPlanUseCase = mock()

    @BeforeAll
    internal fun initialise() {
        runBlocking {
            stubCommon()
        }
    }

    private suspend fun stubCommon() {
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(getFeatureFlagValue(any())).thenReturn(true)
    }

    @BeforeEach
    fun setup() {
        underTest = CreateScheduledMeetingViewModel(
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            getVisibleContactsUseCase = getVisibleContactsUseCase,
            getScheduledMeetingByChat = getScheduledMeetingByChat,
            getContactFromEmailUseCase = getContactFromEmailUseCase,
            getContactItem = getContactItem,
            getChatRoomUseCase = getChatRoomUseCase,
            createChatroomAndSchedMeetingUseCase = createChatroomAndSchedMeetingUseCase,
            updateScheduledMeetingUseCase = updateScheduledMeetingUseCase,
            createChatLink = createChatLink,
            removeChatLink = removeChatLink,
            queryChatLink = queryChatLink,
            recurrenceDialogOptionMapper = recurrenceDialogOptionMapper,
            weekDayMapper = weekDayMapper,
            deviceGateway = deviceGateway,
            getStringFromStringResMapper = getStringFromStringResMapper,
            getPluralStringFromStringResMapper = getPluralStringFromStringResMapper,
            removeParticipantFromChat = removeParticipantFromChat,
            inviteParticipantToChat = inviteParticipantToChat,
            monitorChatRoomUpdatesUseCase = monitorChatRoomUpdatesUseCase,
            setWaitingRoomUseCase = setWaitingRoomUseCase,
            setWaitingRoomRemindersUseCase = setWaitingRoomRemindersUseCase,
            setOpenInviteUseCase = setOpenInviteUseCase,
        )
    }

    @AfterAll
    internal fun tearDown() {
        reset(
            monitorConnectivityUseCase,
            isConnectedToInternetUseCase,
            getVisibleContactsUseCase,
            getScheduledMeetingByChat,
            getContactFromEmailUseCase,
            getContactItem,
            getChatRoomUseCase,
            createChatroomAndSchedMeetingUseCase,
            updateScheduledMeetingUseCase,
            createChatLink,
            removeChatLink,
            queryChatLink,
            recurrenceDialogOptionMapper,
            weekDayMapper,
            deviceGateway,
            getStringFromStringResMapper,
            getPluralStringFromStringResMapper,
            removeParticipantFromChat,
            inviteParticipantToChat,
            monitorChatRoomUpdatesUseCase,
            setWaitingRoomUseCase,
            setWaitingRoomRemindersUseCase,
            getFeatureFlagValue,
            getCurrentSubscriptionPlanUseCase,
            setOpenInviteUseCase,
            monitorAccountDetailUseCase
        )
    }
}