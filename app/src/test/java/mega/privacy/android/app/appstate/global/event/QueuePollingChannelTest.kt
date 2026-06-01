package mega.privacy.android.app.appstate.global.event

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.PriorityQueue

@ExtendWith(CoroutineMainDispatcherExtension::class)
class QueuePollingChannelTest {
    private lateinit var underTest: QueuePollingChannel<Int, Int>

    @BeforeEach
    fun setUp() {
        underTest = QueuePollingChannel(
            queue = PriorityQueue(),
            mapper = { it }
        )
    }

    @Test
    fun `test that no events are emitted if the queue is empty`() = runTest {
        underTest.events.consumeAsFlow().test {
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    fun `test that an event is emitted if item is added to the queue`() = runTest {
        val expected = 1
        underTest.events.consumeAsFlow().test {
            underTest.add(expected)
            assertThat(awaitItem()()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that all items create events`() = runTest {
        val expected = listOf(1, 2, 3)
        underTest.events.consumeAsFlow().test {
            expected.forEach {
                underTest.add(it)
            }
            expected.forEach {
                assertThat(awaitItem()()).isEqualTo(it)
            }
        }
    }

    @Test
    fun `test that items from priority queue are emitted in priority order - smallest to biggest`() =
        runTest {
            val expected = listOf(1, 2, 3)

            expected.reversed().forEach {
                underTest.add(it)
            }
            underTest.events.consumeAsFlow().test {
                expected.forEach {
                    assertThat(awaitItem()()).isEqualTo(it)
                }
            }
        }

    @Test
    fun `test that duplicates are ignored`() = runTest {
        val expected = listOf(1, 2, 3)
        underTest.events.consumeAsFlow().test {
            expected.forEach {
                underTest.add(it)
            }
            underTest.add(3)
            expected.forEach {
                assertThat(awaitItem()()).isEqualTo(it)
            }
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    fun `test that concurrent add and poll on a comparator-backed queue do not crash`() = runTest {
        val channel = QueuePollingChannel(
            queue = PriorityQueue<Int>(compareBy { it }),
            mapper = { it }
        )
        val itemCount = 2_000
        val producers = 8

        val drained = mutableListOf<Int>()
        val drainJob = launch {
            for (event in channel.events) {
                event()?.let { drained += it }
                if (drained.size >= itemCount) break
            }
        }

        withContext(Dispatchers.Default) {
            (0 until producers).map { producerIndex ->
                async {
                    val start = producerIndex * (itemCount / producers)
                    val end = start + (itemCount / producers)
                    for (item in start until end) {
                        channel.add(item)
                    }
                }
            }.awaitAll()
        }

        drainJob.join()

        assertThat(drained).hasSize(itemCount)
        assertThat(drained.toSet()).isEqualTo((0 until itemCount).toSet())
    }

}