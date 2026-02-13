package mega.privacy.android.domain.usecase.call

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.CallRecordingEvent
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSession
import mega.privacy.android.domain.entity.call.ChatSessionChanges
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.entity.call.ChatSessionUpdatesResult
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

class MonitorRecordedChatsUseCaseTest {
    private lateinit var underTest: MonitorRecordedChatsUseCase

    private val monitorActiveCallUseCase = mock<MonitorActiveCallUseCase>()
    private val monitorChatSessionUpdatesUseCase = mock<MonitorChatSessionUpdatesUseCase>()
    private val getParticipantFullNameUseCase = mock<GetParticipantFullNameUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorRecordedChatsUseCase(
            monitorActiveCallUseCase = monitorActiveCallUseCase,
            monitorChatSessionUpdatesUseCase = monitorChatSessionUpdatesUseCase,
            getParticipantFullNameUseCase = getParticipantFullNameUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorActiveCallUseCase,
            monitorChatSessionUpdatesUseCase,
            getParticipantFullNameUseCase,
        )
    }

    @ParameterizedTest(name = "initial value when status is {0} and no recording session expects no initial event")
    @EnumSource(ChatCallStatus::class)
    fun `test that initial value when no recording session expects no initial event`(status: ChatCallStatus) =
        runTest {
            val chatId = 100L
            val call = createChatCall(chatId = chatId, status = status, hasRecordingSession = false)
            whenever(monitorActiveCallUseCase()).thenReturn(flowOf(listOf(call)))
            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(emptyFlow())

            underTest().test {
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "initial value when status is {0} and has recording session expects PreexistingRecording")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["InProgress", "Connecting", "Joining"],
    )
    fun `test that initial value when has recording session expects PreexistingRecording`(status: ChatCallStatus) =
        runTest {
            val chatId = 101L
            val call = createChatCall(chatId = chatId, status = status, hasRecordingSession = true)
            whenever(monitorActiveCallUseCase()).thenReturn(flowOf(listOf(call)))
            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(emptyFlow())

            underTest().test {
                assertThat(awaitItem()).isEqualTo(CallRecordingEvent.PreexistingRecording(chatId))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when monitorActiveCallUseCase emits null then NotRecording is emitted`() = runTest {
        whenever(monitorActiveCallUseCase()).thenReturn(flowOf(null))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(CallRecordingEvent.NotRecording)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when multiple calls and one has recording session then only that call emits PreexistingRecording`() =
        runTest {
            val chatIdRecorded = 200L
            val chatIdNotRecorded = 201L
            val callRecorded = createChatCall(
                chatId = chatIdRecorded,
                status = ChatCallStatus.InProgress,
                hasRecordingSession = true,
            )
            val callNotRecorded = createChatCall(
                chatId = chatIdNotRecorded,
                status = ChatCallStatus.InProgress,
                hasRecordingSession = false,
            )
            whenever(monitorActiveCallUseCase()).thenReturn(
                flowOf(
                    listOf(
                        callRecorded,
                        callNotRecorded
                    )
                )
            )
            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(emptyFlow())

            underTest().test {
                assertThat(awaitItem()).isEqualTo(
                    CallRecordingEvent.PreexistingRecording(
                        chatIdRecorded
                    )
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when call did not start recorded then SessionOnRecording change with isRecording true emits Recording`() =
        runTest {
            val chatId = 300L
            val peerId = 400L
            val participantName = "Participant Name"
            val activeCallFlow = MutableSharedFlow<List<ChatCall>?>(replay = 0)
            val sessionUpdatesFlow = MutableSharedFlow<ChatSessionUpdatesResult>(replay = 0)

            val call = createChatCall(
                chatId = chatId,
                status = ChatCallStatus.InProgress,
                hasRecordingSession = false,
            )
            val session = createSessionWithRecordingChange(
                peerId = peerId,
                isRecording = true,
                sessionOnRecordingChanged = true,
                statusChanged = false,
            )
            val result = createSessionUpdatesResult(session = session, chatId = chatId)

            whenever(monitorActiveCallUseCase()).thenReturn(activeCallFlow)
            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(sessionUpdatesFlow)
            wheneverBlocking { getParticipantFullNameUseCase(peerId) }.thenReturn(participantName)

            underTest().test {
                activeCallFlow.emit(listOf(call))
                sessionUpdatesFlow.emit(result)
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(
                    CallRecordingEvent.Recording(chatId, participantName),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when call did not start recorded then SessionOnRecording change with isRecording false emits RecordingEnded`() =
        runTest {
            val chatId = 301L
            val peerId = 401L
            val activeCallFlow = MutableSharedFlow<List<ChatCall>?>(replay = 0)
            val sessionUpdatesFlow = MutableSharedFlow<ChatSessionUpdatesResult>(replay = 0)

            val call = createChatCall(
                chatId = chatId,
                status = ChatCallStatus.InProgress,
                hasRecordingSession = false,
            )
            val session = createSessionWithRecordingChange(
                peerId = peerId,
                isRecording = false,
                sessionOnRecordingChanged = true,
                statusChanged = false,
            )
            val result = createSessionUpdatesResult(session = session, chatId = chatId)

            whenever(monitorActiveCallUseCase()).thenReturn(activeCallFlow)
            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(sessionUpdatesFlow)

            underTest().test {
                activeCallFlow.emit(listOf(call))
                sessionUpdatesFlow.emit(result)
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(CallRecordingEvent.RecordingEnded(chatId))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when session update has Status change with Progress and isRecording then emits PreexistingRecording`() =
        runTest {
            val chatId = 302L
            val peerId = 402L
            val activeCallFlow = MutableSharedFlow<List<ChatCall>?>(replay = 0)
            val sessionUpdatesFlow = MutableSharedFlow<ChatSessionUpdatesResult>(replay = 0)

            val call = createChatCall(
                chatId = chatId,
                status = ChatCallStatus.InProgress,
                hasRecordingSession = false,
            )
            val session = createSessionWithStatusChangeToProgressAndRecording(
                peerId = peerId,
                isRecording = true,
            )
            val result = createSessionUpdatesResult(session = session, chatId = chatId)

            whenever(monitorActiveCallUseCase()).thenReturn(activeCallFlow)
            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(sessionUpdatesFlow)

            underTest().test {
                activeCallFlow.emit(listOf(call))
                sessionUpdatesFlow.emit(result)
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(CallRecordingEvent.PreexistingRecording(chatId))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when getParticipantFullNameUseCase returns null then Recording event has null participantRecording`() =
        runTest {
            val chatId = 303L
            val peerId = 403L
            val activeCallFlow = MutableSharedFlow<List<ChatCall>?>(replay = 0)
            val sessionUpdatesFlow = MutableSharedFlow<ChatSessionUpdatesResult>(replay = 0)

            val call = createChatCall(
                chatId = chatId,
                status = ChatCallStatus.InProgress,
                hasRecordingSession = false,
            )
            val session = createSessionWithRecordingChange(
                peerId = peerId,
                isRecording = true,
                sessionOnRecordingChanged = true,
                statusChanged = false,
            )
            val result = createSessionUpdatesResult(session = session, chatId = chatId)

            whenever(monitorActiveCallUseCase()).thenReturn(activeCallFlow)
            whenever(monitorChatSessionUpdatesUseCase()).thenReturn(sessionUpdatesFlow)
            wheneverBlocking { getParticipantFullNameUseCase(any()) }.thenReturn(null)

            underTest().test {
                activeCallFlow.emit(listOf(call))
                sessionUpdatesFlow.emit(result)
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(
                    CallRecordingEvent.Recording(chatId, null),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createChatCall(
        chatId: Long,
        status: ChatCallStatus,
        hasRecordingSession: Boolean,
    ): ChatCall {
        val session = mock<ChatSession> {
            on { this.isRecording } doReturn hasRecordingSession
        }
        return mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn status
            on { sessionByClientId } doReturn mapOf(1L to session)
        }
    }

    private fun createSessionWithRecordingChange(
        peerId: Long,
        isRecording: Boolean,
        sessionOnRecordingChanged: Boolean,
        statusChanged: Boolean,
    ): ChatSession = mock<ChatSession> {
        on { this.peerId } doReturn peerId
        on { this.isRecording } doReturn isRecording
        on { this.changes } doReturn buildList {
            if (sessionOnRecordingChanged) add(ChatSessionChanges.SessionOnRecording)
            if (statusChanged) add(ChatSessionChanges.Status)
        }
        on { hasChanged(ChatSessionChanges.SessionOnRecording) } doReturn sessionOnRecordingChanged
        on { hasChanged(ChatSessionChanges.Status) } doReturn statusChanged
    }

    private fun createSessionWithStatusChangeToProgressAndRecording(
        peerId: Long,
        isRecording: Boolean,
    ): ChatSession = mock<ChatSession> {
        on { this.peerId } doReturn peerId
        on { this.isRecording } doReturn isRecording
        on { this.status } doReturn ChatSessionStatus.Progress
        on { this.changes } doReturn listOf(ChatSessionChanges.Status)
        on { hasChanged(ChatSessionChanges.Status) } doReturn true
        on { hasChanged(ChatSessionChanges.SessionOnRecording) } doReturn false
    }

    private fun createSessionUpdatesResult(
        session: ChatSession,
        chatId: Long,
    ): ChatSessionUpdatesResult {
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
        }
        return ChatSessionUpdatesResult(session = session, call = call)
    }
}
