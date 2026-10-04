package mega.privacy.android.app.appstate.global.lifecycle

import android.content.ComponentCallbacks2
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.logging.FlushLogsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppForegroundLifecycleObserverTest {

    private lateinit var underTest: AppForegroundLifecycleObserver
    private lateinit var applicationScope: CoroutineScope

    private val flushLogsUseCase = mock<FlushLogsUseCase>()

    @BeforeEach
    fun setUp() {
        reset(flushLogsUseCase)
        applicationScope = CoroutineScope(UnconfinedTestDispatcher())
        underTest = AppForegroundLifecycleObserver(
            flushLogsUseCase = flushLogsUseCase,
            applicationScope = applicationScope,
        )
    }

    @AfterEach
    fun tearDown() {
        applicationScope.cancel()
    }

    @Test
    fun `test that onStop flushes the logs`() = runTest {
        underTest.onStop(mock<LifecycleOwner>())

        verify(flushLogsUseCase).invoke()
    }

    @Test
    fun `test that onTrimMemory flushes the logs when the ui is hidden`() = runTest {
        underTest.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

        verify(flushLogsUseCase).invoke()
    }

    @Test
    fun `test that onTrimMemory does not flush the logs below the ui hidden threshold`() = runTest {
        underTest.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)

        verifyNoInteractions(flushLogsUseCase)
    }

    @Test
    fun `test that onStop does not propagate the failure when the flush throws`() = runTest {
        whenever(flushLogsUseCase()).thenThrow(RuntimeException("disk full"))

        underTest.onStop(mock<LifecycleOwner>())

        verify(flushLogsUseCase).invoke()
    }
}
