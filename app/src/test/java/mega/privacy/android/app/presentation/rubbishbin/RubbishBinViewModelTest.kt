package mega.privacy.android.app.presentation.rubbishbin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class RubbishBinViewModelTest {

    private lateinit var underTest: RubbishBinViewModel

    private val getRubbishBinChildrenNode = mock<GetRubbishBinChildrenNode>()
    private val monitorNodeUpdates = FakeMonitorUpdates()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = RubbishBinViewModel(
            getRubbishBinChildrenNode = getRubbishBinChildrenNode,
            monitorNodeUpdates = monitorNodeUpdates,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            Truth.assertThat(initial.rubbishBinHandle).isEqualTo(-1L)
            Truth.assertThat(initial.nodes).isEmpty()
        }
    }

    @Test
    fun `test that rubbish bin handle is updated if new value provided`() = runTest {
        underTest.state.map { it.rubbishBinHandle }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                Truth.assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setRubbishBinHandle(newValue)
                Truth.assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that rubbish bin node updates live data is set when node updates triggered from use case`() =
        runTest {
            whenever(getRubbishBinChildrenNode(any())).thenReturn(listOf(mock(), mock()))

            runCatching {
                val result =
                    underTest.updateRubbishBinNodes.test().awaitValue(50, TimeUnit.MILLISECONDS)
                monitorNodeUpdates.emit(listOf(mock()))
                result
            }.onSuccess { result ->
                result.assertValue { it.getContentIfNotHandled()?.size == 2 }
            }
        }

    @Test
    fun `test that rubbish bin node updates live data is not set when get rubbish bin node returns a null list`() =
        runTest {
            whenever(getRubbishBinChildrenNode(any())).thenReturn(null)

            runCatching {
                underTest.updateRubbishBinNodes.test().awaitValue(50, TimeUnit.MILLISECONDS)
            }.onSuccess { result ->
                result.assertNoValue()
            }
        }

}
