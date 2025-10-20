package mega.privacy.android.app.presence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.usecase.chat.RetryConnectionsAndSignalPresenceUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignalPresenceViewModelTest {
    private lateinit var underTest: SignalPresenceViewModel

    private val retryConnectionsAndSignalPresenceUseCase =
        mock<RetryConnectionsAndSignalPresenceUseCase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = SignalPresenceViewModel(
            retryConnectionsAndSignalPresenceUseCase = retryConnectionsAndSignalPresenceUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        reset(retryConnectionsAndSignalPresenceUseCase)
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
}