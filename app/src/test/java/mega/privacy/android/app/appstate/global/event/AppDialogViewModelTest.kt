package mega.privacy.android.app.appstate.global.event

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppDialogViewModelTest {
    private lateinit var underTest: AppDialogViewModel
    private lateinit var appDialogsEventQueue: AppDialogsEventQueueImpl

    @BeforeEach
    fun setUp() {
        appDialogsEventQueue = AppDialogsEventQueueImpl()
        underTest = AppDialogViewModel(
            appDialogsEventQueueReceiver = appDialogsEventQueue,
        )
    }

    @Test
    fun `test that initial state is consumed`() = runTest {
        underTest.dialogEvents.test {
            assertThat(awaitItem()).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that triggered event is received when new event enters the queue`() = runTest {
        appDialogsEventQueue.emit(mock())

        underTest.dialogEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }

    @Test
    fun `test that event is updated to consumed when dialogDisplayed is called`() = runTest {
        appDialogsEventQueue.emit(mock())

        underTest.dialogEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            underTest.dialogDisplayed()
            assertThat(awaitItem()).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that new event is not emitted if previous event is not yet handled`() = runTest {
        appDialogsEventQueue.emit(mock())
        appDialogsEventQueue.emit(mock())

        underTest.dialogEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            underTest.dialogDisplayed()
            assertThat(awaitItem()).isEqualTo(consumed())
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    fun `test that new event is emitted after previous event is handled`() = runTest {
        appDialogsEventQueue.emit(mock())
        appDialogsEventQueue.emit(mock())

        underTest.dialogEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            underTest.dialogDisplayed()
            assertThat(awaitItem()).isEqualTo(consumed())
            underTest.eventHandled()
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }
}