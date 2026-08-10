package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.QueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.mock

private val scheduler = TestCoroutineScheduler()

@OptIn(ExperimentalCoroutinesApi::class)
class QueueEventViewModelSequenceTest {
    private lateinit var underTest: QueueEventViewModel
    private lateinit var combinedEventQueueImpl: CombinedEventQueueImpl

    private data object TestNavKey1 : NavKey
    private data object TestNavKey2 : NavKey
    private data object TestNavKey3 : NavKey

    @BeforeEach
    fun setUp() {
        combinedEventQueueImpl =
            CombinedEventQueueImpl(scheduler::currentTime, QueueEventComparator())
        underTest = QueueEventViewModel(
            navigationEventQueueReceiver = combinedEventQueueImpl,
        )
    }

    @Test
    fun `test that sequential navigation events are emitted in order`() = runTest {
        combinedEventQueueImpl.emit(TestNavKey1)
        advanceTimeBy(100)
        combinedEventQueueImpl.emit(TestNavKey2)
        advanceTimeBy(100)
        combinedEventQueueImpl.emit(TestNavKey3)

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isEqualTo(consumed())

            val firstTriggered = awaitItem()
            assertWithMessage("First item should be triggered")
                .that(firstTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val firstContent =
                (firstTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat((firstContent as NavigationQueueEvent).keys).containsExactly(TestNavKey1)

            underTest.eventDisplayed()
            val firstConsumed = awaitItem()
            assertThat(firstConsumed).isEqualTo(consumed())

            val secondTriggered = awaitItem()
            assertWithMessage("Second item should be triggered")
                .that(secondTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val secondContent =
                (secondTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat((secondContent as NavigationQueueEvent).keys).containsExactly(TestNavKey2)

            underTest.eventDisplayed()
            val secondConsumed = awaitItem()
            assertThat(secondConsumed).isEqualTo(consumed())

            val thirdTriggered = awaitItem()
            assertWithMessage("Third item should be triggered")
                .that(thirdTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val thirdContent =
                (thirdTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat((thirdContent as NavigationQueueEvent).keys).containsExactly(TestNavKey3)

            underTest.eventDisplayed()
            val thirdConsumed = awaitItem()
            assertThat(thirdConsumed).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that sequential dialog events are emitted in order`() = runTest {
        val dialog1 = mock<AppDialogEvent>()
        val dialog2 = mock<AppDialogEvent>()
        val dialog3 = mock<AppDialogEvent>()
        combinedEventQueueImpl.emit(dialog1)
        advanceTimeBy(100)
        combinedEventQueueImpl.emit(dialog2)
        advanceTimeBy(100)
        combinedEventQueueImpl.emit(dialog3)

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isEqualTo(consumed())

            val firstTriggered = awaitItem()
            assertWithMessage("First item should be triggered")
                .that(firstTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val firstContent =
                (firstTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat(firstContent).isEqualTo(dialog1)

            underTest.eventDisplayed()
            val firstConsumed = awaitItem()
            assertThat(firstConsumed).isEqualTo(consumed())

            underTest.eventHandled()
            val secondTriggered = awaitItem()
            assertWithMessage("Second item should be triggered")
                .that(secondTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val secondContent =
                (secondTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat(secondContent).isEqualTo(dialog2)

            underTest.eventDisplayed()
            val secondConsumed = awaitItem()
            assertThat(secondConsumed).isEqualTo(consumed())

            underTest.eventHandled()
            val thirdTriggered = awaitItem()
            assertWithMessage("Third item should be triggered")
                .that(thirdTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val thirdContent =
                (thirdTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat(thirdContent).isEqualTo(dialog3)

            underTest.eventDisplayed()
            val thirdConsumed = awaitItem()
            assertThat(thirdConsumed).isEqualTo(consumed())


        }
    }

    @Test
    fun `test that mixed navigation and dialog events are emitted in order`() = runTest {
        val dialog1 = AppDialogEvent(TestNavKey1)
        val navKey1 = TestNavKey1
        val dialog2 = AppDialogEvent(TestNavKey2)
        val navKey2 = TestNavKey2
        combinedEventQueueImpl.emit(dialog1)
        advanceTimeBy(100)
        combinedEventQueueImpl.emit(navKey1)
        advanceTimeBy(100)
        combinedEventQueueImpl.emit(dialog2)
        advanceTimeBy(100)
        combinedEventQueueImpl.emit(navKey2)

        underTest.navigationEvents.test {
            // Expect initial consumed state
            assertThat(awaitItem()).isEqualTo(consumed())

            // Emit first navigation event and advance time to process it
            val firstTriggered = awaitItem()
            assertWithMessage("First item should be triggered")
                .that(firstTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val firstContent =
                (firstTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat((firstContent as NavigationQueueEvent).keys).containsExactly(navKey1)

            underTest.eventDisplayed()
            val firstConsumed = awaitItem()
            assertThat(firstConsumed).isEqualTo(consumed())

            val secondTriggered = awaitItem()
            assertWithMessage("Second item should be triggered")
                .that(secondTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val secondContent =
                (secondTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat((secondContent as NavigationQueueEvent).keys).containsExactly(navKey2)

            underTest.eventDisplayed()
            val secondConsumed = awaitItem()
            assertThat(secondConsumed).isEqualTo(consumed())

            val thirdTriggered = awaitItem()
            assertWithMessage("Third item should be triggered")
                .that(thirdTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val thirdContent =
                (thirdTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat(thirdContent).isEqualTo(dialog1)

            underTest.eventDisplayed()
            val thirdConsumed = awaitItem()
            assertThat(thirdConsumed).isEqualTo(consumed())

            underTest.eventHandled()

            val fourthTriggered = awaitItem()
            assertWithMessage("Fourth item should be triggered")
                .that(fourthTriggered)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val fourthContent =
                (fourthTriggered as StateEventWithContentTriggered<QueueEvent>).content
            assertThat(fourthContent).isEqualTo(dialog2)

            underTest.eventDisplayed()
            val fourthConsumed = awaitItem()
            assertThat(fourthConsumed).isEqualTo(consumed())

            underTest.eventHandled()
        }
    }

    @Test
    fun `test that navigation event is emitted after previous navigation event is displayed`() =
        runTest {
            underTest.navigationEvents.test {
                assertThat(awaitItem()).isEqualTo(consumed())

                combinedEventQueueImpl.emit(TestNavKey1)
                val firstItem = awaitItem()
                assertWithMessage("First item should be triggered").that(firstItem)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                val firstContent = (firstItem as StateEventWithContentTriggered<QueueEvent>).content
                assertThat((firstContent as NavigationQueueEvent).keys).containsExactly(TestNavKey1)

                underTest.eventDisplayed()
                val consumed = awaitItem()
                assertThat(consumed).isEqualTo(consumed())

                // Emit second event and advance time to process it
                combinedEventQueueImpl.emit(TestNavKey2)
                val nextItem = awaitItem()
                assertWithMessage("Next item should be triggered").that(nextItem)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                val nextContent = (nextItem as StateEventWithContentTriggered<QueueEvent>).content
                assertThat((nextContent as NavigationQueueEvent).keys).containsExactly(TestNavKey2)
            }
        }

    @Test
    fun `test that dialog event is emitted after previous dialog event is handled`() = runTest {
        val dialog1 = mock<AppDialogEvent>()
        val dialog2 = mock<AppDialogEvent>()

        underTest.navigationEvents.test {
            assertThat(awaitItem()).isEqualTo(consumed())

            combinedEventQueueImpl.emit(dialog1)
            val firstItem = awaitItem()
            assertWithMessage("First item should be triggered").that(firstItem)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val firstContent = (firstItem as StateEventWithContentTriggered<QueueEvent>).content
            assertThat(firstContent).isEqualTo(dialog1)

            underTest.eventDisplayed()
            val consumed = awaitItem()
            assertThat(consumed).isEqualTo(consumed())

            underTest.eventHandled()
            combinedEventQueueImpl.emit(dialog2)
            val nextItem = awaitItem()
            assertWithMessage("Next item should be triggered").that(nextItem)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            val nextContent = (nextItem as StateEventWithContentTriggered<QueueEvent>).content
            assertThat(nextContent).isEqualTo(dialog2)
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(
            StandardTestDispatcher(
                scheduler = scheduler
            )
        )
    }
}

