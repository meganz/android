package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock

@ExtendWith(CoroutineMainDispatcherExtension::class)
class NavigationEventViewModelTest {
    private lateinit var underTest: NavigationEventViewModel

    private lateinit var navigationEventQueueImpl: NavigationEventQueueImpl

    @BeforeEach
    fun setUp() {
        navigationEventQueueImpl = NavigationEventQueueImpl()
        underTest = NavigationEventViewModel(
            navigationEventQueueReceiver = navigationEventQueueImpl
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
        navigationEventQueueImpl.emit(mock())

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }

    @Test
    fun `test that event is updated to consumed when dialogDisplayed is called`() = runTest {
        navigationEventQueueImpl.emit(mock())

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            underTest.eventHandled()
            assertThat(awaitItem()).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that new event is not emitted if previous event is not yet handled`() = runTest {
        navigationEventQueueImpl.emit(mock())
        navigationEventQueueImpl.emit(mock())

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    fun `test that new event is emitted after previous event is handled`() = runTest {
        val navKey1 = mock<NavKey>()
        val navKey2 = mock<NavKey>()
        navigationEventQueueImpl.emit(navKey1)
        navigationEventQueueImpl.emit(navKey2)

        underTest.navigationEvents.test {
            assertThat((awaitItem() as StateEventWithContentTriggered).content).isEqualTo(navKey1)
            underTest.eventHandled()
            assertThat((awaitItem() as StateEventWithContentTriggered).content).isEqualTo(navKey2)
        }
    }

}