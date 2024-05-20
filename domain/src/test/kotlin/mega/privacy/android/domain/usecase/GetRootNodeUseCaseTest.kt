package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetRootNodeUseCaseTest {
    private lateinit var underTest: GetRootNodeUseCase
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetRootNodeUseCase(nodeRepository = nodeRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(nodeRepository)
    }

    @Test
    fun `test that result is null`() =
        runTest {
            whenever(nodeRepository.getRootNode()).thenReturn(null)
            assertThat(underTest()).isNull()
        }

    @Test
    fun `test that the node is returned`() =
        runTest {
            val testNode = mock<Node>()
            whenever(nodeRepository.getRootNode()).thenReturn(testNode)
            assertThat(underTest()).isEqualTo(testNode)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest()
            verify(nodeRepository).getRootNode()
        }
}