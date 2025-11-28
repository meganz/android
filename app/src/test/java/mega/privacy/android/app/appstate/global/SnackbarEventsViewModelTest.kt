package mega.privacy.android.app.appstate.global

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueueReceiver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SnackbarEventsViewModelTest {

    private lateinit var underTest: SnackbarEventsViewModel
    private val snackbarEventQueueReceiver: SnackbarEventQueueReceiver = mock()
    private val testChannel = Channel<SnackbarAttributes>(Channel.UNLIMITED)

    private fun init() {
        whenever(snackbarEventQueueReceiver.eventQueue).thenReturn(testChannel)

        underTest = SnackbarEventsViewModel(snackbarEventQueueReceiver)
    }

    @Test
    fun `test event is triggered when message is queued`() = runTest {
        init()

        val attributes = SnackbarAttributes("Test message")
        testChannel.send(attributes)

        underTest.snackbarEventState.test {
            assertThat(awaitItem()).isEqualTo(triggered(attributes))
        }
    }

    @Test
    fun `test multiple events are processed in order`() = runTest {
        init()

        val attributes1 = SnackbarAttributes("First message")
        val attributes2 = SnackbarAttributes("Second message")

        underTest.snackbarEventState.test {
            assertThat(awaitItem()).isEqualTo(consumed())

            // Send two events simultaneously
            testChannel.send(attributes1)
            testChannel.send(attributes2)

            // First event should be triggered
            val eventState1 = awaitItem()
            assertThat(eventState1).isEqualTo(triggered(attributes1))

            underTest.consumeEvent()

            // Second event should be triggered after consuming the first
            assertThat(awaitItem()).isEqualTo(triggered(attributes2))

            // After consuming the second, state should be consumed
            underTest.consumeEvent()
            assertThat(awaitItem()).isEqualTo(consumed())
        }
    }

    @Test
    fun `test consumeEvent resets state to consumed`() = runTest {
        init()

        val attributes = SnackbarAttributes("Test message")

        underTest.snackbarEventState.test {
            assertThat(awaitItem()).isEqualTo(consumed())

            testChannel.send(attributes)
            assertThat(awaitItem()).isEqualTo(triggered(attributes))

            underTest.consumeEvent()
            assertThat(awaitItem()).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that next event should suspend when waiting for consumption`() = runTest {
        init()

        val attributes1 = SnackbarAttributes("First message")
        val attributes2 = SnackbarAttributes("Second message")

        underTest.snackbarEventState.test {
            testChannel.send(attributes1)
            testChannel.send(attributes2)

            assertThat(awaitItem()).isEqualTo(consumed())

            // First event should be triggered
            val eventState1 = awaitItem()
            assertThat(eventState1).isEqualTo(triggered(attributes1))

            // Not consuming the first event yet, so the second should not be processed
            expectNoEvents()

            underTest.consumeEvent()

            advanceUntilIdle()

            val eventState2 = awaitItem()
            assertThat(eventState2).isEqualTo(triggered(attributes2))

            underTest.consumeEvent()
            assertThat(awaitItem()).isEqualTo(consumed())

        }
    }
}

