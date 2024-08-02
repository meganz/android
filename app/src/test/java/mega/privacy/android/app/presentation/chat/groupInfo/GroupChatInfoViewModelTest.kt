package mega.privacy.android.app.presentation.chat.groupInfo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.SetOpenInviteWithChatIdUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.MonitorSFUServerUpgradeUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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

    private val connectivityFlow = MutableSharedFlow<Boolean>()
    private val updatePushNotificationSettings = MutableSharedFlow<Boolean>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initializeStubbing()
        initializeViewModel()
    }

    private  fun initializeStubbing() {
        whenever(monitorConnectivityUseCase()).thenReturn(connectivityFlow)
        whenever(monitorUpdatePushNotificationSettingsUseCase()).thenReturn(
            updatePushNotificationSettings
        )
        wheneverBlocking { getFeatureFlagValueUseCase.invoke(any()) }.thenReturn(false)
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
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase
        )
    }

    @Test
    fun `test that the end call use case is executed, and the meeting's statistics are sent when the user ends the call for all`() =
        runTest {
            underTest.endCallForAll()

            verify(endCallUseCase).invoke(underTest.state.value.chatId)
            verify(sendStatisticsMeetingsUseCase).invoke(any())
        }

    @Test
    fun `test that a call is started when on call button tapped`() = runTest {
        val chatID = 123L
        whenever(get1On1ChatIdUseCase(any())).thenReturn(chatID)
        whenever(setChatVideoInDeviceUseCase()).thenThrow(RuntimeException())

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
            val newChatId = 456L
            val call = mock<ChatCall>()
            whenever(getChatCallUseCase.invoke(newChatId)).thenReturn(call)
            underTest.setChatId(newChatId)
            underTest.state.test {
                val item = awaitItem()
                assertThat(item.chatId).isEqualTo(newChatId)
                verify(monitorChatCallUpdatesUseCase).invoke()
            }
        }

    @Test
    fun `test that call is not set if there is no existing call when setChatId is invoked`() =
        runTest {
            val newChatId = 456L
            whenever(getChatCallUseCase.invoke(newChatId)).thenReturn(null)
            underTest.setChatId(newChatId)
            underTest.state.test {
                val item = awaitItem()
                assertThat(item.chatId).isEqualTo(newChatId)
                verifyNoInteractions(monitorChatCallUpdatesUseCase)
            }
        }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
    }
}
