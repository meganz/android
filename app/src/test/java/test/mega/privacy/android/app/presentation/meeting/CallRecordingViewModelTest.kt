package test.mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.CallRecordingViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.meeting.CallRecordingEvent
import mega.privacy.android.domain.usecase.meeting.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.HangChatCallByChatIdUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallFinishedByChatIdUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallSessionOnRecordingUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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

    private val monitorCallSessionOnRecordingUseCase: MonitorCallSessionOnRecordingUseCase = mock {
        onBlocking { invoke(chatId) } doReturn emptyFlow()
    }
    private val hangChatCallByChatIdUseCase = mock<HangChatCallByChatIdUseCase>()
    private val broadcastCallRecordingConsentEventUseCase =
        mock<BroadcastCallRecordingConsentEventUseCase>()
    private val monitorCallRecordingConsentEventUseCase: MonitorCallRecordingConsentEventUseCase =
        mock {
            onBlocking { invoke() } doReturn MutableStateFlow(null)
        }
    private val monitorCallFinishedByChatIdUseCase: MonitorCallFinishedByChatIdUseCase = mock {
        onBlocking { invoke(chatId) } doReturn Unit
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
            monitorCallFinishedByChatIdUseCase = monitorCallFinishedByChatIdUseCase,
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
        wheneverBlocking { monitorCallSessionOnRecordingUseCase(chatId) }.thenReturn(emptyFlow())
        wheneverBlocking { monitorCallRecordingConsentEventUseCase() }
            .thenReturn(MutableStateFlow(null))
        wheneverBlocking { monitorCallFinishedByChatIdUseCase(chatId) }.thenReturn(Unit)
        whenever(savedStateHandle.get<Long>(Constants.CHAT_ID)).thenReturn(chatId)
    }

    @Test
    fun `test that call recording event is updated if an update is received`() =
        runTest {
            val eventFlow = MutableSharedFlow<CallRecordingEvent>()
            val event =
                CallRecordingEvent(isSessionOnRecording = true, participantRecording = "name")

            whenever(monitorCallSessionOnRecordingUseCase(chatId)).thenReturn(eventFlow)

            init()
            testScheduler.advanceUntilIdle()
            eventFlow.emit(event)
            underTest.state.map { it.callRecordingEvent }.test {
                Truth.assertThat(awaitItem()).isEqualTo(event)
            }
        }

    @ParameterizedTest(name = " and accepted is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is recording consent accepted is updated if an update is received`(
        accepted: Boolean,
    ) = runTest {
        val eventFlow = MutableStateFlow<Boolean?>(null)

        whenever(monitorCallRecordingConsentEventUseCase()).thenReturn(eventFlow)

        init()
        testScheduler.advanceUntilIdle()
        eventFlow.emit(accepted)
        underTest.state.map { it.isRecordingConsentAccepted }.test {
            Truth.assertThat(awaitItem()).isEqualTo(accepted)
        }
    }

    @Test
    fun `test that state is reset if recording consent event is received as null`() = runTest {
        val eventFlow = MutableStateFlow<Boolean?>(null)

        whenever(monitorCallRecordingConsentEventUseCase()).thenReturn(eventFlow)

        init()
        testScheduler.advanceUntilIdle()
        eventFlow.emit(true)
        eventFlow.emit(null)
        underTest.state.test {
            val item = awaitItem()
            Truth.assertThat(item.callRecordingEvent.isSessionOnRecording).isEqualTo(false)
            Truth.assertThat(item.callRecordingEvent.participantRecording).isNull()
            Truth.assertThat(item.isRecordingConsentAccepted).isNull()
        }
    }

    @Test
    fun `test that set participant recording consumed updates state`() = runTest {
        val eventFlow = MutableSharedFlow<CallRecordingEvent>()
        val name = "name"
        val event =
            CallRecordingEvent(isSessionOnRecording = true, participantRecording = name)

        whenever(monitorCallSessionOnRecordingUseCase(chatId)).thenReturn(eventFlow)

        init()
        testScheduler.advanceUntilIdle()
        eventFlow.emit(event)
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
            underTest.state.map { it.isRecordingConsentAccepted }.test {
                Truth.assertThat(awaitItem()).isFalse()
            }
            verify(broadcastCallRecordingConsentEventUseCase).invoke(false)
            verify(hangChatCallByChatIdUseCase).invoke(chatId)
        }
}