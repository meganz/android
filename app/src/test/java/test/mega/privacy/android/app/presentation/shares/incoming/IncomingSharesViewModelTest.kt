package test.mega.privacy.android.app.presentation.shares.incoming

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
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class IncomingSharesViewModelTest {
    private lateinit var underTest: IncomingSharesViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = IncomingSharesViewModel()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.incomingParentHandle).isEqualTo(-1L)
            assertThat(initial.incomingTreeDepth).isEqualTo(0)
        }
    }

    @Test
    fun `test that incoming parent handle is updated if new value provided`() = runTest {
        underTest.state.map { it.incomingParentHandle }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setIncomingParentHandle(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that incoming tree depth is increased when calling increaseIncomingTreeDepth`() =
        runTest {
            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.increaseIncomingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(1)
                }
        }

    @Test
    fun `test that incoming tree depth is decreased when calling decreaseIncomingTreeDepth`() =
        runTest {
            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.setIncomingTreeDepth(3)
                    assertThat(awaitItem()).isEqualTo(3)
                    underTest.decreaseIncomingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(2)
                }
        }

    @Test
    fun `test that incoming tree depth is updated if new value provided`() =
        runTest {
            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    val newValue = 1
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.setIncomingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming tree depth equals 0 if resetIncomingTreeDepth`() =
        runTest {
            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    underTest.resetIncomingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }
}
