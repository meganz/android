package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeNavigationStackUseCaseTest {

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getAncestorsIdsUseCase = mock<GetAncestorsIdsUseCase>()
    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()

    private val underTest = GetNodeNavigationStackUseCase(
        getNodeByIdUseCase = getNodeByIdUseCase,
        getAncestorsIdsUseCase = getAncestorsIdsUseCase,
        getRootNodeIdUseCase = getRootNodeIdUseCase,
    )

    @BeforeEach
    fun reset() {
        reset(getNodeByIdUseCase, getAncestorsIdsUseCase, getRootNodeIdUseCase)
    }

    @Test
    fun `test that a cloud-drive node path drops the root and is ordered top-down`() = runTest {
        val target = NodeId(3L)
        val node = mock<TypedFolderNode>()
        whenever(getNodeByIdUseCase(target)).thenReturn(node)
        // bottom-up: parent, grandparent, root
        whenever(getAncestorsIdsUseCase(node)).thenReturn(
            listOf(
                NodeId(2L),
                NodeId(1L),
                NodeId(0L)
            )
        )
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(0L))

        val result = underTest(target)

        assertThat(result.stack).containsExactly(NodeId(1L), NodeId(2L), NodeId(3L)).inOrder()
        assertThat(result.isUnderRootNode).isTrue()
    }

    @Test
    fun `test that an incoming-share node path keeps the full ancestor chain`() = runTest {
        val target = NodeId(30L)
        val node = mock<TypedFolderNode>()
        whenever(getNodeByIdUseCase(target)).thenReturn(node)
        // bottom-up: parent, shareRoot (root node id is not among them)
        whenever(getAncestorsIdsUseCase(node)).thenReturn(listOf(NodeId(20L), NodeId(10L)))
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(0L))

        val result = underTest(target)

        assertThat(result.stack).containsExactly(NodeId(10L), NodeId(20L), NodeId(30L)).inOrder()
        assertThat(result.isUnderRootNode).isFalse()
    }

    @Test
    fun `test that an empty path is returned when the node cannot be resolved`() = runTest {
        val target = NodeId(5L)
        whenever(getNodeByIdUseCase(target)).thenReturn(null)

        val result = underTest(target)

        assertThat(result.stack).isEmpty()
        assertThat(result.isUnderRootNode).isFalse()
    }
}
