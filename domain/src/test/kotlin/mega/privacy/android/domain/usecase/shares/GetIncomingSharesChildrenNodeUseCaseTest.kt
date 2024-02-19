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
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.node.GetTypedChildrenNodeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetIncomingSharesChildrenNodeUseCaseTest {

    private val getNodeByHandle: GetNodeByIdUseCase = mock()
    private val mapNodeToShareUseCase: MapNodeToShareUseCase = mock()
    private val getChildrenNode: GetTypedChildrenNodeUseCase = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getOthersSortOrder: GetOthersSortOrder = mock()
    private val nodeRepository: NodeRepository = mock()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()

    private val underTest = GetIncomingSharesChildrenNodeUseCase(
        getNodeByHandle,
        getChildrenNode,
        getContactVerificationWarningUseCase,
        areCredentialsVerifiedUseCase,
        mapNodeToShareUseCase,
        getCloudSortOrder,
        getOthersSortOrder,
        nodeRepository
    )

    @Test
    fun `test that invoke with parentHandle -1 should return list of incoming share nodes`() =
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
            whenever(getContactVerificationWarningUseCase.invoke()).thenReturn(false)
            whenever(nodeRepository.getAllIncomingShares(SortOrder.ORDER_NONE)).thenReturn(
                listOf(shareData)
            )
            whenever(getNodeByHandle.invoke(NodeId(shareData.nodeHandle))).thenReturn(node)

            val result = underTest.invoke(-1L)

            assertThat(result).isNotNull()
            assertThat(result).hasSize(1)
            verifyNoInteractions(areCredentialsVerifiedUseCase)
        }

    @Test
    fun `test that invoke with valid parentHandle should return list of child nodes`() = runTest {
        val childNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(2L))
        }
        val shareFileNode = mock<ShareFileNode>()
        whenever(mapNodeToShareUseCase.invoke(any(), any())).thenReturn(shareFileNode)
        whenever(getCloudSortOrder.invoke()).thenReturn(SortOrder.ORDER_NONE)
        whenever(getContactVerificationWarningUseCase.invoke()).thenReturn(false)
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
        verifyNoInteractions(areCredentialsVerifiedUseCase)
    }

    @Test
    fun `test that contact credentials are checked when contact verification is ON and parentHandle is -1`() =
        runTest {
            val shareData = mock<ShareData> {
                on { nodeHandle }.thenReturn(1L)
                on { user }.thenReturn("user")
            }
            val node = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            val shareFileNode = mock<ShareFileNode>()
            whenever(getOthersSortOrder.invoke()).thenReturn(SortOrder.ORDER_NONE)
            whenever(nodeRepository.getAllIncomingShares(SortOrder.ORDER_NONE)).thenReturn(
                listOf(shareData)
            )
            whenever(getNodeByHandle.invoke(NodeId(shareData.nodeHandle))).thenReturn(node)
            whenever(
                mapNodeToShareUseCase.invoke(
                    node, shareData.copy(
                        isContactCredentialsVerified = true
                    )
                )
            ).thenReturn(shareFileNode)
            whenever(getContactVerificationWarningUseCase.invoke()).thenReturn(true)
            whenever(areCredentialsVerifiedUseCase.invoke(any())).thenReturn(true)

            val result = underTest.invoke(-1L)

            assertThat(result).isNotNull()
            assertThat(result).hasSize(1)
            verify(areCredentialsVerifiedUseCase).invoke(any())
        }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeByHandle,
            mapNodeToShareUseCase,
            getChildrenNode,
            getCloudSortOrder,
            getOthersSortOrder,
            nodeRepository,
            getContactVerificationWarningUseCase,
            areCredentialsVerifiedUseCase
        )
    }
}
