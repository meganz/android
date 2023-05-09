package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetTypedChildrenNodeUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetTypedChildrenNodeUseCaseTest {

    private lateinit var underTest: GetTypedChildrenNodeUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val addNodeTypeUseCase = mock<AddNodeType>()

    @BeforeAll
    fun setUp() {
        underTest = GetTypedChildrenNodeUseCase(
            nodeRepository = nodeRepository,
            addNodeTypeUseCase = addNodeTypeUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that children node are returned when invoked`() =
        runTest {
            val typedNodes = listOf<TypedFileNode>(mock(), mock())
            val untypedNodes = listOf<FileNode>(mock(), mock())
            val parentNodeId = NodeId(123L)
            val sortOrder = SortOrder.ORDER_DEFAULT_ASC
            whenever(
                nodeRepository.getNodeChildren(
                    parentNodeId,
                    sortOrder
                )
            ).thenReturn(untypedNodes)
            untypedNodes.forEachIndexed { index, unTypedNode ->
                whenever(addNodeTypeUseCase.invoke(unTypedNode)).thenReturn(typedNodes[index])
            }
            val actual = underTest(parentNodeId, sortOrder)
            Truth.assertThat(actual).isEqualTo(typedNodes)
        }
}
