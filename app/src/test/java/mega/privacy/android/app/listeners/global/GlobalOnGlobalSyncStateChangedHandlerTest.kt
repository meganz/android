package mega.privacy.android.app.listeners.global

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.notifications.NotifyNotificationCountChangeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalOnGlobalSyncStateChangedHandlerTest {
    private lateinit var underTest: GlobalOnGlobalSyncStateChangedHandler

    private val notifyNotificationCountChangeUseCase = mock<NotifyNotificationCountChangeUseCase>()

    @AfterEach
    fun tearDown() {
        reset(notifyNotificationCountChangeUseCase)
    }

    @Test
    fun `test that notifyNotificationCountChangeUseCase is called when invoked`() = runTest {
        underTest = GlobalOnGlobalSyncStateChangedHandler(
            coroutineScope = backgroundScope,
            notifyNotificationCountChangeUseCase = notifyNotificationCountChangeUseCase,
        )

        underTest.invoke()
        advanceTimeBy(underTest.timeout)

        verify(notifyNotificationCountChangeUseCase).invoke()
    }

    @Test
    fun `test that notifyNotificationCountChangeUseCase is called again if invoked after the debounce timeout`() =
        runTest {
            underTest = GlobalOnGlobalSyncStateChangedHandler(
                coroutineScope = backgroundScope,
                notifyNotificationCountChangeUseCase = notifyNotificationCountChangeUseCase,
            )

            underTest.invoke()
            advanceTimeBy(underTest.timeout + 1.seconds)
            underTest.invoke()
            advanceTimeBy(underTest.timeout)
            verify(notifyNotificationCountChangeUseCase, times(2)).invoke()
        }

    @Test
    fun `test that notifyNotificationCountChangeUseCase is only called once per debounce timeout`() =
        runTest {
            underTest = GlobalOnGlobalSyncStateChangedHandler(
                coroutineScope = backgroundScope,
                notifyNotificationCountChangeUseCase = notifyNotificationCountChangeUseCase,
            )

            underTest.invoke()
            underTest.invoke()
            underTest.invoke()
            underTest.invoke()
            advanceTimeBy(underTest.timeout + 1.seconds)
            underTest.invoke()
            verify(notifyNotificationCountChangeUseCase, times(2)).invoke()
            verifyNoMoreInteractions(notifyNotificationCountChangeUseCase)
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}