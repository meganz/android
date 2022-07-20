package test.mega.privacy.android.app.presentation.shares.outgoing

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class OutgoingSharesViewModelTest {
    private lateinit var underTest: OutgoingSharesViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = OutgoingSharesViewModel()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.outgoingParentHandle).isEqualTo(-1L)
            assertThat(initial.outgoingTreeDepth).isEqualTo(0)
        }
    }

    @Test
    fun `test that outgoing tree depth is increased when calling increaseOutgoingTreeDepth`() =
        runTest {
            underTest.state.map { it.outgoingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.increaseOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(1)
                }
        }

    @Test
    fun `test that outgoing tree depth is decreased when calling decreaseOutgoingTreeDepth`() =
        runTest {
            underTest.state.map { it.outgoingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.increaseOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(1)
                    underTest.decreaseOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }

    @Test
    fun `test that outgoing tree depth equals 0 if resetOutgoingTreeDepth`() =
        runTest {
            underTest.state.map { it.outgoingTreeDepth }.distinctUntilChanged()
                .test {
                    underTest.resetOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }

    @Test
    fun `test that outgoing parent handle is updated if new value provided`() =
        runTest {
            underTest.state.map { it.outgoingParentHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.setOutgoingParentHandle(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }
}
