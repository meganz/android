package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.MultipleNodesRestoreResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.GetNodeNameByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

internal class RestoreNodesUseCaseTest {
    private lateinit var underTest: RestoreNodesUseCase

    private val moveNodeUseCase: MoveNodeUseCase = mock()
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase = mock()
    private val getNodeNameByIdUseCase: GetNodeNameByIdUseCase = mock()
    private val accountRepository: AccountRepository = mock()
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase = mock()

    @Before
    fun setUp() {
        underTest = RestoreNodesUseCase(
            moveNodeUseCase = moveNodeUseCase,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
            getNodeNameByIdUseCase = getNodeNameByIdUseCase,
            accountRepository = accountRepository,
            getRootNodeIdUseCase = getRootNodeIdUseCase
        )
    }

    @Test
    fun `test that moves to root when destination is in rubbish bin or deleted`() = runTest {
        val moveFirstNode = 1L to 2L
        val rootNodeId = NodeId(999L)
        val rootNodeName = "Cloud Drive"

        whenever(isNodeInRubbishOrDeletedUseCase(moveFirstNode.second)).thenReturn(true)
        whenever(getRootNodeIdUseCase()).thenReturn(rootNodeId)
        whenever(getNodeNameByIdUseCase(rootNodeId)).thenReturn(rootNodeName)
        whenever(moveNodeUseCase(NodeId(moveFirstNode.first), rootNodeId))
            .thenReturn(NodeId(moveFirstNode.first))

        val result = underTest(mapOf(moveFirstNode))

        verify(moveNodeUseCase).invoke(NodeId(moveFirstNode.first), rootNodeId)
        verify(getRootNodeIdUseCase).invoke()
        verify(getNodeNameByIdUseCase).invoke(rootNodeId)
        verify(accountRepository).resetAccountDetailsTimeStamp()
        assertThat(result).isInstanceOf(SingleNodeRestoreResult::class.java)
        val singleResult = result as SingleNodeRestoreResult
        assertThat(singleResult.successCount).isEqualTo(1)
        assertThat(singleResult.destinationFolderName).isEqualTo(rootNodeName)
    }

    @Test
    fun `test that uses original destination when not in rubbish bin and not deleted`() = runTest {
        val moveFirstNode = 1L to 100L
        val destinationNodeId = NodeId(moveFirstNode.second)
        val destinationNodeName = "Destination Folder"

        whenever(isNodeInRubbishOrDeletedUseCase(moveFirstNode.second)).thenReturn(false)
        whenever(getNodeNameByIdUseCase(destinationNodeId)).thenReturn(destinationNodeName)
        whenever(moveNodeUseCase(NodeId(moveFirstNode.first), destinationNodeId))
            .thenReturn(NodeId(moveFirstNode.first))

        val result = underTest(mapOf(moveFirstNode))

        verify(moveNodeUseCase).invoke(NodeId(moveFirstNode.first), destinationNodeId)
        verify(getNodeNameByIdUseCase).invoke(destinationNodeId)
        verifyNoInteractions(getRootNodeIdUseCase)
        verify(accountRepository).resetAccountDetailsTimeStamp()
        assertThat(result).isEqualTo(SingleNodeRestoreResult(1, destinationNodeName))
    }

    @Test
    fun `test that throw ForeignNodeException when move node throw ForeignNodeException`() =
        runTest {
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(false)
            whenever(moveNodeUseCase(NodeId(any()), NodeId(any()), eq(null)))
                .thenThrow(ForeignNodeException::class.java)
            try {
                underTest(mapOf(1L to 2L))
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(ForeignNodeException::class.java)
            }
        }

    @Test
    fun `test that return MultipleNodesRestoreResult correctly when at least one move successfully`() =
        runTest {
            val moveFirstNode = 1L to 100L
            val moveSecondNode = 2L to 101L
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(false)
            whenever(moveNodeUseCase(NodeId(moveFirstNode.first), NodeId(moveFirstNode.second)))
                .thenReturn(NodeId(moveFirstNode.first))
            whenever(moveNodeUseCase(NodeId(moveSecondNode.first), NodeId(moveSecondNode.second)))
                .thenThrow(SecurityException::class.java)
            val result = underTest(mapOf(moveFirstNode, moveSecondNode))
            verify(accountRepository).resetAccountDetailsTimeStamp()
            assertThat(result).isEqualTo(MultipleNodesRestoreResult(1, 1))
        }

    @Test
    fun `test that return SingleNodeRestoreResult correctly when move successfully`() =
        runTest {
            val moveFirstNode = 1L to 100L
            val destinationNodeId = NodeId(moveFirstNode.second)
            val nodeName = "name"
            whenever(isNodeInRubbishOrDeletedUseCase(moveFirstNode.second)).thenReturn(false)
            whenever(getNodeNameByIdUseCase(destinationNodeId)).thenReturn(nodeName)
            whenever(moveNodeUseCase(NodeId(moveFirstNode.first), destinationNodeId))
                .thenReturn(NodeId(moveFirstNode.first))
            val result = underTest(mapOf(moveFirstNode))
            verify(getNodeNameByIdUseCase).invoke(destinationNodeId)
            verify(accountRepository).resetAccountDetailsTimeStamp()
            assertThat(result).isEqualTo(SingleNodeRestoreResult(1, nodeName))
        }

    @Test
    fun `test that return SingleNodeRestoreResult correctly when move failed`() =
        runTest {
            val moveFirstNode = 1L to 100L
            whenever(isNodeInRubbishOrDeletedUseCase(moveFirstNode.second)).thenReturn(false)
            whenever(moveNodeUseCase(NodeId(moveFirstNode.first), NodeId(moveFirstNode.second)))
                .thenThrow(RuntimeException::class.java)
            val result = underTest(mapOf(moveFirstNode))
            verifyNoInteractions(accountRepository)
            assertThat(result).isEqualTo(SingleNodeRestoreResult(0, null))
        }

    @Test
    fun `test that uses original destination when root node ID is null`() = runTest {
        val moveFirstNode = 1L to 2L
        val destinationNodeId = NodeId(moveFirstNode.second)
        val destinationNodeName = "Original Destination"

        whenever(isNodeInRubbishOrDeletedUseCase(moveFirstNode.second)).thenReturn(true)
        whenever(getRootNodeIdUseCase()).thenReturn(null)
        whenever(getNodeNameByIdUseCase(destinationNodeId)).thenReturn(destinationNodeName)
        whenever(moveNodeUseCase(NodeId(moveFirstNode.first), destinationNodeId))
            .thenReturn(NodeId(moveFirstNode.first))

        val result = underTest(mapOf(moveFirstNode))

        verify(getRootNodeIdUseCase).invoke()
        verify(moveNodeUseCase).invoke(NodeId(moveFirstNode.first), destinationNodeId)
        verify(getNodeNameByIdUseCase).invoke(destinationNodeId)
        verify(accountRepository).resetAccountDetailsTimeStamp()
        assertThat(result).isInstanceOf(SingleNodeRestoreResult::class.java)
        val singleResult = result as SingleNodeRestoreResult
        assertThat(singleResult.successCount).isEqualTo(1)
        assertThat(singleResult.destinationFolderName).isEqualTo(destinationNodeName)
    }
}
