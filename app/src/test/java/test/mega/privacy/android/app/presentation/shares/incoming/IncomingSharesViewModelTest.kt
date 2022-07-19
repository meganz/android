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
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class IncomingSharesViewModelTest {
    private lateinit var underTest: IncomingSharesViewModel

    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val authorizeNode = mock<AuthorizeNode>()
    private val getIncomingSharesChildrenNode = mock<GetIncomingSharesChildrenNode>()
    private val monitorNodeUpdates = mock<MonitorNodeUpdates>()


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = IncomingSharesViewModel(
//            getNodeByHandle,
//            authorizeNode,
            getIncomingSharesChildrenNode,
//            monitorNodeUpdates,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.incomingParentHandle).isEqualTo(-1L)
            assertThat(initial.incomingTreeDepth).isEqualTo(0)
            assertThat(initial.nodes).isEmpty()
        }
    }

    @Test
    fun `test that getIncomingSharesNode is called at initialization is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.incomingParentHandle).isEqualTo(-1L)
            assertThat(initial.incomingTreeDepth).isEqualTo(0)
            assertThat(initial.nodes).isEmpty()
        }
    }

    @Test
    fun `test that incoming tree depth is increased when calling increaseIncomingTreeDepth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.increaseIncomingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(1)
                }
        }

    @Test
    fun `test that incoming tree depth is decreased when calling decreaseIncomingTreeDepth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.setIncomingTreeDepth(3, any())
                    assertThat(awaitItem()).isEqualTo(3)
                    underTest.decreaseIncomingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(2)
                }
        }

    @Test
    fun `test that incoming tree depth is updated when set incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    val newValue = 1
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.setIncomingTreeDepth(newValue, any())
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming tree depth is reset to 0 if fails to get node list when calling set incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())
            underTest.setIncomingTreeDepth(3, any())

            underTest.state.map { it.incomingTreeDepth }
                .test {
                    assertThat(awaitItem()).isEqualTo(3)
                    whenever(getIncomingSharesChildrenNode(any())).thenReturn(null)
                    underTest.setIncomingTreeDepth(2, any())
                    assertThat(awaitItem()).isEqualTo(0)
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

    @Test
    fun `test that incoming parent handle is updated when increase incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.incomingParentHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseIncomingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming parent handle is updated when decrease incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.incomingParentHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.decreaseIncomingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming parent handle is updated when set incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.incomingParentHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.setIncomingTreeDepth(any(), newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming parent handle is reset to default if fails to get node list when calling set incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())
            underTest.setIncomingTreeDepth(any(), 123456789L)

            underTest.state.map { it.incomingParentHandle }
                .test {
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    whenever(getIncomingSharesChildrenNode(any())).thenReturn(null)
                    underTest.setIncomingTreeDepth(any(), 987654321L)
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that incoming parent handle is set to INVALID_HANDLE when reset incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.incomingParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseIncomingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    underTest.resetIncomingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that getIncomingSharesNode executes when calling increaseIncomingTreeDepth`() =
        runTest {
            val parentHandle = 123456789L
            underTest.increaseIncomingTreeDepth(parentHandle)
            verify(getIncomingSharesChildrenNode).invoke(parentHandle)
        }

    @Test
    fun `test that getIncomingSharesNode executes when calling decreaseIncomingTreeDepth`() =
        runTest {
            val parentHandle = 123456789L
            underTest.decreaseIncomingTreeDepth(parentHandle)
            verify(getIncomingSharesChildrenNode).invoke(parentHandle)
        }

    @Test
    fun `test that getIncomingSharesNode executes when set incoming tree depth`() =
        runTest {
            val parentHandle = 123456789L
            underTest.setIncomingTreeDepth(any(), parentHandle)
            verify(getIncomingSharesChildrenNode).invoke(parentHandle)
        }

    @Test
    fun `test that getIncomingSharesNode executes when resetIncomingTreeDepth`() =
        runTest {
            underTest.resetIncomingTreeDepth()
            // initialization call + subsequent call
            verify(getIncomingSharesChildrenNode, times(2)).invoke(-1L)
        }
}
