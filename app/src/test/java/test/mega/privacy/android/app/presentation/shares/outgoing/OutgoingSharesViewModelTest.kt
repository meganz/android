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
import org.mockito.kotlin.any

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
                    underTest.increaseOutgoingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(1)
                }
        }

    @Test
    fun `test that outgoing tree depth is decreased when calling decreaseOutgoingTreeDepth`() =
        runTest {
            underTest.state.map { it.outgoingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.increaseOutgoingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(1)
                    underTest.decreaseOutgoingTreeDepth(any())
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
    fun `test that outgoing parent handle is updated when increase outgoing tree depth`() =
        runTest {
            underTest.state.map { it.outgoingParentHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseOutgoingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that outgoing parent handle is updated when decrease outgoing tree depth`() =
        runTest {
            underTest.state.map { it.outgoingParentHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.decreaseOutgoingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that outgoing parent handle is set to -1L when reset outgoing tree depth`() =
        runTest {
            underTest.state.map { it.outgoingParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    underTest.resetOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }
}
