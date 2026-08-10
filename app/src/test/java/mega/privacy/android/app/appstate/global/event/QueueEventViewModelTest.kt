package mega.privacy.android.app.appstate.global.event

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock

@ExtendWith(CoroutineMainDispatcherExtension::class)
class QueueEventViewModelTest {
    private lateinit var underTest: QueueEventViewModel
    private lateinit var combinedEventQueueImpl: CombinedEventQueueImpl

    @BeforeEach
    fun setUp() {
        combinedEventQueueImpl =
            CombinedEventQueueImpl(System::currentTimeMillis, QueueEventComparator())
        underTest = QueueEventViewModel(
            navigationEventQueueReceiver = combinedEventQueueImpl,
        )
    }

    @Test
    fun `test that initial state is consumed`() = runTest {
        underTest.navigationEvents.test {
            assertThat(awaitItem()).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that triggered event is received when new event enters the queue`() = runTest {
        combinedEventQueueImpl.emit(mock<AppDialogEvent>())

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }

    @Test
    fun `test that event is updated to consumed when dialogDisplayed is called`() = runTest {
        combinedEventQueueImpl.emit(mock<AppDialogEvent>())

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            underTest.eventDisplayed()
            assertThat(awaitItem()).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that new event is not emitted if previous event is not yet handled`() = runTest {
        combinedEventQueueImpl.emit(mock<AppDialogEvent>())
        combinedEventQueueImpl.emit(mock<AppDialogEvent>())

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            underTest.eventDisplayed()
            assertThat(awaitItem()).isEqualTo(consumed())
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    fun `test that new event is emitted after previous event is handled`() = runTest {
        combinedEventQueueImpl.emit(mock<AppDialogEvent>())
        combinedEventQueueImpl.emit(mock<AppDialogEvent>())

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            underTest.eventDisplayed()
            assertThat(awaitItem()).isEqualTo(consumed())
            underTest.eventHandled()
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }

    @Test
    fun `test that triggered event is received when new navigation event enters the queue`() =
        runTest {
            combinedEventQueueImpl.emit(listOf(mock()))

            underTest.navigationEvents.test {
                assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            }
        }

    @Test
    fun `test that event is updated to consumed when eventDisplayed is called`() = runTest {
        combinedEventQueueImpl.emit(listOf(mock()))

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            underTest.eventDisplayed()
            assertThat(awaitItem()).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that new event is not emitted if previous event is not yet displayed`() = runTest {
        combinedEventQueueImpl.emit(listOf(mock()))
        combinedEventQueueImpl.emit(listOf(mock()))

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

}