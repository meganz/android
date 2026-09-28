package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
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
class GetContactIncomingSharesUseCaseTest {

    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()
    private val mapNodeToShareUseCase: MapNodeToShareUseCase = mock()
    private val getOthersSortOrder: GetOthersSortOrder = mock()
    private val nodeRepository: NodeRepository = mock()

    private val underTest = GetContactIncomingSharesUseCase(
        getNodeByIdUseCase = getNodeByIdUseCase,
        getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
        areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
        mapNodeToShareUseCase = mapNodeToShareUseCase,
        getOthersSortOrder = getOthersSortOrder,
        nodeRepository = nodeRepository,
    )

    @Test
    fun `test that invoke maps the contact incoming shares returned by the repository`() = runTest {
        val email = "contact@mega.co.nz"
        val shareData = mock<ShareData> {
            on { nodeHandle }.thenReturn(1L)
            on { user }.thenReturn(email)
        }
        val node = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(1L))
        }
        whenever(getContactVerificationWarningUseCase()).thenReturn(false)
        whenever(getOthersSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(nodeRepository.getContactIncomingSharesSortedByOrder(email, SortOrder.ORDER_NONE))
            .thenReturn(listOf(shareData))
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)
        whenever(mapNodeToShareUseCase(node, shareData)).thenReturn(mock<ShareFolderNode>())

        val result = underTest(email)

        assertThat(result).hasSize(1)
        verify(nodeRepository).getContactIncomingSharesSortedByOrder(email, SortOrder.ORDER_NONE)
        verify(getNodeByIdUseCase).invoke(NodeId(1L))
        verifyNoInteractions(areCredentialsVerifiedUseCase)
    }

    @Test
    fun `test that invoke preserves the order returned by the repository`() = runTest {
        val email = "contact@mega.co.nz"
        val firstShare = mock<ShareData> {
            on { nodeHandle }.thenReturn(1L)
            on { user }.thenReturn(email)
        }
        val secondShare = mock<ShareData> {
            on { nodeHandle }.thenReturn(2L)
            on { user }.thenReturn(email)
        }
        val firstNode = mock<TypedFolderNode> { on { id }.thenReturn(NodeId(1L)) }
        val secondNode = mock<TypedFolderNode> { on { id }.thenReturn(NodeId(2L)) }
        val firstShareNode = mock<ShareFolderNode>()
        val secondShareNode = mock<ShareFolderNode>()
        whenever(getContactVerificationWarningUseCase()).thenReturn(false)
        whenever(getOthersSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(nodeRepository.getContactIncomingSharesSortedByOrder(email, SortOrder.ORDER_NONE))
            .thenReturn(listOf(firstShare, secondShare))
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(firstNode)
        whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(secondNode)
        whenever(mapNodeToShareUseCase(firstNode, firstShare)).thenReturn(firstShareNode)
        whenever(mapNodeToShareUseCase(secondNode, secondShare)).thenReturn(secondShareNode)

        val result = underTest(email)

        assertThat(result).containsExactly(firstShareNode, secondShareNode).inOrder()
    }

    @Test
    fun `test that credentials are verified when contact verification is on`() = runTest {
        val email = "contact@mega.co.nz"
        val shareData = mock<ShareData> {
            on { nodeHandle }.thenReturn(1L)
            on { user }.thenReturn(email)
        }
        val node = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(1L))
        }
        whenever(getContactVerificationWarningUseCase()).thenReturn(true)
        whenever(getOthersSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(nodeRepository.getContactIncomingSharesSortedByOrder(email, SortOrder.ORDER_NONE))
            .thenReturn(listOf(shareData))
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)
        whenever(areCredentialsVerifiedUseCase(email)).thenReturn(true)
        whenever(
            mapNodeToShareUseCase(node, shareData.copy(isContactCredentialsVerified = true))
        ).thenReturn(mock<ShareFolderNode>())

        val result = underTest(email)

        assertThat(result).hasSize(1)
        verify(areCredentialsVerifiedUseCase).invoke(email)
    }

    @Test
    fun `test that the provided sort order is used and the global others sort order is not read`() =
        runTest {
            val email = "contact@mega.co.nz"
            val shareData = mock<ShareData> {
                on { nodeHandle }.thenReturn(1L)
                on { user }.thenReturn(email)
            }
            val node = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            whenever(getContactVerificationWarningUseCase()).thenReturn(false)
            whenever(
                nodeRepository.getContactIncomingSharesSortedByOrder(
                    email,
                    SortOrder.ORDER_SIZE_ASC
                )
            ).thenReturn(listOf(shareData))
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)
            whenever(mapNodeToShareUseCase(node, shareData)).thenReturn(mock<ShareFolderNode>())

            underTest(email, sortOrder = SortOrder.ORDER_SIZE_ASC)

            verify(nodeRepository)
                .getContactIncomingSharesSortedByOrder(email, SortOrder.ORDER_SIZE_ASC)
            verifyNoInteractions(getOthersSortOrder)
        }

    @Test
    fun `test that invoke returns empty list when the contact has no incoming shares`() = runTest {
        val email = "contact@mega.co.nz"
        whenever(getContactVerificationWarningUseCase()).thenReturn(false)
        whenever(getOthersSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(nodeRepository.getContactIncomingSharesSortedByOrder(any(), any()))
            .thenReturn(emptyList())

        val result = underTest(email)

        assertThat(result).isEmpty()
        verifyNoInteractions(getNodeByIdUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            getContactVerificationWarningUseCase,
            areCredentialsVerifiedUseCase,
            mapNodeToShareUseCase,
            getOthersSortOrder,
            nodeRepository,
        )
    }
}
