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
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetOutgoingSharesChildrenNode
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesViewModel
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class OutgoingSharesViewModelTest {
    private lateinit var underTest: OutgoingSharesViewModel

    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getParentNodeHandle = mock<GetParentNodeHandle>()
    private val getOutgoingSharesChildrenNode = mock<GetOutgoingSharesChildrenNode>()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = OutgoingSharesViewModel(
            getNodeByHandle,
            getParentNodeHandle,
            getOutgoingSharesChildrenNode
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.outgoingParentHandle).isEqualTo(-1L)
            assertThat(initial.outgoingTreeDepth).isEqualTo(0)
            assertThat(initial.nodes).isEmpty()
            assertThat(initial.isInvalidParentHandle).isEqualTo(true)
        }
    }

    @Test
    fun `test that nodes are refreshed at initialization`() = runTest {
        verify(getOutgoingSharesChildrenNode).invoke(-1L)
    }

    @Test
    fun `test that outgoing tree depth is increased when calling increaseOutgoingTreeDepth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

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
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

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
    fun `test that outgoing tree depth is reset to 0 if fails to get node list when calling set outgoing tree depth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            underTest.increaseOutgoingTreeDepth(any())

            underTest.state.map { it.outgoingTreeDepth }
                .test {
                    assertThat(awaitItem()).isEqualTo(1)
                    whenever(getOutgoingSharesChildrenNode(any())).thenReturn(null)
                    underTest.increaseOutgoingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }

    @Test
    fun `test that outgoing tree depth equals 0 if resetOutgoingTreeDepth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.outgoingTreeDepth }.distinctUntilChanged()
                .test {
                    underTest.resetOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }

    @Test
    fun `test that outgoing parent handle is updated when increase outgoing tree depth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

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
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

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
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.outgoingParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    underTest.resetOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that outgoing parent handle is reset to default if fails to get node list when calling set outgoing tree depth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            underTest.increaseOutgoingTreeDepth(123456789L)

            underTest.state.map { it.outgoingParentHandle }
                .test {
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    whenever(getOutgoingSharesChildrenNode(any())).thenReturn(null)
                    underTest.increaseOutgoingTreeDepth(987654321L)
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that is invalid parent handle is set to false when call set outgoing tree depth with valid parent handle`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.increaseOutgoingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(false)
                }
        }

    @Test
    fun `test that is invalid parent handle is set to true when call set outgoing tree depth with invalid parent handle`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(false)
                    underTest.increaseOutgoingTreeDepth(-1L)
                    assertThat(awaitItem()).isEqualTo(true)
                }
        }

    @Test
    fun `test that is invalid parent handle is set to false when cannot retrieve node`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(false)

                    whenever(getNodeByHandle(any())).thenReturn(null)

                    underTest.increaseOutgoingTreeDepth(987654321L)
                    assertThat(awaitItem()).isEqualTo(true)
                }
        }

    @Test
    fun `test that getOutgoingSharesChildrenNode executes when calling increaseOutgoingTreeDepth`() =
        runTest {
            val parentHandle = 123456789L
            underTest.increaseOutgoingTreeDepth(parentHandle)
            verify(getOutgoingSharesChildrenNode).invoke(parentHandle)
        }

    @Test
    fun `test that getOutgoingSharesChildrenNode executes when calling decreaseOutgoingTreeDepth`() =
        runTest {
            val parentHandle = 123456789L
            underTest.decreaseOutgoingTreeDepth(parentHandle)
            verify(getOutgoingSharesChildrenNode).invoke(parentHandle)
        }

    @Test
    fun `test that getOutgoingSharesChildrenNode executes when resetIncomingTreeDepth`() =
        runTest {
            underTest.resetOutgoingTreeDepth()
            // initialization call + subsequent call
            verify(getOutgoingSharesChildrenNode, times(2)).invoke(-1L)
        }

    @Test
    fun `test that getOutgoingSharesChildrenNode executes when refresh`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

            val parentHandle = 123456789L
            val job = underTest.increaseOutgoingTreeDepth(parentHandle)
            job.invokeOnCompletion {
                assertThat(underTest.state.value.outgoingParentHandle).isEqualTo(parentHandle)
                underTest.refreshOutgoingSharesNode()
            }
            // increaseOutgoingTreeDepth call + refreshOutgoingSharesNode call
            verify(getOutgoingSharesChildrenNode, times(2)).invoke(parentHandle)
        }

    @Test
    fun `test that nodes is set with result of getOutgoingSharesChildrenNode if not null`() =
        runTest {
            val node1 = mock<MegaNode>()
            val node2 = mock<MegaNode>()
            val expected = listOf(node1, node2)

            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(expected)

            underTest.state.map { it.nodes }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEmpty()
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that nodes is empty if result of getOutgoingSharesChildrenNode null`() =
        runTest {
            val node1 = mock<MegaNode>()
            val node2 = mock<MegaNode>()
            val expected = listOf(node1, node2)

            whenever(getOutgoingSharesChildrenNode(123456789L)).thenReturn(expected)
            whenever(getOutgoingSharesChildrenNode(987654321L)).thenReturn(null)

            underTest.state.map { it.nodes }.distinctUntilChanged()
                .test {
                    underTest.increaseOutgoingTreeDepth(123456789L).invokeOnCompletion {
                        underTest.increaseOutgoingTreeDepth(987654321L)
                    }
                    assertThat(awaitItem()).isEmpty()
                    assertThat(awaitItem()).isEqualTo(expected)
                    assertThat(awaitItem()).isEmpty()
                }
        }


    @Test
    fun `test that get parent node handle returns the result of get parent node handle use case`() =
        runTest {
            val expected = 123456789L
            whenever(getParentNodeHandle(any())).thenReturn(expected)

            assertThat(underTest.getParentNodeHandle()).isEqualTo(expected)
        }
}
