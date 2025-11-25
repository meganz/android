package mega.privacy.android.app.appstate.global.event

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class AppDialogsEventQueueImplTest {
    private val underTest = AppDialogsEventQueueImpl()

    @Test
    fun `test that events are received`() = runTest {
        val expected = mock<AppDialogEvent>()
        underTest.emit(expected)

        val actual = underTest.events.tryReceive().getOrNull()
        assertThat(actual?.invoke()).isEqualTo(expected)
    }

    @Test
    fun `test that events are only consumed by one receiver`() = runTest {
        val expectedCount = 10
        repeat(expectedCount) {
            underTest.emit(mock<AppDialogEvent>())
        }

        val receivedByCollector1 = mutableListOf<AppDialogEvent>()
        val receivedByCollector2 = mutableListOf<AppDialogEvent>()
        val collectorJob1 = launch {
            for (i in 1..expectedCount) { // Try to receive up to eventCount items
                underTest.events.tryReceive().getOrNull()?.invoke()?.let {
                    receivedByCollector1.add(it as AppDialogEvent)
                }
            }
        }

        val collectorJob2 = launch {
            for (i in 1..expectedCount) {
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
}