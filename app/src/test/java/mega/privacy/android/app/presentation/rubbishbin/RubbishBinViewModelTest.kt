package mega.privacy.android.app.presentation.rubbishbin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

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
    fun `test that on setting rubbish bin handle rubbish bin node returns empty list`() =
        runTest {
            val newValue = 123456789L
            whenever(getRubbishBinChildrenNode.invoke(newValue)).thenReturn(ArrayList())
            monitorNodeUpdates.emit(listOf())
            underTest.setRubbishBinHandle(newValue)
            Truth.assertThat(underTest.state.value.nodes.size).isEqualTo(0)
        }

    @Test
    fun `test that on setting rubbish bin handle rubbish bin node returns some items in list`() =
        runTest {
            val newValue = 123456789L
            whenever(getRubbishBinChildrenNode.invoke(newValue)).thenReturn(listOf(mock(),
                mock()))
            monitorNodeUpdates.emit(listOf(mock(), mock()))
            underTest.setRubbishBinHandle(newValue)
            Truth.assertThat(underTest.state.value.nodes.size).isEqualTo(2)
        }

    @Test
    fun `test that on setting rubbish bin handle rubbish bin node returns null`() = runTest {
        val newValue = 123456789L
        whenever(getRubbishBinChildrenNode.invoke(newValue)).thenReturn(null)
        underTest.setRubbishBinHandle(newValue)
        Truth.assertThat(underTest.state.value.nodes.size).isEqualTo(0)
    }
}
