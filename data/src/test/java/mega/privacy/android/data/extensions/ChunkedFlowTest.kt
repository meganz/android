package mega.privacy.android.data.extensions

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ChunkedFlowTest {


    @Test
    internal fun `test that chunked does not emit after duration if no values`() = runTest {
        val emptyFlow = flowOf<Int>().chunked(100.milliseconds)

        emptyFlow.test {
            awaitComplete()
        }
    }


    @Test
    internal fun `test that chunked emits multiple items in a single chunk`() = runTest {
        val chunkDuration = 100
        val expected = listOf(1, 2, 3, 4, 5)
        val flow = expected.asFlow().chunked(chunkDuration.milliseconds)

        val actual = mutableListOf<List<Int>>()

        flow.test {
            actual.add(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        advanceTimeBy(chunkDuration.milliseconds)

        println(actual)
        assertThat(actual.flatten()).isEqualTo(expected)
    }

    @Test
    internal fun `test that chunked returns multiple single value chunks if events span multiple durations`() =
        runTest {
            val itemDelay = 50
            val chunkDuration = itemDelay - 1
            val totalItems = 10
            val totalTime = (totalItems + 1) * itemDelay
            val chunked = (1..totalItems).asFlow().onEach { delay(itemDelay.milliseconds) }
                .chunked(chunkDuration.milliseconds)
            val actual = mutableListOf<List<Int>>()
            val job = launch {
                chunked.test {
                    while (coroutineContext.isActive) {
                        val event = awaitEvent()
                        if (event is Event.Item) {
                            val chunk = event.value
                            actual.add(chunk)
                        } else break
                    }
                }
            }
            advanceTimeBy(totalTime.milliseconds)
            job.cancel()

            actual.forEach {
                assertThat(it).hasSize(1)
            }

            assertThat(actual.flatten()).hasSize(totalItems)

        }

    @Test
    internal fun `test that chunked returns multiple items per chunk if emitted within chunk duration`() =
        runTest {
            val itemDelay = 5.0
            val chunkDuration = itemDelay * 3.0
            val chunkRatio = chunkDuration / itemDelay
            val totalItems = 10
            val chunked = (1..totalItems).asFlow().onEach { delay(itemDelay.milliseconds) }
                .chunked(chunkDuration.milliseconds)

            val floor = floor(chunkRatio).toInt()
            val ceiling = ceil(chunkRatio).toInt()

            val actual = mutableListOf<List<Int>>()
            val job = launch {
                chunked.test {
                    while (coroutineContext.isActive) {
                        val event = awaitEvent()
                        if (event is Event.Item) {
                            val chunk = event.value
                            actual.add(chunk)
                        } else break
                    }
                }
            }


            for (i in 0..totalItems) {
                advanceTimeBy(itemDelay.milliseconds)
            }
            advanceTimeBy(chunkDuration.milliseconds) //Make sure we get the last chunk
            job.cancel()

            actual.forEachIndexed() { index, chunk ->
                println(chunk)
                // Due to timing tolerances the first chunk can be smaller than expected
                // The last chunk can be smaller than expected if we run out of items
                if (index == 0 || index == actual.size - 1) {
                    assertThat(chunk.size).isAtLeast(1)
                } else {
                    assertThat(chunk.size).isAtLeast(floor)
                }
                assertThat(chunk.size).isAtMost(ceiling)
            }

            assertThat(actual.flatten()).hasSize(totalItems)
        }

    @Test
    internal fun `test that chunked emits last chunk as ChunkCancelledWithPendingValuesException in on completion if cancelled`() =
        runTest {
            val itemDelay = 5.0
            val chunkDuration = itemDelay * 3.0
            val totalItems = 10
            val emittedValues = mutableListOf<Int>()
            var pendingValues = emptyList<Int>()

            val timeBasedFlow = (1..totalItems).asFlow()
                .onEach {
                    delay(itemDelay.milliseconds)
                    emittedValues.add(it)
                }
                .chunked(chunkDuration.milliseconds)

            val actual = mutableListOf<List<Int>>()
            val job = launch {
                timeBasedFlow
                    .onCompletion {
                        if (it is ChunkCancelledWithPendingValuesException) {
                            pendingValues = it.pendingValues.filterIsInstance<Int>()
                        }
                    }
                    .collect { chunk ->
                        actual.add(chunk)
                    }
            }

            advanceTimeBy((chunkDuration + itemDelay * 2).milliseconds)
            job.cancel()
            yield() //wait for onCompletion to be processed

            println(actual)
            //test the test: we have some values, but not all
            assertThat(actual.flatten().size).isAtLeast(1)
            assertThat(actual.flatten().size).isLessThan(emittedValues.size)

            actual.add(pendingValues)
            println(actual)
            assertThat(actual.flatten()).isEqualTo(emittedValues)
        }

    @Test
    internal fun `test that ChunkCancelledWithPendingValuesException is not thrown if there are no pending values when cancelled`() =
        runTest {
            val chunkDuration = 5.0

            val chunkedFlow = MutableStateFlow(1)
                .chunked(chunkDuration.milliseconds)

            val actual = mutableListOf<List<Int>>()
            var exception: Throwable? = null
            val job = launch {
                chunkedFlow
                    .onCompletion {
                        exception = it
                    }
                    .collect { chunk ->
                        actual.add(chunk)
                    }
            }


            advanceTimeBy(chunkDuration.milliseconds * 2)
            job.cancel()
            yield() //wait for onCompletion to be processed

            println(actual)
            assertThat(actual.flatten()).isEqualTo(listOf(1))
            assertThat(exception).isNotInstanceOf(ChunkCancelledWithPendingValuesException::class.java)
            assertThat(exception).isInstanceOf(CancellationException::class.java)
        }

    @Test
    fun `test that chunked emits in flushIfNoValuesReceivedIn when no more values are received `() =
        runTest {
            val chunkDuration = 100
            val flushIfNoValuesReceivedIn = 20
            val expected = listOf(1, 2, 3, 4, 5)
            val flow = expected.asFlow()
                .chunked(chunkDuration.milliseconds, flushIfNoValuesReceivedIn.milliseconds)

            val actual = mutableListOf<List<Int>>()

            flow.test {
                actual.add(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            advanceTimeBy(flushIfNoValuesReceivedIn.milliseconds)

            println(actual)
            assertThat(actual.flatten()).isEqualTo(expected)
        }

    @Test
    internal fun `test that collect chunked emits last chunk if cancelled`() =
        runTest {
            val itemDelay = 5.0
            val chunkDuration = itemDelay * 3.0
            val totalItems = 10
            val emittedValues = mutableListOf<Int>()

            val timeBasedFlow = (1..totalItems).asFlow()
                .onEach {
                    delay(itemDelay.milliseconds)
                    emittedValues.add(it)
                }

            val actual = mutableListOf<List<Int>>()
            val job = launch {
                timeBasedFlow
                    .collectChunked(chunkDuration.milliseconds) { chunk ->
                        actual.add(chunk)
                    }
            }

            advanceTimeBy((chunkDuration + itemDelay * 2).milliseconds)
            job.cancel()
            yield() //wait for onCompletion to be processed

            println(actual)
            assertThat(actual.flatten()).isEqualTo(emittedValues)
        }

    @Test
    internal fun `test that canceled flow during the emission does not emit duplicated last chunk`() =
        runTest {
            val itemsForChunk = 3
            val itemDelay = 5.0
            val chunkDuration = itemDelay * itemsForChunk
            val actual = mutableListOf<List<Int>>()
            val cancelFlagFlow = MutableStateFlow(false)
            val job = launch {
                (1..10).asFlow().onEach {
                    delay(5)
                }.collectChunked(chunkDuration.milliseconds) { chunk ->
                    actual.add(chunk)
                    cancelFlagFlow.value = true
                    delay(100) //simulate a slow collector to receive the cancel while emitting
                }
            }
            launch {
                cancelFlagFlow.collect {
                    if (it) {
                        job.cancel()
                        cancel()
                    }
                }
            }
            advanceTimeBy((chunkDuration * 2).milliseconds)

            println(actual)
            assertThat(actual.flatten()).isEqualTo((1..itemsForChunk).toList())
        }


    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}