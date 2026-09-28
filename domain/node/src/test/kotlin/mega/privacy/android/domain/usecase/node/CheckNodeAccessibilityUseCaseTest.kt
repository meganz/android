package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckNodeAccessibilityUseCaseTest {
    private lateinit var underTest: CheckNodeAccessibilityUseCase

    private val nodeRepository: NodeRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = CheckNodeAccessibilityUseCase(nodeRepository = nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that invoke succeeds when node is accessible`() = runTest {
        val nodeId = NodeId(1L)
        whenever(nodeRepository.checkNodeAccessibility(nodeId)).thenReturn(Unit)

        assertDoesNotThrow { underTest(nodeId) }
    }

    @Test
    fun `test that invoke throws BlockedMegaException when node is taken down`() = runTest {
        val nodeId = NodeId(1L)
        whenever(nodeRepository.checkNodeAccessibility(nodeId)).thenAnswer {
            throw BlockedMegaException(
                errorCode = -16,
                errorString = "File removed as it violated our Terms of Service"
            )
        }

        assertThrows<BlockedMegaException> { underTest(nodeId) }
    }

    @Test
    fun `test that invoke throws exception when repository throws`() = runTest {
        val nodeId = NodeId(1L)
        whenever(nodeRepository.checkNodeAccessibility(nodeId)).thenThrow(RuntimeException("Unexpected error"))

        assertThrows<RuntimeException> { underTest(nodeId) }
    }
}
