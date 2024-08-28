package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.CallRecordingViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.CallRecordingEvent
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSession
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.domain.usecase.call.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallByChatIdUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallSessionOnRecordingUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallRecordingViewModelTest {

    private lateinit var underTest: CallRecordingViewModel

    private val chatId = 123L
    private val peerId = 456L
    private val recordingFlow = MutableSharedFlow<CallRecordingEvent>()
    private val consentFlow = MutableStateFlow<Boolean?>(null)
    private val callFlow = MutableSharedFlow<ChatCall?>()

    private val monitorCallSessionOnRecordingUseCase: MonitorCallSessionOnRecordingUseCase = mock {
        onBlocking { invoke(chatId) } doReturn recordingFlow
    }
    private val hangChatCallByChatIdUseCase = mock<HangChatCallByChatIdUseCase>()
    private val broadcastCallRecordingConsentEventUseCase =
        mock<BroadcastCallRecordingConsentEventUseCase>()
    private val monitorCallRecordingConsentEventUseCase: MonitorCallRecordingConsentEventUseCase =
        mock {
            onBlocking { invoke() } doReturn consentFlow
        }
    private val monitorCallInChatUseCase = mock<MonitorCallInChatUseCase> {
        onBlocking { invoke(chatId) } doReturn callFlow
    }
    private val savedStateHandle: SavedStateHandle = mock {
        on { get<Long>(Constants.CHAT_ID) } doReturn chatId
    }

    @BeforeAll
    internal fun setUp() {
        init()
    }

    private fun init() {
        underTest = CallRecordingViewModel(
            monitorCallSessionOnRecordingUseCase = monitorCallSessionOnRecordingUseCase,
            hangChatCallByChatIdUseCase = hangChatCallByChatIdUseCase,
            broadcastCallRecordingConsentEventUseCase = broadcastCallRecordingConsentEventUseCase,
            monitorCallRecordingConsentEventUseCase = monitorCallRecordingConsentEventUseCase,
            monitorCallInChatUseCase = monitorCallInChatUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(
            hangChatCallByChatIdUseCase,
            broadcastCallRecordingConsentEventUseCase,
            savedStateHandle
        )
        wheneverBlocking { monitorCallSessionOnRecordingUseCase(chatId) }.thenReturn(recordingFlow)
        wheneverBlocking { monitorCallRecordingConsentEventUseCase() }.thenReturn(consentFlow)
        wheneverBlocking { monitorCallInChatUseCase(chatId) }.thenReturn(callFlow)
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
    }

    @Test
    fun `test that call recording event is updated and broadcast consent event is not invoked when recording starts`() =
        runTest {
            val event =
                CallRecordingEvent(isSessionOnRecording = true, participantRecording = "name")

            whenever(monitorCallSessionOnRecordingUseCase(chatId)).thenReturn(recordingFlow)

            init()
            testScheduler.advanceUntilIdle()
            recordingFlow.emit(event)
            underTest.state.map { it.callRecordingEvent }.test {
                Truth.assertThat(awaitItem()).isEqualTo(event)
            }
            verifyNoInteractions(broadcastCallRecordingConsentEventUseCase)
        }

    @Test
    fun `test that call recording event is updated and broadcast consent event is invoked if recording stops`() =
        runTest {
            val event =
                CallRecordingEvent(isSessionOnRecording = false, participantRecording = "name")

            whenever(monitorCallSessionOnRecordingUseCase(chatId)).thenReturn(recordingFlow)

            init()
            testScheduler.advanceUntilIdle()
            recordingFlow.emit(event)
            underTest.state.map { it.callRecordingEvent }.test {
                Truth.assertThat(awaitItem()).isEqualTo(event)
            }
            verify(broadcastCallRecordingConsentEventUseCase, atLeastOnce()).invoke(null)
        }

    @ParameterizedTest(name = " and accepted is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is recording consent accepted is updated if an update is received`(
        accepted: Boolean,
    ) = runTest {
        whenever(monitorCallRecordingConsentEventUseCase()).thenReturn(consentFlow)

        init()
        testScheduler.advanceUntilIdle()
        consentFlow.emit(accepted)
        underTest.state.map { it.isRecordingConsentAccepted }.test {
            Truth.assertThat(awaitItem()).isEqualTo(accepted)
        }
    }

    @Test
    fun `test that state is reset if recording consent event is received as null`() = runTest {
        whenever(monitorCallRecordingConsentEventUseCase()).thenReturn(consentFlow)

        init()
        testScheduler.advanceUntilIdle()
        consentFlow.emit(true)
        consentFlow.emit(null)
        underTest.state.test {
            val item = awaitItem()
            Truth.assertThat(item.callRecordingEvent.isSessionOnRecording).isEqualTo(false)
            Truth.assertThat(item.callRecordingEvent.participantRecording).isNull()
            Truth.assertThat(item.isRecordingConsentAccepted).isNull()
        }
    }

    @Test
    fun `test that set participant recording consumed updates state`() = runTest {
        val name = "name"
        val event =
            CallRecordingEvent(isSessionOnRecording = true, participantRecording = name)

        whenever(monitorCallSessionOnRecordingUseCase(chatId)).thenReturn(recordingFlow)

        init()
        testScheduler.advanceUntilIdle()
        recordingFlow.emit(event)
        underTest.state.map { it.callRecordingEvent.participantRecording }.test {
            Truth.assertThat(awaitItem()).isEqualTo(name)
            underTest.setParticipantRecordingConsumed()
            Truth.assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that set is recording consent accepted as true invokes broadcast call recording consent event and hang chat call and updates state`() =
        runTest {
            init()
            underTest.setIsRecordingConsentAccepted(true)
            consentFlow.emit(true)
            testScheduler.advanceUntilIdle()
            underTest.state.map { it.isRecordingConsentAccepted }.test {
                Truth.assertThat(awaitItem()).isTrue()
            }
            verify(broadcastCallRecordingConsentEventUseCase).invoke(true)
            verifyNoInteractions(hangChatCallByChatIdUseCase)
        }

    @Test
    fun `test that set is recording consent accepted as false invokes broadcast call recording consent event, does not invoke hang chat call and updates state`() =
        runTest {
            init()
            underTest.setIsRecordingConsentAccepted(false)
            consentFlow.emit(false)
            testScheduler.advanceUntilIdle()
            underTest.state.map { it.isRecordingConsentAccepted }.test {
                Truth.assertThat(awaitItem()).isFalse()
            }
            verify(broadcastCallRecordingConsentEventUseCase).invoke(false)
            verify(hangChatCallByChatIdUseCase).invoke(chatId)
        }

    @ParameterizedTest(name = " when call state changes as not joined with {0}")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "WaitingRoom", "TerminatingUserParticipation",
            "GenericNotification", "Destroyed", "Unknown"]
    )
    fun `test that isParticipatingInCall is set to false and broadcast recording consent is invoked`(
        status: ChatCallStatus,
    ) = runTest {
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn status
        }

        whenever(monitorCallInChatUseCase(chatId)).thenReturn(callFlow)

        init()
        callFlow.emit(call)
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.isParticipatingInCall }.test {
            Truth.assertThat(awaitItem()).isFalse()
        }
        verify(broadcastCallRecordingConsentEventUseCase, atLeastOnce()).invoke(null)
    }

    @ParameterizedTest(name = " and call state changes as joined with {0}")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Connecting", "Joining", "InProgress"]
    )
    fun `test that isParticipatingInCall is set to true and broadcast recording consent is not invoked when is recording`(
        status: ChatCallStatus,
    ) = runTest {
        val session1 = mock<ChatSession> {
            on { this.isRecording } doReturn true
            on { this.peerId } doReturn peerId
        }
        val map = mapOf(Pair(peerId, session1))
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn status
            on { sessionByClientId } doReturn map
        }

        whenever(monitorCallInChatUseCase(chatId)).thenReturn(callFlow)

        init()
        callFlow.emit(call)
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.isParticipatingInCall }.test {
            Truth.assertThat(awaitItem()).isTrue()
        }
        verifyNoInteractions(broadcastCallRecordingConsentEventUseCase)
    }

    @ParameterizedTest(name = " and call state changes as joined with {0}")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Connecting", "Joining", "InProgress"]
    )
    fun `test that isParticipatingInCall is set to true and broadcast recording consent is invoked when is not recording`(
        status: ChatCallStatus,
    ) = runTest {
        val session1 = mock<ChatSession> {
            on { this.isRecording } doReturn false
            on { this.peerId } doReturn peerId
        }
        val map = mapOf(Pair(peerId, session1))
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn status
            on { sessionByClientId } doReturn map
        }

        whenever(monitorCallInChatUseCase(chatId)).thenReturn(callFlow)

        init()
        callFlow.emit(call)
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.isParticipatingInCall }.test {
            Truth.assertThat(awaitItem()).isTrue()
        }
        verify(broadcastCallRecordingConsentEventUseCase, atLeastOnce()).invoke(null)
    }

    @ParameterizedTest(name = " and regardless of the status of the call: {0}")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "WaitingRoom", "TerminatingUserParticipation",
            "GenericNotification", "Destroyed", "Unknown", "Connecting", "Joining", "InProgress"]
    )
    fun `test that isSessionOnRecording is set to true when is recording`(
        status: ChatCallStatus,
    ) = runTest {
        val session1 = mock<ChatSession> {
            on { this.isRecording } doReturn true
            on { this.peerId } doReturn peerId
        }
        val map = mapOf(Pair(peerId, session1))
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn status
            on { sessionByClientId } doReturn map
        }

        whenever(monitorCallInChatUseCase(chatId)).thenReturn(callFlow)

        init()
        callFlow.emit(call)
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.callRecordingEvent.isSessionOnRecording }.test {
            Truth.assertThat(awaitItem()).isTrue()
        }
    }

    @ParameterizedTest(name = " and regardless of the status of the call: {0}")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "WaitingRoom", "TerminatingUserParticipation",
            "GenericNotification", "Destroyed", "Unknown", "Connecting", "Joining", "InProgress"]
    )
    fun `test that isSessionOnRecording is set to false when is recording`(
        status: ChatCallStatus,
    ) = runTest {
        val session1 = mock<ChatSession> {
            on { this.isRecording } doReturn false
            on { this.peerId } doReturn peerId
        }
        val map = mapOf(Pair(peerId, session1))
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn status
            on { sessionByClientId } doReturn map
        }

        whenever(monitorCallInChatUseCase(chatId)).thenReturn(callFlow)

        init()
        callFlow.emit(call)
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.callRecordingEvent.isSessionOnRecording }.test {
            Truth.assertThat(awaitItem()).isFalse()
        }
    }
}