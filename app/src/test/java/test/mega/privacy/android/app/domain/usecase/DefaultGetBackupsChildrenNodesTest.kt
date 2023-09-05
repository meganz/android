package test.mega.privacy.android.app.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetBackupsChildrenNodes
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetBackupsNode
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.HasBackupsChildren
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class of [DefaultGetBackupsChildrenNodes]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetBackupsChildrenNodesTest {

    private lateinit var underTest: DefaultGetBackupsChildrenNodes

    private val getChildrenNode = mock<GetChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_NONE)
    }
    private val getBackupsNode = mock<GetBackupsNode>()
    private val hasBackupsChildren = mock<HasBackupsChildren>()
    private val monitorNodeUpdates = mock<MonitorNodeUpdates>()

    @Before
    fun setUp() {
        underTest = DefaultGetBackupsChildrenNodes(
            getChildrenNode = getChildrenNode,
            getCloudSortOrder = getCloudSortOrder,
            getBackupsNode = getBackupsNode,
            hasBackupsChildren = hasBackupsChildren,
            monitorNodeUpdates = monitorNodeUpdates,
        )
    }

    @Test
    fun `test that an empty list is returned if the backups node is null`() = runTest {
        whenever(hasBackupsChildren()).thenReturn(true)
        whenever(getBackupsNode()).thenReturn(null)

        underTest().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that an empty list is returned if the backups node does not have children nodes`() =
        runTest {
            whenever(hasBackupsChildren()).thenReturn(false)

            underTest().test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the backups children nodes are returned`() = runTest {
        val testBackupsNode = mock<MegaNode>()
        val testChildNode = mock<MegaNode>()

        whenever(hasBackupsChildren()).thenReturn(true)
        whenever(getBackupsNode()).thenReturn(testBackupsNode)
        whenever(
            getChildrenNode(
                parent = testBackupsNode,
                order = getCloudSortOrder(),
            )
        ).thenReturn(listOf(testChildNode))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(listOf(testChildNode))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that whenever a node update occurs, the use case to retrieve the backups children nodes is called`() =
        runTest {
            val testBackupsNode = mock<MegaNode>()

            whenever(monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(emptyMap())))
            whenever(getBackupsNode()).thenReturn(testBackupsNode)
            whenever(hasBackupsChildren()).thenReturn(true)

            underTest().test {
                // The use case is called on ViewModel initialization and
                // when a Node Update occurs
                verify(getChildrenNode, times(2)).invoke(
                    parent = testBackupsNode,
                    order = getCloudSortOrder()
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
}