package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.MultipleNodesRestoreResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.exception.node.NodeInRubbishException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

internal class RestoreNodesUseCaseTest {
    private lateinit var underTest: RestoreNodesUseCase

    private val moveNodeUseCase: MoveNodeUseCase = mock()
    private val isNodeInRubbish: IsNodeInRubbish = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val accountRepository: AccountRepository = mock()

    @Before
    fun setUp() {
        underTest = RestoreNodesUseCase(
            moveNodeUseCase = moveNodeUseCase,
            isNodeInRubbish = isNodeInRubbish,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            accountRepository = accountRepository
        )
    }

    @Test
    fun `test that throw NodeInRubbishException when parent is in rubbish bin`() = runTest {
        whenever(isNodeInRubbish(any())).thenReturn(true)
        try {
            underTest(mapOf(1L to 2L))
        } catch (e: Exception) {
            Truth.assertThat(e).isInstanceOf(NodeInRubbishException::class.java)
        }
    }

    @Test
    fun `test that throw ForeignNodeException when move node throw ForeignNodeException`() =
        runTest {
            whenever(isNodeInRubbish(any())).thenReturn(false)
            whenever(moveNodeUseCase(NodeId(any()), NodeId(any())))
                .thenThrow(ForeignNodeException::class.java)
            try {
                underTest(mapOf(1L to 2L))
            } catch (e: Exception) {
                Truth.assertThat(e).isInstanceOf(ForeignNodeException::class.java)
            }
        }

    @Test
    fun `test that return MultipleNodesRestoreResult correctly when at least one move successfully`() =
        runTest {
            val moveFirstNode = 1L to 100L
            val moveSecondNode = 2L to 101L
            whenever(isNodeInRubbish(any())).thenReturn(false)
            whenever(moveNodeUseCase(NodeId(moveFirstNode.first), NodeId(moveFirstNode.second)))
                .thenReturn(NodeId(moveFirstNode.first))
            whenever(moveNodeUseCase(NodeId(moveSecondNode.first), NodeId(moveSecondNode.second)))
                .thenThrow(SecurityException::class.java)
            val result = underTest(mapOf(moveFirstNode, moveSecondNode))
            verify(accountRepository).resetAccountDetailsTimeStamp()
            Truth.assertThat(result).isEqualTo(MultipleNodesRestoreResult(1, 1))
        }

    @Test
    fun `test that return SingleNodeRestoreResult correctly when move successfully`() =
        runTest {
            val moveFirstNode = 1L to 100L
            val fileNode = mock<FileNode> {
                on { name }.thenReturn("name")
            }
            whenever(getNodeByHandleUseCase(moveFirstNode.second)).thenReturn(fileNode)
            whenever(isNodeInRubbish(any())).thenReturn(false)
            whenever(moveNodeUseCase(NodeId(moveFirstNode.first), NodeId(moveFirstNode.second)))
                .thenReturn(NodeId(moveFirstNode.first))
            val result = underTest(mapOf(moveFirstNode))
            verify(accountRepository).resetAccountDetailsTimeStamp()
            Truth.assertThat(result).isEqualTo(SingleNodeRestoreResult(1, fileNode.name))
        }

    @Test
    fun `test that return SingleNodeRestoreResult correctly when move failed`() =
        runTest {
            val moveFirstNode = 1L to 100L
            whenever(isNodeInRubbish(any())).thenReturn(false)
            whenever(moveNodeUseCase(NodeId(moveFirstNode.first), NodeId(moveFirstNode.second)))
                .thenThrow(RuntimeException::class.java)
            val result = underTest(mapOf(moveFirstNode))
            verifyNoInteractions(accountRepository)
            Truth.assertThat(result).isEqualTo(SingleNodeRestoreResult(0, null))
        }
}