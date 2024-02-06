package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.shares.ShareFileNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.node.GetTypedChildrenNodeUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetOutgoingSharesChildrenNodeUseCaseTest {

    private val getNodeByHandle: GetNodeByIdUseCase = mock()
    private val mapNodeToShareUseCase: MapNodeToShareUseCase = mock()
    private val getChildrenNode: GetTypedChildrenNodeUseCase = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getOthersSortOrder: GetOthersSortOrder = mock()
    private val nodeRepository: NodeRepository = mock()

    private val underTest = GetOutgoingSharesChildrenNodeUseCase(
        getNodeByHandle,
        getChildrenNode,
        mapNodeToShareUseCase,
        getCloudSortOrder,
        getOthersSortOrder,
        nodeRepository
    )

    @Test
    fun `test that invoke with parentHandle -1 should return list of outgoing share nodes`() =
        runTest {
            val shareData = mock<ShareData> {
                on { nodeHandle }.thenReturn(1L)
            }
            val node = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            val shareFileNode = mock<ShareFileNode>()
            whenever(getOthersSortOrder.invoke()).thenReturn(SortOrder.ORDER_NONE)
            whenever(mapNodeToShareUseCase.invoke(node, shareData)).thenReturn(shareFileNode)
            whenever(nodeRepository.getAllOutgoingShares(SortOrder.ORDER_NONE)).thenReturn(
                listOf(shareData)
            )
            whenever(getNodeByHandle.invoke(NodeId(shareData.nodeHandle))).thenReturn(node)

            val result = underTest.invoke(-1L)

            assertThat(result).isNotNull()
            assertThat(result).hasSize(1)
        }

    @Test
    fun `test that invoke with valid parentHandle should return list of child nodes`() = runTest {
        val childNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(2L))
        }
        val shareFileNode = mock<ShareFileNode>()
        whenever(mapNodeToShareUseCase.invoke(any(), any())).thenReturn(shareFileNode)
        whenever(getCloudSortOrder.invoke()).thenReturn(SortOrder.ORDER_NONE)
        whenever(getNodeByHandle.invoke(NodeId(123L))).thenReturn(childNode)
        whenever(
            getChildrenNode.invoke(
                childNode.id,
                SortOrder.ORDER_NONE
            )
        ).thenReturn(listOf(mock(), mock()))

        val result = underTest.invoke(123L)

        assertThat(result).isNotNull()
        assertThat(result).hasSize(2)
    }
}
