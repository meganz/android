package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetNodePathByIdUseCaseTest {
    lateinit var underTest: GetNodePathByIdUseCase
    private val nodeRepository = mock<NodeRepository>()

    @Before
    fun setUp() {
        underTest = GetNodePathByIdUseCase(nodeRepository)
    }

    @Test
    fun `test that the node path is returned when invoked`() =
        runTest {
            val id = NodeId(1234L)
            val expectedPath = "Node Path"
            whenever(nodeRepository.getNodePathById(id)).thenReturn(expectedPath)
            assertThat(underTest(id)).isEqualTo(expectedPath)
            assertThat(underTest(NodeId(-1))).isNotEqualTo(expectedPath)
            assertThat(underTest(NodeId(-1))).isNull()
        }

    @Test
    fun `test that the node path is null when the node id is invalid or does not exist`() =
        runTest {
            assertThat(underTest(NodeId(-1))).isNull()
        }
}