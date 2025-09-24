package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
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
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.meeting.CallPushMessageNotificationActionType
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorCallPushNotificationUseCaseTest {

    private val callRepository = mock<CallRepository>()
    private val contactsRepository = mock<ContactsRepository>()
    private val setFakeIncomingCallStateUseCase = mock<SetFakeIncomingCallStateUseCase>()
    private val setPendingToHangUpCallUseCase = mock<SetPendingToHangUpCallUseCase>()
    private val getMyUserHandleUseCase = mock<GetMyUserHandleUseCase>()

    private val defaultDispatcher = UnconfinedTestDispatcher()

    private lateinit var underTest: MonitorCallPushNotificationUseCase
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
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
            getMyUserHandleUseCase = getMyUserHandleUseCase,
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
            getMyUserHandleUseCase
        )

        // Set up default empty flows for all repository methods
        whenever(callRepository.monitorFakeIncomingCall()).thenReturn(emptyFlow())
        whenever(callRepository.monitorChatCallUpdates()).thenReturn(emptyFlow())
        whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(emptyFlow())
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
            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(
                    ChatConnectionState(chatId = chatId, chatConnectionStatus = state1),
                    ChatConnectionState(chatId = chatId, chatConnectionStatus = state2)
                )
            )
            val actual = underTest()
            assertThat(actual).isNotNull()
        }

    @Test
    fun `test that monitorFakeIncomingCallUpdates emits correct action types for all FakeIncomingCallState values`() =
        runTest(defaultDispatcher) {
            val testMap: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()
            testMap[chatId] = FakeIncomingCallState.Notification

            whenever(callRepository.monitorFakeIncomingCall()).thenReturn(flowOf(testMap))

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Show)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorFakeIncomingCallUpdates emits Hide action for Screen state`() =
        runTest(defaultDispatcher) {
            val testMap: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()
            testMap[chatId] = FakeIncomingCallState.Screen

            whenever(callRepository.monitorFakeIncomingCall()).thenReturn(flowOf(testMap))

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Hide)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorFakeIncomingCallUpdates emits Remove action for Dismiss state`() =
        runTest(defaultDispatcher) {
            val testMap: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()
            testMap[chatId] = FakeIncomingCallState.Dismiss

            whenever(callRepository.monitorFakeIncomingCall()).thenReturn(flowOf(testMap))

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Remove)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorFakeIncomingCallUpdates emits Remove action and calls setFakeIncomingCallUseCase for Remove state`() =
        runTest(defaultDispatcher) {
            val testMap: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()
            testMap[chatId] = FakeIncomingCallState.Remove

            whenever(callRepository.monitorFakeIncomingCall()).thenReturn(flowOf(testMap))

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Remove)
                cancelAndIgnoreRemainingEvents()
            }

            verify(setFakeIncomingCallStateUseCase).invoke(chatId = chatId, type = null)
        }

    @Test
    fun `test that monitorChatConnectionStateUpdates emits Missed action when call is null and notification is active`() =
        runTest(defaultDispatcher) {
            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(
                    ChatConnectionState(
                        chatId = chatId,
                        chatConnectionStatus = ChatConnectionStatus.Online
                    )
                )
            )
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(callRepository.getChatCall(chatId)).thenReturn(null)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Missed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(setFakeIncomingCallStateUseCase).invoke(chatId = chatId, type = null)
            verify(setPendingToHangUpCallUseCase).invoke(chatId = chatId, add = false)
        }

    @Test
    fun `test that monitorChatConnectionStateUpdates emits Remove action when call is InProgress and user is participant`() =
        runTest(defaultDispatcher) {
            val myUserHandle = 999L
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.InProgress,
                peerIdParticipants = listOf(myUserHandle)
            )

            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(
                    ChatConnectionState(
                        chatId = chatId,
                        chatConnectionStatus = ChatConnectionStatus.Online
                    )
                )
            )
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(callRepository.getChatCall(chatId)).thenReturn(call)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
            whenever(getMyUserHandleUseCase()).thenReturn(myUserHandle)

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Remove)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorChatConnectionStateUpdates emits Remove action when call is Joining and user is participant`() =
        runTest(defaultDispatcher) {
            val myUserHandle = 999L
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.Joining,
                peerIdParticipants = listOf(myUserHandle)
            )

            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(
                    ChatConnectionState(
                        chatId = chatId,
                        chatConnectionStatus = ChatConnectionStatus.Online
                    )
                )
            )
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(callRepository.getChatCall(chatId)).thenReturn(call)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
            whenever(getMyUserHandleUseCase()).thenReturn(myUserHandle)

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Remove)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorChatConnectionStateUpdates emits Update action when call exists but user is not participant`() =
        runTest(defaultDispatcher) {
            val myUserHandle = 999L
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.UserNoPresent,
                peerIdParticipants = listOf(888L) // Different user
            )

            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(
                    ChatConnectionState(
                        chatId = chatId,
                        chatConnectionStatus = ChatConnectionStatus.Online
                    )
                )
            )
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(callRepository.getChatCall(chatId)).thenReturn(call)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
            whenever(getMyUserHandleUseCase()).thenReturn(myUserHandle)

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Update)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorChatConnectionStateUpdates hangs up call when pending to hang up`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.InProgress
            )

            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(
                    ChatConnectionState(
                        chatId = chatId,
                        chatConnectionStatus = ChatConnectionStatus.Online
                    )
                )
            )
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(callRepository.getChatCall(chatId)).thenReturn(call)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
            whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(true)

            underTest().test {
                awaitItem()
                verify(callRepository).hangChatCall(callId)
                verify(setPendingToHangUpCallUseCase).invoke(chatId = chatId, add = false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorChatCallUpdates emits Remove action when call composition changes and user is added`() =
        runTest(defaultDispatcher) {
            val myUserHandle = 999L
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                peerIdParticipants = listOf(myUserHandle),
                status = ChatCallStatus.UserNoPresent, // Changed to UserNoPresent as per new implementation
                changes = listOf(ChatCallChanges.CallComposition),
                callCompositionChange = CallCompositionChanges.Added,
                peerIdCallCompositionChange = myUserHandle // Added this field as per new implementation
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(getMyUserHandleUseCase()).thenReturn(myUserHandle)

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Remove)
                cancelAndIgnoreRemainingEvents()
            }

            // Verify that setFakeIncomingCallUseCase is called with Remove
            verify(setFakeIncomingCallStateUseCase).invoke(
                chatId = chatId,
                type = FakeIncomingCallState.Remove
            )
        }

    @Test
    fun `test that monitorChatCallUpdates calls setFakeIncomingCallUseCase when call status changes to Joining`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.Joining,
                changes = listOf(ChatCallChanges.Status)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(callId)).thenReturn(false)

            underTest().test {
                awaitComplete()
                verify(setFakeIncomingCallStateUseCase).invoke(
                    chatId = chatId,
                    type = FakeIncomingCallState.Remove
                )
            }
        }

    @Test
    fun `test that monitorChatCallUpdates calls setFakeIncomingCallUseCase when call status changes to InProgress`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.InProgress,
                changes = listOf(ChatCallChanges.Status)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(callId)).thenReturn(false)

            underTest().test {
                awaitComplete()
                verify(setFakeIncomingCallStateUseCase).invoke(
                    chatId = chatId,
                    type = FakeIncomingCallState.Remove
                )
            }
        }

    @Test
    fun `test that monitorChatCallUpdates calls setFakeIncomingCallUseCase when call status changes to TerminatingUserParticipation`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.TerminatingUserParticipation,
                changes = listOf(ChatCallChanges.Status)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(callId)).thenReturn(false)

            underTest().test {
                awaitComplete()
                verify(setFakeIncomingCallStateUseCase).invoke(
                    chatId = chatId,
                    type = FakeIncomingCallState.Remove
                )
            }
        }

    @Test
    fun `test that monitorChatCallUpdates calls setFakeIncomingCallUseCase when call status changes to Destroyed`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.Destroyed,
                changes = listOf(ChatCallChanges.Status)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(callId)).thenReturn(false)

            underTest().test {
                awaitComplete()
                verify(setFakeIncomingCallStateUseCase).invoke(
                    chatId = chatId,
                    type = FakeIncomingCallState.Remove
                )
            }
        }

    @Test
    fun `test that monitorChatCallUpdates calls setPendingToHangUpCallUseCase when call is pending to hang up`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.Joining,
                changes = listOf(ChatCallChanges.Status)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(callId)).thenReturn(true)

            underTest().test {
                awaitComplete()
                verify(setPendingToHangUpCallUseCase).invoke(chatId = chatId, add = false)
            }
        }

    @Test
    fun `test that monitorChatCallUpdates emits Update action when status changes to UserNoPresent and conditions are met`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.UserNoPresent,
                changes = listOf(ChatCallChanges.Status)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(false)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Update)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorChatCallUpdates emits Update action when ringing status changes to UserNoPresent and conditions are met`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.UserNoPresent,
                changes = listOf(ChatCallChanges.RingingStatus)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(false)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Update)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorChatCallUpdates hangs up call when pending to hang up and status is UserNoPresent`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.UserNoPresent,
                changes = listOf(ChatCallChanges.Status)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(true)

            underTest().test {
                awaitComplete()
                verify(callRepository).hangChatCall(callId)
                verify(setPendingToHangUpCallUseCase).invoke(chatId = chatId, add = false)
            }
        }

    @Test
    fun `test that monitorChatCallUpdates does not emit when changes is null`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.InProgress,
                changes = null
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))

            underTest().test {
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorChatCallUpdates does not emit when changes is empty`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.InProgress,
                changes = emptyList()
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))

            underTest().test {
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorChatConnectionStateUpdates does not emit when not connected`() =
        runTest(defaultDispatcher) {
            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(
                    ChatConnectionState(
                        chatId = chatId,
                        chatConnectionStatus = ChatConnectionStatus.Online
                    )
                )
            )
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(false)

            underTest().test {
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorChatConnectionStateUpdates does not emit when notification is not active`() =
        runTest(defaultDispatcher) {
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.InProgress
            )

            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(
                    ChatConnectionState(
                        chatId = chatId,
                        chatConnectionStatus = ChatConnectionStatus.Online
                    )
                )
            )
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(callRepository.getChatCall(chatId)).thenReturn(call)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Screen)

            underTest().test {
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorFakeIncomingCallUpdates filters empty maps`() =
        runTest(defaultDispatcher) {
            val emptyMap: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()

            whenever(callRepository.monitorFakeIncomingCall()).thenReturn(flowOf(emptyMap))

            underTest().test {
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorChatCallUpdates emits Remove action when user is already participating in InProgress call`() =
        runTest(defaultDispatcher) {
            val myUserHandle = 999L
            val call = ChatCall(
                chatId = chatId,
                callId = callId,
                status = ChatCallStatus.UserNoPresent,
                changes = listOf(ChatCallChanges.Status),
                peerIdParticipants = listOf(myUserHandle)
            )

            whenever(callRepository.monitorChatCallUpdates()).thenReturn(flowOf(call))
            whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(false)
            whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
            whenever(isChatStatusConnectedForCallUseCase(chatId)).thenReturn(true)
            whenever(getMyUserHandleUseCase()).thenReturn(myUserHandle)

            underTest().test {
                val item = awaitItem()
                assertThat(item).isNotEmpty()
                assertThat(item[chatId]).isEqualTo(CallPushMessageNotificationActionType.Remove)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
