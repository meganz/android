package test.mega.privacy.android.app.presentation.shares.links

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
import mega.privacy.android.app.domain.usecase.GetPublicLinks
import mega.privacy.android.app.presentation.shares.links.LinksViewModel
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
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
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates

@ExperimentalCoroutinesApi
class LinksViewModelTest {
    private lateinit var underTest: LinksViewModel

    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getParentNodeHandle = mock<GetParentNodeHandle>()
    private val getPublicLinks = mock<GetPublicLinks>()
    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_ASC)
    }
    private val getLinksSortOrder = mock<GetLinksSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_DESC)
    }
    private val monitorNodeUpdates = FakeMonitorUpdates()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = LinksViewModel(
            getNodeByHandle,
            getParentNodeHandle,
            getPublicLinks,
            getCloudSortOrder,
            getLinksSortOrder,
            monitorNodeUpdates,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.linksHandle).isEqualTo(-1L)
            assertThat(initial.linksTreeDepth).isEqualTo(0)
            assertThat(initial.linksParentHandle).isEqualTo(null)
            assertThat(initial.nodes).isEmpty()
            assertThat(initial.isLoading).isFalse()
        }
    }

    @Test
    fun `test that nodes are refreshed at initialization`() = runTest {
        verify(getPublicLinks).invoke(-1L)
    }

    @Test
    fun `test that links tree depth is increased when calling increaseLinksTreeDepth`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())

            underTest.state.map { it.linksTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.increaseLinksTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(1)
                }
        }

    @Test
    fun `test that links tree depth is decreased when calling decreaseLinksTreeDepth`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())

            underTest.state.map { it.linksTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.increaseLinksTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(1)
                    underTest.decreaseLinksTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }

    @Test
    fun `test that links tree depth is reset to 0 if fails to get public links when calling set links tree depth`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())
            underTest.increaseLinksTreeDepth(any())

            underTest.state.map { it.linksTreeDepth }
                .test {
                    assertThat(awaitItem()).isEqualTo(1)
                    whenever(getPublicLinks(any())).thenReturn(null)
                    underTest.increaseLinksTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }

    @Test
    fun `test that links tree depth equals 0 if resetLinksTreeDepth`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())

            underTest.state.map { it.linksTreeDepth }.distinctUntilChanged()
                .test {
                    underTest.resetLinksTreeDepth()
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }

    @Test
    fun `test that links handle is updated when increase links tree depth`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())

            underTest.state.map { it.linksHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseLinksTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that links handle is updated when decrease links tree depth`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())

            underTest.state.map { it.linksHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.decreaseLinksTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that links handle is set to -1L when reset links tree depth`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())

            underTest.state.map { it.linksHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseLinksTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    underTest.resetLinksTreeDepth()
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that links handle is reset to default if fails to get public links when calling set links tree depth`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())
            underTest.increaseLinksTreeDepth(123456789L)

            underTest.state.map { it.linksHandle }
                .test {
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    whenever(getPublicLinks(any())).thenReturn(null)
                    underTest.increaseLinksTreeDepth(987654321L)
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that getPublicLinks executes when calling increaseLinksTreeDepth`() =
        runTest {
            val handle = 123456789L
            underTest.increaseLinksTreeDepth(handle)
            verify(getPublicLinks).invoke(handle)
        }

    @Test
    fun `test that getPublicLinks executes when calling decreaseLinksTreeDepth`() =
        runTest {
            val handle = 123456789L
            underTest.decreaseLinksTreeDepth(handle)
            verify(getPublicLinks).invoke(handle)
        }

    @Test
    fun `test that getPublicLinks executes when resetLinksTreeDepth`() =
        runTest {
            underTest.refreshLinksSharesNode()
            // initialization call + subsequent call
            verify(getPublicLinks, times(2)).invoke(-1L)
        }

    @Test
    fun `test that getPublicLinks executes when refresh`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())

            val handle = 123456789L
            val job = underTest.increaseLinksTreeDepth(handle)
            job.invokeOnCompletion {
                assertThat(underTest.state.value.linksHandle).isEqualTo(handle)
                underTest.refreshLinksSharesNode()
            }
            // increaseLinksTreeDepth call + refreshLinksSharesNode call
            verify(getPublicLinks, times(2)).invoke(handle)
        }

    @Test
    fun `test that nodes is set with result of getPublicLinks if not null`() =
        runTest {
            val node1 = mock<MegaNode>()
            val node2 = mock<MegaNode>()
            val expected = listOf(node1, node2)

            whenever(getPublicLinks(any())).thenReturn(expected)

            underTest.state.map { it.nodes }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEmpty()
                    underTest.increaseLinksTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that nodes is empty if result of getPublicLinks null`() =
        runTest {
            val node1 = mock<MegaNode>()
            val node2 = mock<MegaNode>()
            val expected = listOf(node1, node2)

            whenever(getPublicLinks(123456789L)).thenReturn(expected)
            whenever(getPublicLinks(987654321L)).thenReturn(null)

            underTest.state.map { it.nodes }.distinctUntilChanged()
                .test {
                    underTest.increaseLinksTreeDepth(123456789L).invokeOnCompletion {
                        underTest.increaseLinksTreeDepth(987654321L)
                    }
                    assertThat(awaitItem()).isEmpty()
                    assertThat(awaitItem()).isEqualTo(expected)
                    assertThat(awaitItem()).isEmpty()
                }
        }

    @Test
    fun `test that is invalid parent handle is set to false when call set links tree depth with valid parent handle`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.increaseLinksTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(false)
                }
        }

    @Test
    fun `test that is invalid handle is set to true when call set links tree depth with invalid handle`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.increaseLinksTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(false)
                    underTest.increaseLinksTreeDepth(-1L)
                    assertThat(awaitItem()).isEqualTo(true)
                }
        }

    @Test
    fun `test that is invalid handle is set to true when cannot retrieve node`() =
        runTest {
            whenever(getPublicLinks(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.increaseLinksTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(false)

                    whenever(getNodeByHandle(any())).thenReturn(null)

                    underTest.increaseLinksTreeDepth(987654321L)
                    assertThat(awaitItem()).isEqualTo(true)
                }
        }

    @Test
    fun `test that getParentNodeHandle is called when setLinksTreeDepth`() =
        runTest {
            val handle = 123456789L
            underTest.increaseLinksTreeDepth(handle)
            verify(getParentNodeHandle).invoke(handle)
        }

    @Test
    fun `test that parent handle is set with result of getParentNodeHandle`() =
        runTest {
            val expected = 111111111L
            whenever(getParentNodeHandle(any())).thenReturn(expected)
            whenever(getPublicLinks(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.linksParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(null)
                    underTest.increaseLinksTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that parent handle is set to null when refreshNodes fails`() =
        runTest {
            whenever(getParentNodeHandle(any())).thenReturn(111111111L)
            whenever(getPublicLinks(any())).thenReturn(mock())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.linksParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(null)
                    underTest.increaseLinksTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(111111111L)
                    whenever(getPublicLinks(any())).thenReturn(null)
                    underTest.increaseLinksTreeDepth(123456789L)
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
        verify(getPublicLinks, times(2)).invoke(underTest.state.value.linksHandle)
    }

    @Test
    fun `test that sort order is set with result of getLinksSortOrder if depth is equals to 0 when call setIncomingTreeDepth`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getPublicLinks(any())).thenReturn(mock())
            whenever(getLinksSortOrder()).thenReturn(expected)

            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.resetLinksTreeDepth()
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that sort order is set with result of getCloudSortOrder if depth is different than 0 when call setIncomingTreeDepth`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getPublicLinks(any())).thenReturn(mock())
            whenever(getCloudSortOrder()).thenReturn(expected)

            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.increaseLinksTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that sort order is set with result of getLinksSortOrder when refreshNodes fails`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getPublicLinks(any())).thenReturn(null)
            whenever(getLinksSortOrder()).thenReturn(expected)

            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.increaseLinksTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }
}
