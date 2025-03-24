package mega.privacy.android.data.extensions

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.extension.onEachSampled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class OnEachSampledTest {

    @Test
    internal fun `test that action is invoked on first emission`() = runTest {
        val flow = MutableSharedFlow<Int>()
        val action = mock<(Int) -> Unit>()
        flow
            .onEachSampled(
                Duration.INFINITE,
                getCurrentTimeMillis = { testScheduler.currentTime },
                action = action
            )
            .test {
                val value = 1
                flow.emit(value)
                verify(action)(value)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    internal fun `test that action is not invoked on second emission when period has not passed`() =
        runTest {
            val flow = MutableSharedFlow<Int>()
            val action = mock<(Int) -> Unit>()
            flow
                .onEachSampled(
                    2.seconds,
                    getCurrentTimeMillis = { testScheduler.currentTime },
                    action = action
                )
                .test {
                    val value = 1
                    flow.emit(value)
                    advanceTimeBy(1.seconds)
                    flow.emit(2)
                    verify(action)(value)
                    verifyNoMoreInteractions(action)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    internal fun `test that action is invoked on second emission when period has passed`() =
        runTest {
            val flow = MutableSharedFlow<Int>()
            val action = mock<(Int) -> Unit>()
            flow
                .onEachSampled(
                    1.seconds,
                    getCurrentTimeMillis = { testScheduler.currentTime },
                    action = action
                )
                .test {
                    val secondValue = 2
                    flow.emit(1)
                    advanceTimeBy(2.seconds)
                    flow.emit(secondValue)
                    verify(action)(secondValue)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    internal fun `test that action is invoked on second emission when shouldEmitImmediately is true even when period has not passed`() =
        runTest {
            val flow = MutableSharedFlow<Int>()
            val action = mock<(Int) -> Unit>()
            val valueToEmitImmediately = 5
            flow
                .onEachSampled(
                    2.seconds,
                    shouldEmitImmediately = { _, new -> new == valueToEmitImmediately },
                    getCurrentTimeMillis = { testScheduler.currentTime },
                    action = action
                )
                .test {
                    (0..valueToEmitImmediately + 2).forEach {
                        flow.emit(it)
                    }
                    verify(action)(0)
                    verify(action)(valueToEmitImmediately)
                    verifyNoMoreInteractions(action)
                    cancelAndIgnoreRemainingEvents()
                }
        }
}