package test.mega.privacy.android.app.presentation.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.search.SearchViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class SearchViewModelTest {

    private lateinit var underTest: SearchViewModel

    private val monitorNodeUpdates = mock<MonitorNodeUpdates>()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /**
     * Initialize the view model under test
     */
    private fun setUnderTest() {
        underTest = SearchViewModel(
            monitorNodeUpdates,
        )
    }

    @Test
    fun `test that node updates live data is not set when no updates triggered from use case`() =
        runTest {
            setUnderTest()

            underTest.updateNodes.test().assertNoValue()
        }

    @Test
    fun `test that node updates live data is set when node updates triggered from use case`() =
        runTest {
            whenever(monitorNodeUpdates()).thenReturn(flowOf(listOf(mock())))

            setUnderTest()

            runCatching {
                underTest.updateNodes.test().awaitValue(50, TimeUnit.MILLISECONDS)
            }.onSuccess { result ->
                result.assertValue { it.getContentIfNotHandled()?.size == 1 }
            }
        }

}