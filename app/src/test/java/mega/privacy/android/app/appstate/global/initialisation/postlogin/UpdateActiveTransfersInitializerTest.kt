package mega.privacy.android.app.appstate.global.initialisation.postlogin

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersResumedEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.UpdateActiveTransfersUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateActiveTransfersInitializerTest {

    private lateinit var underTest: UpdateActiveTransfersInitializer

    private val updateActiveTransfersUseCase = mock<UpdateActiveTransfersUseCase>()
    private val monitorTransfersResumedEventUseCase = mock<MonitorTransfersResumedEventUseCase>()
    private val monitorTransfersResumedEventFakeFlow = MutableSharedFlow<List<Int>>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateActiveTransfersInitializer(
            monitorTransfersResumedEventUseCase = monitorTransfersResumedEventUseCase,
            updateActiveTransfersUseCase = updateActiveTransfersUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(updateActiveTransfersUseCase, monitorTransfersResumedEventUseCase)
        whenever(monitorTransfersResumedEventUseCase())
            .thenReturn(monitorTransfersResumedEventFakeFlow)
    }

    @Test
    fun `test that updateActiveTransfersUseCase is invoked when monitor emits an event`() =
        runTest {
            val job = launch { underTest("test-session", false) }
            advanceUntilIdle()

            monitorTransfersResumedEventFakeFlow.emit(listOf(1, 2, 3))
            advanceUntilIdle()

            verify(updateActiveTransfersUseCase).invoke()
            job.cancel()
        }

    @Test
    fun `test that updateActiveTransfersUseCase is invoked once per emission`() = runTest {
        val job = launch { underTest("test-session", false) }
        advanceUntilIdle()

        monitorTransfersResumedEventFakeFlow.emit(listOf(1))
        monitorTransfersResumedEventFakeFlow.emit(listOf(2))
        monitorTransfersResumedEventFakeFlow.emit(listOf(3))
        advanceUntilIdle()

        verify(updateActiveTransfersUseCase, times(3)).invoke()
        job.cancel()
    }

    @Test
    fun `test that collection continues when updateActiveTransfersUseCase throws`() = runTest {
        whenever(updateActiveTransfersUseCase()).thenThrow(RuntimeException("Test exception"))

        val job = launch { underTest("test-session", false) }
        advanceUntilIdle()

        monitorTransfersResumedEventFakeFlow.emit(listOf(1))
        monitorTransfersResumedEventFakeFlow.emit(listOf(2))
        advanceUntilIdle()

        verify(updateActiveTransfersUseCase, times(2)).invoke()
        job.cancel()
    }

    @Test
    fun `test that monitor flow is retried when it throws`() = runTest {
        var attempts = 0
        whenever(monitorTransfersResumedEventUseCase()).thenReturn(
            flow {
                attempts++
                if (attempts == 1) {
                    throw RuntimeException("first failure")
                }
                emit(listOf(1))
                awaitCancellation()
            }
        )

        val job = launch { underTest("test-session", false) }
        advanceUntilIdle()

        assertThat(attempts).isEqualTo(2)
        verify(updateActiveTransfersUseCase).invoke()
        job.cancel()
    }
}
