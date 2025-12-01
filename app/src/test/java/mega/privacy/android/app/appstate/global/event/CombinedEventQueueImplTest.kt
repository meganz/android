package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CombinedEventQueueImplTest {
    private lateinit var underTest: CombinedEventQueueImpl

    private val timeProvider: () -> Long = mock<() -> Long>()

    private data object TestKey1 : NavKey
    private data class TestKey2(val id: Int) : NavKey

    @BeforeEach
    fun setUp() {
        underTest = CombinedEventQueueImpl(
            getTime = timeProvider,
            queueEventComparator = QueueEventComparator()
        )
    }

    @AfterEach
    fun tearDown() {
        reset(timeProvider)
    }

    @Test
    fun `test that dialog events are received`() = runTest {
        mockTimeProvider()
        val expected = AppDialogEvent(TestKey1)
        underTest.emit(expected)

        val actual = underTest.events.tryReceive().getOrNull()
        assertThat(actual?.invoke()).isEqualTo(expected)
    }

    @Test
    fun `test that navigation events are received`() = runTest {
        mockTimeProvider()
        val key = TestKey1
        val expected = NavigationQueueEvent(listOf(key))
        underTest.emit(key)

        val actual = underTest.events.tryReceive().getOrNull()
        assertThat(actual?.invoke()).isEqualTo(expected)
    }

    @Test
    fun `test that navigation list events are received`() = runTest {
        mockTimeProvider()
        val keys = listOf(TestKey1, TestKey2(0))
        val expected = NavigationQueueEvent(keys)
        underTest.emit(keys)

        val actual = underTest.events.tryReceive().getOrNull()
        assertThat(actual?.invoke()).isEqualTo(expected)
    }

    @Test
    fun `test that events are only consumed by one receiver`() = runTest {
        mockTimeProvider()
        val expectedCount = 10
        repeat(expectedCount) {
            underTest.emit(AppDialogEvent(TestKey2(it)))
        }

        val receivedByCollector1 = mutableListOf<AppDialogEvent>()
        val receivedByCollector2 = mutableListOf<AppDialogEvent>()
        val collectorJob1 = launch {
            (1..expectedCount).forEach { i ->
                // Try to receive up to eventCount items
                underTest.events.tryReceive().getOrNull()?.invoke()?.let {
                    receivedByCollector1.add(it as AppDialogEvent)
                }
            }
        }

        val collectorJob2 = launch {
            (1..expectedCount).forEach { i ->
                underTest.events.tryReceive().getOrNull()?.invoke()?.let {
                    receivedByCollector2.add(it as AppDialogEvent)
                }
            }
        }

        collectorJob1.join()
        collectorJob2.join()

        val totalReceivedCount = receivedByCollector1.size + receivedByCollector2.size
        assertThat(totalReceivedCount).isEqualTo(expectedCount)

    }

    @Test
    fun `test that the same events are ordered by time`() = runTest {
        mockTimeProvider(100, 200, 300)
        val expectedFirst = AppDialogEvent(TestKey2(1))
        val expectedSecond = AppDialogEvent(TestKey2(2))
        val expectedThird = AppDialogEvent(TestKey2(3))

        underTest.emit(expectedFirst)
        underTest.emit(expectedSecond)
        underTest.emit(expectedThird)

        underTest.events.receiveAsFlow().map { it.invoke() }.test {
            assertThat(awaitItem()).isEqualTo(expectedFirst)
            assertThat(awaitItem()).isEqualTo(expectedSecond)
            assertThat(awaitItem()).isEqualTo(expectedThird)
        }
    }

    @Test
    fun `test that priority overrides time ordering`() = runTest {
        mockTimeProvider(100, 200, 300)
        val expectedFirst = AppDialogEvent(TestKey2(1))
        val expectedSecond = AppDialogEvent(TestKey2(2))
        val expectedThird = AppDialogEvent(TestKey2(3))

        underTest.emit(expectedThird, NavPriority.Priority(1))
        underTest.emit(expectedSecond, NavPriority.Priority(2))
        underTest.emit(expectedFirst, NavPriority.Priority(3))

        underTest.events.receiveAsFlow().map { it.invoke() }.test {
            assertThat(awaitItem()).isEqualTo(expectedFirst)
            assertThat(awaitItem()).isEqualTo(expectedSecond)
            assertThat(awaitItem()).isEqualTo(expectedThird)
        }
    }

    @Test
    fun `test that navigation events are prioritised over dialog events`() = runTest {
        mockTimeProvider(100, 200)

        val keys = listOf(TestKey1)
        val expectedFirst = NavigationQueueEvent(keys)
        val expectedSecond = AppDialogEvent(TestKey2(2))


        underTest.emit(expectedSecond, NavPriority.Priority(100))
        underTest.emit(keys, NavPriority.Default)

        underTest.events.receiveAsFlow().map { it.invoke() }.test {
            assertThat(awaitItem()).isEqualTo(expectedFirst)
            assertThat(awaitItem()).isEqualTo(expectedSecond)
        }

    }

    private fun mockTimeProvider(firstTime: Long = 0L, vararg times: Long = longArrayOf(0L)) {
        whenever(timeProvider()).thenReturn(firstTime, *times.toTypedArray())
    }
}