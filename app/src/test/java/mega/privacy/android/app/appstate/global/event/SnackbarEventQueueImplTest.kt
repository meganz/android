package mega.privacy.android.app.appstate.global.event

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SnackbarEventQueueImplTest {

    private lateinit var snackbarEventQueue: SnackbarEventQueueImpl
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun init() {
        snackbarEventQueue = SnackbarEventQueueImpl(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            applicationScope = TestScope(testDispatcher),
            context = mock() // Temporary, will be removed later
        )
    }

    @Test
    fun `test queueMessage with string when single activity is enabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(true)
        val message = "Test message"

        init()
        snackbarEventQueue.queueMessage(message)

        val receivedMessage = snackbarEventQueue.eventQueue.receive()
        assertThat(receivedMessage.message).isEqualTo(message)
    }

    @Test
    fun `test queueMessage with attributes when single activity is enabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(true)
        val attributes = SnackbarAttributes("Test message")

        init()
        snackbarEventQueue.queueMessage(attributes)

        val receivedMessage = snackbarEventQueue.eventQueue.receive()
        assertThat(receivedMessage).isEqualTo(attributes)
    }

    @Test
    fun `test queueMessage when single activity is disabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(false)
        val message = "Test message"

        init()
        snackbarEventQueue.queueMessage(message)

        assertThat(snackbarEventQueue.eventQueue.isEmpty).isTrue()
    }

    @Test
    fun `test queueMessage with attributes when single activity is disabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(false)
        val attributes = SnackbarAttributes("Test message")

        init()
        snackbarEventQueue.queueMessage(attributes)

        assertThat(snackbarEventQueue.eventQueue.isEmpty).isTrue()
    }

    @Test
    fun `test multiple messages are queued in order when feature enabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(true)
        val attributes1 = SnackbarAttributes("First message")
        val attributes2 = SnackbarAttributes("Second message")

        init()
        snackbarEventQueue.queueMessage(attributes1)
        snackbarEventQueue.queueMessage(attributes2)

        assertThat(snackbarEventQueue.eventQueue.receive()).isEqualTo(attributes1)
        assertThat(snackbarEventQueue.eventQueue.receive()).isEqualTo(attributes2)
    }
}
