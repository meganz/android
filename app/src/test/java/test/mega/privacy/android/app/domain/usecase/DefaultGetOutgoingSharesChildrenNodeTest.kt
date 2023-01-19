package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetOutgoingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetOutgoingSharesChildrenNode
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetOutgoingSharesChildrenNodeTest {
    private lateinit var underTest: GetOutgoingSharesChildrenNode

    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getChildrenNode = mock<GetChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_NONE)
    }
    private val getOthersSortOrder = mock<GetOthersSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_ASC)
    }
    private val nodeRepository = mock<NodeRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetOutgoingSharesChildrenNode(
            getNodeByHandle,
            getChildrenNode,
            getCloudSortOrder,
            getOthersSortOrder,
            nodeRepository,
        )
    }

    @Test
    fun `test that invoke with -1L execute filesRepository getOutgoingSharesNode function with others sort order`() =
        runTest {
            whenever(nodeRepository.getOutgoingSharesNode(any())).thenReturn(emptyList())
            underTest(-1L)

            verify(nodeRepository).getOutgoingSharesNode(getOthersSortOrder())
        }

    @Test
    fun `test that invoke with INVALID_HANDLE executes filesRepository getOutgoingSharesNode function with others sort order`() =
        runTest {
            whenever(nodeRepository.getOutgoingSharesNode(any())).thenReturn(emptyList())

            underTest(MegaApiJava.INVALID_HANDLE)

            verify(nodeRepository).getOutgoingSharesNode(getOthersSortOrder())
        }

    @Test
    fun `test that share nodes retrieved from file repository getOutgoingShareNode without user is filtered out from the list`() =
        runTest {
            val nodeHandle1 = 123456789L
            val megaShare1 = mock<ShareData> {
                on { nodeHandle }.thenReturn(nodeHandle1)
                on { user }.thenReturn("user1")
            }
            val nodeHandle2 = 987654321L
            val megaShare2 = mock<ShareData> {
                on { nodeHandle }.thenReturn(nodeHandle2)
                on { user }.thenReturn(null)
            }
            val nodeHandle3 = 111111111L
            val megaShare3 = mock<ShareData> {
                on { nodeHandle }.thenReturn(nodeHandle3)
                on { user }.thenReturn("user3")
            }
            val list = listOf(megaShare1, megaShare2, megaShare3)
            whenever(nodeRepository.getOutgoingSharesNode(any())).thenReturn(list)

            underTest(-1L)

            verify(getNodeByHandle).invoke(megaShare1.nodeHandle)
            verify(getNodeByHandle).invoke(megaShare3.nodeHandle)
            verify(getNodeByHandle, never()).invoke(megaShare2.nodeHandle)
        }

    @Test
    fun `test that share nodes retrieved from file repository getOutgoingShareNode all have distinct handle`() =
        runTest {
            val nodeHandle1 = 123456789L
            val megaShare1 = mock<ShareData> {
                on { nodeHandle }.thenReturn(nodeHandle1)
                on { user }.thenReturn("user1")
            }
            val nodeHandle2 = 123456789L
            val megaShare2 = mock<ShareData> {
                on { nodeHandle }.thenReturn(nodeHandle2)
                on { user }.thenReturn("user1")
            }
            val nodeHandle3 = 111111111L
            val megaShare3 = mock<ShareData> {
                on { nodeHandle }.thenReturn(nodeHandle3)
                on { user }.thenReturn("user3")
            }
            val list = listOf(megaShare1, megaShare2, megaShare3)
            whenever(nodeRepository.getOutgoingSharesNode(any())).thenReturn(list)

            underTest(-1L)

            verify(getNodeByHandle, times(1)).invoke(123456789L)
            verify(getNodeByHandle).invoke(megaShare3.nodeHandle)
        }

    @Test
    fun `test that invoke with valid parent handle, retrieve parent node`() =
        runTest {
            val parentHandle = 123456789L
            underTest(parentHandle)
            verify(getNodeByHandle).invoke(any())
        }

    @Test
    fun `test that if parent node can be retrieved, executes getChildrenNode with getCloudSortOrder`() =
        runTest {
            val parentHandle = 123456789L
            val result = mock<MegaNode> {}
            whenever(getNodeByHandle(parentHandle)).thenReturn(result)
            underTest(parentHandle)

            verify(getChildrenNode).invoke(result, getCloudSortOrder())
        }
}