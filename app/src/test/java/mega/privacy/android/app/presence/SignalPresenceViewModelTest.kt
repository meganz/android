package mega.privacy.android.app.presence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.usecase.MonitorChatSignalPresenceUseCase
import mega.privacy.android.domain.usecase.chat.RetryConnectionsAndSignalPresenceUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignalPresenceViewModelTest {
    private lateinit var underTest: SignalPresenceViewModel

    private val retryConnectionsAndSignalPresenceUseCase =
        mock<RetryConnectionsAndSignalPresenceUseCase>()
    private val monitorChatSignalPresenceUseCase =
        mock<MonitorChatSignalPresenceUseCase>()
    private val monitorChatSignalPresenceFlow = MutableSharedFlow<Unit>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        whenever(monitorChatSignalPresenceUseCase()).thenReturn(monitorChatSignalPresenceFlow)
        underTest = SignalPresenceViewModel(
            retryConnectionsAndSignalPresenceUseCase = retryConnectionsAndSignalPresenceUseCase,
            monitorChatSignalPresenceUseCase = monitorChatSignalPresenceUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        reset(retryConnectionsAndSignalPresenceUseCase, monitorChatSignalPresenceUseCase)
    }

    @Test
    fun `test that signal is sent when signal presence is called the first time`() = runTest {
        underTest.signalPresence()
        advanceUntilIdle()
        verify(retryConnectionsAndSignalPresenceUseCase).invoke()
    }

    @Test
    fun `test that subsequent calls are debounced by 500ms`() = runTest {
        underTest.signalPresence()
        advanceUntilIdle()
        verify(retryConnectionsAndSignalPresenceUseCase).invoke()
        underTest.signalPresence()
        underTest.signalPresence()
        underTest.signalPresence()
        advanceTimeBy(501L)
        verify(retryConnectionsAndSignalPresenceUseCase, times(2)).invoke()
    }

    @Test
    fun `test that signal is sent when monitorChatSignalPresence emits and delaySignalPresence is true`() =
        runTest {
            whenever(retryConnectionsAndSignalPresenceUseCase()) doReturn true
            underTest.signalPresence()
            advanceUntilIdle()
            verify(retryConnectionsAndSignalPresenceUseCase).invoke()

            // Emit from monitorChatSignalPresenceUseCase
            monitorChatSignalPresenceFlow.emit(Unit)
            advanceUntilIdle()

            // Should be called again because delaySignalPresence was true
            verify(retryConnectionsAndSignalPresenceUseCase, times(2)).invoke()
        }

    @Test
    fun `test that signal is not sent when monitorChatSignalPresence emits and delaySignalPresence is false`() =
        runTest {
            whenever(retryConnectionsAndSignalPresenceUseCase()) doReturn false
            underTest.signalPresence()
            advanceUntilIdle()
            verify(retryConnectionsAndSignalPresenceUseCase).invoke()

            // Emit from monitorChatSignalPresenceUseCase
            monitorChatSignalPresenceFlow.emit(Unit)
            advanceUntilIdle()

            // Should not be called again because delaySignalPresence was false
            verify(retryConnectionsAndSignalPresenceUseCase, times(2)).invoke()
        }
}