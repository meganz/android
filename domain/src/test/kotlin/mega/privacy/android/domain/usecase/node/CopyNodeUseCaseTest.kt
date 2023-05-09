package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test Class for [CopyNodeUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CopyNodeUseCaseTest {

    private lateinit var underTest: CopyNodeUseCase

    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CopyNodeUseCase(
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that node is copied and returned when invoked`() = runTest {
        val nodeToCopy = NodeId(123L)
        val newNodeParent = NodeId(456L)
        val newNodeName = "new Node"
        val expected = NodeId(789L)
        whenever(nodeRepository.copyNode(nodeToCopy, newNodeParent, newNodeName)).thenReturn(
            expected
        )
        val actual = underTest(nodeToCopy, newNodeParent, newNodeName)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}
