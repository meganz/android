package test.mega.privacy.android.app.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetInboxChildrenNodes
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.HasInboxChildren
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class of [DefaultGetInboxChildrenNodes]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetInboxChildrenNodesTest {

    private lateinit var underTest: DefaultGetInboxChildrenNodes

    private val getChildrenNode = mock<GetChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_NONE)
    }
    private val getInboxNode = mock<GetInboxNode>()
    private val hasInboxChildren = mock<HasInboxChildren>()
    private val monitorNodeUpdates = mock<MonitorNodeUpdates>()

    @Before
    fun setUp() {
        underTest = DefaultGetInboxChildrenNodes(
            getChildrenNode = getChildrenNode,
            getCloudSortOrder = getCloudSortOrder,
            getInboxNode = getInboxNode,
            hasInboxChildren = hasInboxChildren,
            monitorNodeUpdates = monitorNodeUpdates,
        )
    }

    @Test
    fun `test that an empty list is returned if the inbox node is null`() = runTest {
        whenever(hasInboxChildren()).thenReturn(true)
        whenever(getInboxNode()).thenReturn(null)

        underTest().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that an empty list is returned if the inbox node does not have children nodes`() =
        runTest {
            whenever(hasInboxChildren()).thenReturn(false)

            underTest().test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the inbox children nodes are returned`() = runTest {
        val testInboxNode = mock<MegaNode>()
        val testChildNode = mock<MegaNode>()

        whenever(hasInboxChildren()).thenReturn(true)
        whenever(getInboxNode()).thenReturn(testInboxNode)
        whenever(
            getChildrenNode(
                parent = testInboxNode,
                order = getCloudSortOrder(),
            )
        ).thenReturn(listOf(testChildNode))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(listOf(testChildNode))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that whenever a node update occurs, the use case to retrieve the inbox children nodes is called`() =
        runTest {
            val testInboxNode = mock<MegaNode>()

            whenever(monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(emptyMap())))
            whenever(getInboxNode()).thenReturn(testInboxNode)
            whenever(hasInboxChildren()).thenReturn(true)

            underTest().test {
                // The use case is called on ViewModel initialization and
                // when a Node Update occurs
                verify(getChildrenNode, times(2)).invoke(
                    parent = testInboxNode,
                    order = getCloudSortOrder()
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
}