package mega.privacy.android.app.appstate.global.event

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SnackbarEventQueueImplTest {

    private lateinit var snackbarEventQueue: SnackbarEventQueueImpl

    private fun init() {
        snackbarEventQueue = SnackbarEventQueueImpl(
            context = mock() // Temporary, will be removed later
        )
    }

    @Test
    fun `test that queueMessage with string enqueues the message`() = runTest {
        val message = "Test message"

        init()
        snackbarEventQueue.queueMessage(message)

        val receivedMessage = snackbarEventQueue.eventQueue.receive()
        assertThat(receivedMessage.message).isEqualTo(message)
    }

    @Test
    fun `test that queueMessage with attributes enqueues the message`() = runTest {
        val attributes = SnackbarAttributes("Test message")

        init()
        snackbarEventQueue.queueMessage(attributes)

        val receivedMessage = snackbarEventQueue.eventQueue.receive()
        assertThat(receivedMessage).isEqualTo(attributes)
    }

    @Test
    fun `test that multiple messages are queued in order`() = runTest {
        val attributes1 = SnackbarAttributes("First message")
        val attributes2 = SnackbarAttributes("Second message")

        init()
        snackbarEventQueue.queueMessage(attributes1)
        snackbarEventQueue.queueMessage(attributes2)

        assertThat(snackbarEventQueue.eventQueue.receive()).isEqualTo(attributes1)
        assertThat(snackbarEventQueue.eventQueue.receive()).isEqualTo(attributes2)
    }
}
