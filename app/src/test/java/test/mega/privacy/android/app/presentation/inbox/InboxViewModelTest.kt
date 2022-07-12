package test.mega.privacy.android.app.presentation.inbox

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.inbox.InboxViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

/**
 * Test class for [InboxViewModel]
 */
@ExperimentalCoroutinesApi
class InboxViewModelTest {
    private lateinit var underTest: InboxViewModel

    private val monitorNodeUpdates = mock<MonitorNodeUpdates>()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /**
     * This initializes the [InboxViewModel]
     */
    private fun setupUnderTest() {
        underTest = InboxViewModel(
            monitorNodeUpdates = monitorNodeUpdates,
        )
    }

    @Test
    fun `test that update nodes live data is not set when there are no updates triggered from use case`() =
        runTest {
            setupUnderTest()

            underTest.updateNodes.test().assertNoValue()
        }

    @Test
    fun `test that update nodes live data is set when there are updates triggered from use case`() =
        runTest {
            whenever(monitorNodeUpdates()).thenReturn(flowOf(listOf(mock())))

            setupUnderTest()

            runCatching {
                underTest.updateNodes.test().awaitValue(50, TimeUnit.MILLISECONDS)
            }.onSuccess { result ->
                result.assertValue { it.getContentIfNotHandled()?.size == 1 }
            }
        }
}