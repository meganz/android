package mega.privacy.android.feature.sync.domain.extension

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.milliseconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FlowThrottleTest {

    private val window = 200.milliseconds

    private val burst = flow {
        emit(1)
        delay(50)
        emit(2)
        delay(50)
        emit(3)
    }

    @Test
    fun `test that throttleLatest emits the first value at once and the latest at the window end`() =
        runTest {
            val result = collectTimed(burst.throttleLatest(window))

            assertThat(result.items).containsExactly(1 to 0L, 3 to 200L).inOrder()
        }

    @Test
    fun `test that throttleLatest drops values superseded within a window`() = runTest {
        val result = collectTimed(burst.throttleLatest(window))

        assertThat(result.items.map { it.first }).doesNotContain(2)
    }

    @Test
    fun `test that throttleLatest emits at once after a quiet window`() = runTest {
        val upstream = flow {
            emit(1)
            delay(500)
            emit(2)
        }

        val result = collectTimed(upstream.throttleLatest(window))

        assertThat(result.items).containsExactly(1 to 0L, 2 to 500L).inOrder()
    }

    @Test
    fun `test that throttleLatest emits the last value when the upstream completes`() = runTest {
        val upstream = flow {
            emit(1)
            delay(10)
            emit(2)
        }

        val result = collectTimed(upstream.throttleLatest(window))

        assertThat(result.items).containsExactly(1 to 0L, 2 to 200L).inOrder()
        assertThat(result.error).isNull()
    }

    @Test
    fun `test that throttleLatest propagates an upstream exception`() = runTest {
        val upstream = flow {
            emit(1)
            delay(10)
            throw IllegalStateException("upstream failed")
        }

        val result = collectTimed(upstream.throttleLatest(window))

        assertThat(result.items).containsExactly(1 to 0L)
        assertThat(result.error).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.error).hasMessageThat().isEqualTo("upstream failed")
    }

    @Test
    fun `test that throttleLatest emits nothing for an empty upstream`() = runTest {
        val result = collectTimed(emptyFlow<Int>().throttleLatest(window))

        assertThat(result.items).isEmpty()
        assertThat(result.error).isNull()
    }

    private suspend fun TestScope.collectTimed(flow: Flow<Int>): Collected {
        val items = mutableListOf<Pair<Int, Long>>()
        val error = runCatching {
            flow.collect { items += it to testScheduler.currentTime }
        }.exceptionOrNull()
        return Collected(items, error)
    }

    private data class Collected(val items: List<Pair<Int, Long>>, val error: Throwable?)
}
