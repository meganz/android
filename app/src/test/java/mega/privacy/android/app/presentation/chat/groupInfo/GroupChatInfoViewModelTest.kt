package mega.privacy.android.app.presentation.chat.groupInfo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.usecase.SetOpenInviteWithChatIdUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.MonitorSFUServerUpgradeUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GroupChatInfoViewModelTest {

    private lateinit var underTest: GroupChatInfoViewModel

    private val setOpenInviteWithChatIdUseCase: SetOpenInviteWithChatIdUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val startCallUseCase: StartCallUseCase = mock()
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase = mock()
    private val passcodeManagement: PasscodeManagement = mock()
    private val chatApiGateway: MegaChatApiGateway = mock()
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase = mock()
    private val chatManagement: ChatManagement = mock()
    private val endCallUseCase: EndCallUseCase = mock()
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase = mock()
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase =
        mock()
    private val broadcastChatArchivedUseCase: BroadcastChatArchivedUseCase = mock()
    private val broadcastLeaveChatUseCase: BroadcastLeaveChatUseCase = mock()
    private val monitorSFUServerUpgradeUseCase: MonitorSFUServerUpgradeUseCase = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val monitorChatRoomUpdatesUseCase = mock<MonitorChatRoomUpdatesUseCase>()

    private val connectivityFlow = MutableSharedFlow<Boolean>()
    private val updatePushNotificationSettings = MutableSharedFlow<Boolean>()
    private val chatCallUpdates = MutableSharedFlow<ChatCall>()
    private val chatRoomUpdates = MutableSharedFlow<ChatRoom>()

    private val chatId = 123L

    @BeforeEach
    fun resetMocks() {
        reset(
            setOpenInviteWithChatIdUseCase,
            monitorConnectivityUseCase,
            startCallUseCase,
            get1On1ChatIdUseCase,
            passcodeManagement,
            chatApiGateway,
            setChatVideoInDeviceUseCase,
            chatManagement,
            endCallUseCase,
            sendStatisticsMeetingsUseCase,
            monitorUpdatePushNotificationSettingsUseCase,
            broadcastChatArchivedUseCase,
            broadcastLeaveChatUseCase,
            getChatCallUseCase,
            monitorChatCallUpdatesUseCase,
        )
        initializeStubbing()
    }

    private fun initializeStubbing() {
        whenever(monitorConnectivityUseCase()) doReturn connectivityFlow
        whenever(monitorUpdatePushNotificationSettingsUseCase()) doReturn updatePushNotificationSettings
        wheneverBlocking { monitorSFUServerUpgradeUseCase() } doReturn emptyFlow()
        wheneverBlocking { getFeatureFlagValueUseCase.invoke(any()) } doReturn false
        wheneverBlocking { monitorChatRoomUpdatesUseCase(chatId) } doReturn chatRoomUpdates
        wheneverBlocking { monitorChatCallUpdatesUseCase() } doReturn chatCallUpdates
    }

    private fun initializeViewModel() {
        underTest = GroupChatInfoViewModel(
            setOpenInviteWithChatIdUseCase = setOpenInviteWithChatIdUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            startCallUseCase = startCallUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            passcodeManagement = passcodeManagement,
            chatApiGateway = chatApiGateway,
            setChatVideoInDeviceUseCase = setChatVideoInDeviceUseCase,
            chatManagement = chatManagement,
            endCallUseCase = endCallUseCase,
            sendStatisticsMeetingsUseCase = sendStatisticsMeetingsUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            broadcastChatArchivedUseCase = broadcastChatArchivedUseCase,
            broadcastLeaveChatUseCase = broadcastLeaveChatUseCase,
            monitorSFUServerUpgradeUseCase = monitorSFUServerUpgradeUseCase,
            getChatCallUseCase = getChatCallUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorChatRoomUpdatesUseCase = monitorChatRoomUpdatesUseCase,
        )
    }

    @Test
    fun `test that the end call use case is executed, and the meeting's statistics are sent when the user ends the call for all`() =
        runTest {
            initializeViewModel()
            underTest.endCallForAll()

            verify(endCallUseCase).invoke(underTest.state.value.chatId)
            verify(sendStatisticsMeetingsUseCase).invoke(any())
        }

    @Test
    fun `test that a call is started when on call button tapped`() = runTest {
        val chatID = 123L

        whenever(get1On1ChatIdUseCase(any())).thenReturn(chatID)
        whenever(setChatVideoInDeviceUseCase()).thenThrow(RuntimeException())

        initializeViewModel()
        underTest.onCallTap(
            userHandle = 456L,
            video = false,
            audio = true
        )

        verify(chatApiGateway).getChatCall(chatID)
    }

    @Test
    fun `test that new chatId and call are set when setChatId is invoked`() =
        runTest {
            val call = mock<ChatCall> {
                on { callId } doReturn 456L
            }

            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(call)

            initializeViewModel()
            underTest.setChatId(chatId)

            underTest.state.test {
                val item = awaitItem()
                assertThat(item.chatId).isEqualTo(chatId)
                verify(monitorChatCallUpdatesUseCase).invoke()
            }
        }

    @Test
    fun `test that call is not set if there is no existing call when setChatId is invoked`() =
        runTest {
            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(null)

            initializeViewModel()
            underTest.setChatId(chatId)

            underTest.state.test {
                val item = awaitItem()
                assertThat(item.chatId).isEqualTo(chatId)
                verifyNoInteractions(monitorChatCallUpdatesUseCase)
            }
        }

    @Test
    fun `test that open invite updates update state`() = runTest {
        val chat1 = mock<ChatRoom> {
            on { chatId } doReturn chatId
            on { isOpenInvite } doReturn true
            on { hasChanged(ChatRoomChange.OpenInvite) } doReturn true
        }
        val chat2 = mock<ChatRoom> {
            on { chatId } doReturn chatId
            on { isOpenInvite } doReturn false
            on { hasChanged(ChatRoomChange.OpenInvite) } doReturn true
        }


        initializeViewModel()
        underTest.setChatId(chatId)
        chatRoomUpdates.emit(chat1)

        underTest.state.map { it.resultSetOpenInvite }.test {
            assertThat(awaitItem()).isTrue()
            chatRoomUpdates.emit(chat2)
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that retention time updates update state correctly`() = runTest {
        val retentionTime = 5693L
        val chat1 = mock<ChatRoom> {
            on { chatId } doReturn chatId
            on { this.retentionTime } doReturn retentionTime
            on { hasChanged(ChatRoomChange.RetentionTime) } doReturn true
        }

        initializeViewModel()
        underTest.setChatId(chatId)
        chatRoomUpdates.emit(chat1)

        underTest.state.map { it.retentionTime }.test {
            assertThat(awaitItem()).isEqualTo(retentionTime)
        }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
