package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.meeting.CallPushMessageNotificationActionType
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorCallPushNotificationUseCaseTest {

    private val callRepository = mock<CallRepository>()
    private val contactsRepository = mock<ContactsRepository>()
    private val setFakeIncomingCallStateUseCase = mock<SetFakeIncomingCallStateUseCase>()
    private val setPendingToHangUpCallUseCase = mock<SetPendingToHangUpCallUseCase>()
    private val defaultDispatcher = UnconfinedTestDispatcher()

    private lateinit var underTest: MonitorCallPushNotificationUseCase
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }
    private val isChatStatusConnectedForCallUseCase: IsChatStatusConnectedForCallUseCase = mock()

    val chatId = 123L
    val callId = 456L

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        underTest = MonitorCallPushNotificationUseCase(
            callRepository = callRepository,
            setFakeIncomingCallUseCase = setFakeIncomingCallStateUseCase,
            setPendingToHangUpCallUseCase = setPendingToHangUpCallUseCase,
            contactsRepository = contactsRepository,
            isChatStatusConnectedForCallUseCase = isChatStatusConnectedForCallUseCase,
            defaultDispatcher = testDispatcher
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            callRepository,
            setFakeIncomingCallStateUseCase,
            setPendingToHangUpCallUseCase,
            contactsRepository,
            isChatStatusConnectedForCallUseCase,
        )
    }

    @Test
    fun `test that it emits value when change is received in monitorFakeIncomingCall `() =
        runTest(defaultDispatcher) {
            val map1: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()
            val map2: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()
            val map3: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()
            val map4: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()

            map1[chatId] = FakeIncomingCallState.Notification
            map2[chatId] = FakeIncomingCallState.Screen
            map3[chatId] = FakeIncomingCallState.Remove
            map4[chatId] = FakeIncomingCallState.Dismiss

            whenever(callRepository.monitorFakeIncomingCall()).thenReturn(
                flowOf(
                    map1,
                    map2,
                    map3,
                    map4
                )
            )

            val actual = underTest()
            assertThat(actual).isNotNull()
        }

    @Test
    fun `test that it emits value when change is received in monitorChatCallUpdates when call is hang up`() =
        runTest(defaultDispatcher) {
            val call1 = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.UserNoPresent,
            )

            val map: MutableMap<Long, CallPushMessageNotificationActionType> = mutableMapOf()
            map[chatId] = CallPushMessageNotificationActionType.Update

            whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(true)
            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call1))
            val actual = underTest()
            assertThat(actual).isNotNull()
        }

    @Test
    fun `test that it emits value when change is received in monitorChatCallUpdates when update notification`() =
        runTest(defaultDispatcher) {
            val call1 = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.UserNoPresent,
            )
            val map: MutableMap<Long, CallPushMessageNotificationActionType> = mutableMapOf()
            map[chatId] = CallPushMessageNotificationActionType.Update

            whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(false)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call1))

            val actual = underTest()
            assertThat(actual).isNotNull()
        }

    @Test
    fun `test that it emits value when change is received in monitorChatCallUpdates when call is destroyed`() =
        runTest(defaultDispatcher) {
            val call1 = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.Destroyed,
            )
            val map: MutableMap<Long, CallPushMessageNotificationActionType> = mutableMapOf()
            map[chatId] = CallPushMessageNotificationActionType.Update

            whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(false)
            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call1))
            val actual = underTest()
            assertThat(actual).isNotNull()
        }

    @Test
    fun `test that it emits value when change is received in monitorChatConnectionStateUpdates`() =
        runTest(defaultDispatcher) {
            val state1 = ChatConnectionStatus.Offline
            val state2 = ChatConnectionStatus.Online

            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.InProgress,
            )

            val map = emptyMap<Long, CallPushMessageNotificationActionType>().toMutableMap()
            map[chatId] = CallPushMessageNotificationActionType.Update

            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Remove)
            whenever(callRepository.getChatCall(chatId)).thenReturn(call)

            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(monitorChatConnectionStateUseCase()).thenReturn(
                flowOf(
                    ChatConnectionState(chatId = chatId, chatConnectionStatus = state1),
                    ChatConnectionState(chatId = chatId, chatConnectionStatus = state2)
                )
            )
            val actual = underTest()
            assertThat(actual).isNotNull()
        }
}