package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.CallRecordingEvent
import mega.privacy.android.domain.entity.call.ChatSession
import mega.privacy.android.domain.entity.call.ChatSessionChanges
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.entity.call.ChatSessionUpdatesResult
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallSessionOnRecordingUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorCallSessionOnRecordingUseCaseTest {

    private lateinit var underTest: MonitorCallSessionOnRecordingUseCase

    private val eventFlow = MutableSharedFlow<ChatSessionUpdatesResult>()

    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase = mock {
        onBlocking { invoke() } doReturn eventFlow
    }
    private val getChatCallUseCase: GetChatCallUseCase = mock()
    private val getParticipantFullNameUseCase: GetParticipantFullNameUseCase = mock()

    private val chatId = 123L
    private val callId = 789L
    private val peerId = 456L
    private val peerName = "peerName"

    @BeforeAll
    internal fun setUp() {
        underTest = MonitorCallSessionOnRecordingUseCase(
            monitorChatSessionUpdatesUseCase = monitorChatSessionUpdatesUseCase,
            getChatCallUseCase = getChatCallUseCase,
            getParticipantFullNameUseCase = getParticipantFullNameUseCase,
        )
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(getChatCallUseCase, getParticipantFullNameUseCase)
        wheneverBlocking { monitorChatSessionUpdatesUseCase() }.thenReturn(eventFlow)
    }

    @Test
    fun `test that call recording event is not emitted when there is no call in chat and no update is received`() =
        runTest {
            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(emptyFlow())
            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(null)

            underTest.invoke(chatId).test {
                cancelAndConsumeRemainingEvents()
            }
        }

    @ParameterizedTest(name = " and isRecording is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that call recording event is emitted when there is no call in chat and an update is received with recording changes`(
        isRecording: Boolean,
    ) = runTest {
        val session = mock<ChatSession> {
            on { this.isRecording } doReturn isRecording
            on { this.peerId } doReturn peerId
            on { this.changes } doReturn listOf(ChatSessionChanges.SessionOnRecording)
            on { this.hasChanged(ChatSessionChanges.SessionOnRecording) } doReturn true
        }
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.callId } doReturn callId
        }
        val result = mock<ChatSessionUpdatesResult> {
            on { this.call } doReturn call
            on { this.session } doReturn session
        }
        val event = CallRecordingEvent(isRecording, peerName)

        whenever(getChatCallUseCase.invoke(chatId)).thenReturn(null)
        whenever(getParticipantFullNameUseCase(peerId)).thenReturn(peerName)

        underTest.invoke(chatId).test {
            eventFlow.emit(result)
            testScheduler.advanceUntilIdle()
            Truth.assertThat(awaitItem()).isEqualTo(event)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that call recording event is emitted when there is no call in chat and an update is received with status changes as Progress and session is recording`() =
        runTest {
            val session = mock<ChatSession> {
                on { this.isRecording } doReturn true
                on { this.peerId } doReturn peerId
                on { this.status } doReturn ChatSessionStatus.Progress
                on { this.changes } doReturn listOf(ChatSessionChanges.Status)
                on { this.hasChanged(ChatSessionChanges.Status) } doReturn true
            }
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { this.callId } doReturn callId
            }
            val result = mock<ChatSessionUpdatesResult> {
                on { this.call } doReturn call
                on { this.session } doReturn session
            }
            val event = CallRecordingEvent(true, null)

            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(null)

            underTest.invoke(chatId).test {
                eventFlow.emit(result)
                testScheduler.advanceUntilIdle()
                Truth.assertThat(awaitItem()).isEqualTo(event)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that call recording event is not emitted when there is no call in chat and an update is received with status changes as Progress and session is not recording`() =
        runTest {
            val session = mock<ChatSession> {
                on { this.isRecording } doReturn false
                on { this.peerId } doReturn peerId
                on { this.status } doReturn ChatSessionStatus.Progress
                on { this.changes } doReturn listOf(ChatSessionChanges.Status)
            }
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { this.callId } doReturn callId
            }
            val result = mock<ChatSessionUpdatesResult> {
                on { this.call } doReturn call
                on { this.session } doReturn session
            }

            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(flowOf(result))
            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(null)
            whenever(getParticipantFullNameUseCase(peerId)).thenReturn(peerName)

            underTest.invoke(chatId).test {
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that call recording events are emitted when there is a call in chat recording and an update with changes SessionOnRecording is received`() =
        runTest {
            val session1 = mock<ChatSession> {
                on { this.isRecording } doReturn true
                on { this.peerId } doReturn peerId
            }
            val map = mapOf(Pair(peerId, session1))
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { sessionByClientId } doReturn map
            }
            val session2 = mock<ChatSession> {
                on { this.isRecording } doReturn false
                on { this.peerId } doReturn peerId
                on { this.changes } doReturn listOf(ChatSessionChanges.SessionOnRecording)
                on { this.hasChanged(ChatSessionChanges.SessionOnRecording) } doReturn true
            }
            val result = mock<ChatSessionUpdatesResult> {
                on { this.call } doReturn call
                on { this.session } doReturn session2
            }
            val event1 = CallRecordingEvent(true, null)
            val event2 = CallRecordingEvent(false, peerName)

            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(call)
            whenever(getParticipantFullNameUseCase(peerId)).thenReturn(peerName)

            underTest.invoke(chatId).test {
                Truth.assertThat(awaitItem()).isEqualTo(event1)
                eventFlow.emit(result)
                testScheduler.advanceUntilIdle()
                Truth.assertThat(awaitItem()).isEqualTo(event2)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that call recording event is not emitted when there is no call in chat and an update is received with SessionOnHold changes`() =
        runTest {
            val session = mock<ChatSession> {
                on { this.peerId } doReturn peerId
                on { this.changes } doReturn listOf(ChatSessionChanges.SessionOnHold)
            }
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { this.callId } doReturn callId
            }
            val result = mock<ChatSessionUpdatesResult> {
                on { this.call } doReturn call
                on { this.session } doReturn session
            }

            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(flowOf(result))
            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(null)
            whenever(getParticipantFullNameUseCase(peerId)).thenReturn(peerName)

            underTest.invoke(chatId).test {
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that call recording event is emitted when there is a call in chat recording and no update is received`() =
        runTest {
            val session = mock<ChatSession> {
                on { this.isRecording } doReturn true
                on { this.peerId } doReturn peerId
            }
            val map = mapOf(Pair(peerId, session))
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { sessionByClientId } doReturn map
            }
            val event = CallRecordingEvent(true, null)

            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(emptyFlow())
            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(call)

            underTest.invoke(chatId).test {
                Truth.assertThat(awaitItem()).isEqualTo(event)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that call recording event is not emitted when there is a call in chat not recording and no update is received`() =
        runTest {
            val session = mock<ChatSession> {
                on { this.isRecording } doReturn false
                on { this.peerId } doReturn peerId
            }
            val map = mapOf(Pair(peerId, session))
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { sessionByClientId } doReturn map
            }

            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(emptyFlow())
            whenever(getChatCallUseCase.invoke(chatId)).thenReturn(call)
            whenever(getParticipantFullNameUseCase(peerId)).thenReturn(peerName)

            underTest.invoke(chatId).test {
                cancelAndConsumeRemainingEvents()
            }
        }
}