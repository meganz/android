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
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.GetUnverifiedOutgoingShares
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

@ExperimentalCoroutinesApi
class OutgoingSharesViewModelTest {
    private lateinit var underTest: OutgoingSharesViewModel

    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getParentNodeHandle = mock<GetParentNodeHandle>()
    private val getOutgoingSharesChildrenNode = mock<GetOutgoingSharesChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_ASC)
    }
    private val getOtherSortOrder = mock<GetOthersSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_DESC)
    }
    private val monitorNodeUpdates = FakeMonitorUpdates()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val getUnverifiedOutgoingShares = mock<GetUnverifiedOutgoingShares> {
        val shareData = ShareData("user", 8766L, 0, 987654678L, true, false)
        onBlocking { invoke(any()) }.thenReturn(listOf(shareData))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = OutgoingSharesViewModel(
            getNodeByHandle,
            getParentNodeHandle,
            getOutgoingSharesChildrenNode,
            getCloudSortOrder,
            getOtherSortOrder,
            monitorNodeUpdates,
            getUnverifiedOutgoingShares,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.outgoingHandle).isEqualTo(-1L)
            assertThat(initial.outgoingTreeDepth).isEqualTo(0)
            assertThat(initial.nodes).isEmpty()
            assertThat(initial.isInvalidHandle).isEqualTo(true)
            assertThat(initial.outgoingParentHandle).isEqualTo(null)
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
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
    fun `test that outgoing handle is updated when increase outgoing tree depth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.outgoingHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseOutgoingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that outgoing handle is updated when decrease outgoing tree depth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.outgoingHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.decreaseOutgoingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that outgoing handle is set to -1L when reset outgoing tree depth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.outgoingHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    underTest.resetOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that outgoing handle is reset to default if fails to get node list when calling set outgoing tree depth`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            underTest.increaseOutgoingTreeDepth(123456789L)

            underTest.state.map { it.outgoingHandle }
                .test {
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    whenever(getOutgoingSharesChildrenNode(any())).thenReturn(null)
                    underTest.increaseOutgoingTreeDepth(987654321L)
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that is invalid handle is set to false when call set outgoing tree depth with valid handle`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.increaseOutgoingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(false)
                }
        }

    @Test
    fun `test that is invalid handle is set to true when call set outgoing tree depth with invalid handle`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(false)
                    underTest.increaseOutgoingTreeDepth(-1L)
                    assertThat(awaitItem()).isEqualTo(true)
                }
        }

    @Test
    fun `test that is invalid handle is set to true when cannot retrieve node`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
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
            val handle = 123456789L
            underTest.increaseOutgoingTreeDepth(handle)
            verify(getOutgoingSharesChildrenNode).invoke(handle)
        }

    @Test
    fun `test that getOutgoingSharesChildrenNode executes when calling decreaseOutgoingTreeDepth`() =
        runTest {
            val handle = 123456789L
            underTest.decreaseOutgoingTreeDepth(handle)
            verify(getOutgoingSharesChildrenNode).invoke(handle)
        }

    @Test
    fun `test that getOutgoingSharesChildrenNode executes when resetOutgoingTreeDepth`() =
        runTest {
            underTest.resetOutgoingTreeDepth()
            // initialization call + subsequent call
            verify(getOutgoingSharesChildrenNode, times(2)).invoke(-1L)
        }

    @Test
    fun `test that getOutgoingSharesChildrenNode executes when refresh`() =
        runTest {
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())

            val handle = 123456789L
            val job = underTest.increaseOutgoingTreeDepth(handle)
            job.invokeOnCompletion {
                assertThat(underTest.state.value.outgoingHandle).isEqualTo(handle)
                underTest.refreshOutgoingSharesNode()
            }
            // increaseOutgoingTreeDepth call + refreshOutgoingSharesNode call
            verify(getOutgoingSharesChildrenNode, times(2)).invoke(handle)
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
    fun `test that getParentNodeHandle is called when setOutgoingTreeDepth`() =
        runTest {
            val handle = 123456789L
            underTest.increaseOutgoingTreeDepth(handle)
            verify(getParentNodeHandle).invoke(handle)
        }

    @Test
    fun `test that parent handle is set with result of getParentNodeHandle`() =
        runTest {
            val expected = 111111111L
            whenever(getParentNodeHandle(any())).thenReturn(expected)
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.outgoingParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(null)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that parent handle is set to null when refreshNodes fails`() =
        runTest {
            whenever(getParentNodeHandle(any())).thenReturn(111111111L)
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.outgoingParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(null)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(111111111L)
                    whenever(getOutgoingSharesChildrenNode(any())).thenReturn(null)
                    underTest.increaseOutgoingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(null)
                }
        }

    @Test
    fun `test that refresh nodes is called when receiving a node update`() = runTest {
        val node = mock<Node> {
            on { this.id }.thenReturn(NodeId(987654321L))
        }
        monitorNodeUpdates.emit(NodeUpdate(emptyMap()))
        // initialization call + receiving a node update call
        verify(
            getOutgoingSharesChildrenNode,
            times(2)
        ).invoke(underTest.state.value.outgoingHandle)
    }

    @Test
    fun `test that sort order is set with result of getOthersSortOrder if depth is equals to 0 when call setIncomingTreeDepth`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getOtherSortOrder()).thenReturn(expected)

            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.resetOutgoingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that sort order is set with result of getCloudSortOrder if depth is different than 0 when call setIncomingTreeDepth`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getCloudSortOrder()).thenReturn(expected)

            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.increaseOutgoingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that sort order is set with result of getOtherSortOrder when refreshNodes fails`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getOutgoingSharesChildrenNode(any())).thenReturn(null)
            whenever(getOtherSortOrder()).thenReturn(expected)

            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.increaseOutgoingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that unverified outgoing shares are returned`() = runTest {
        val node1 = mock<MegaNode>()
        whenever(getNodeByHandle(any())).thenReturn(node1)
        assertThat(getNodeByHandle(any())).isNotNull()
        initViewModel()
        underTest.state.map { it.unverifiedOutgoingShares }.distinctUntilChanged()
            .test {
                assertThat(awaitItem().size).isEqualTo(1)
            }
    }
}
