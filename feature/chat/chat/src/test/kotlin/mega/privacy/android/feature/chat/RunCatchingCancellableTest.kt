package mega.privacy.android.feature.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class RunCatchingCancellableTest {

    @Test
    fun `test that runCatchingCancellable returns the value when the block succeeds`() = runTest {
        val result = runCatchingCancellable { "value" }

        assertThat(result.getOrNull()).isEqualTo("value")
    }

    @Test
    fun `test that runCatchingCancellable captures an ordinary failure`() = runTest {
        val result = runCatchingCancellable { throw RuntimeException("boom") }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `test that runCatchingCancellable captures a cancellation raised while the caller is still active`() =
        runTest {
            val result = runCatchingCancellable { throw CancellationException("timed out") }

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        }

    @Test
    fun `test that runCatchingCancellable rethrows cancellation when the calling coroutine is cancelled`() =
        runTest {
            lateinit var running: Job
            var result: Result<Unit>? = null
            var thrown: Throwable? = null

            running = launch {
                thrown = runCatching {
                    result = runCatchingCancellable {
                        running.cancel()
                        throw CancellationException("cancelled")
                    }
                }.exceptionOrNull()
            }
            advanceUntilIdle()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
            assertThat(result).isNull()
        }
}
