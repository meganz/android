package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
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
 * Test class for [GetNodesByOriginalFingerprintUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetNodesByOriginalFingerprintUseCaseTest {

    private lateinit var underTest: GetNodesByOriginalFingerprintUseCase

    private val nodeRepository = mock<NodeRepository>()

    private val originalFingerprint = "originalFingerprint"

    @BeforeAll
    fun setUp() {
        underTest = GetNodesByOriginalFingerprintUseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that all nodes with the same fingerprint are returned under the specific parent node`() =
        runTest {
            val firstParentNodeId = NodeId(123456L)
            val secondParentNodeId = NodeId(789012L)
            val firstParentChildNodes = listOf(mock<FileNode>())
            val secondParentChildNodes = listOf(mock<FileNode>())
            whenever(
                nodeRepository.getNodesByOriginalFingerprint(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = firstParentNodeId,
                )
            ).thenReturn(firstParentChildNodes)
            whenever(
                nodeRepository.getNodesByOriginalFingerprint(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = secondParentNodeId
                )
            ).thenReturn(secondParentChildNodes)

            assertThat(
                underTest(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = firstParentNodeId
                )
            ).isEqualTo(firstParentChildNodes)
            assertThat(
                underTest(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = secondParentNodeId
                )
            ).isEqualTo(secondParentChildNodes)
        }

    @Test
    fun `test that all nodes with the same fingerprint are returned`() = runTest {
        val nodeList = listOf(mock<FileNode>(), mock<FileNode>())

        whenever(
            nodeRepository.getNodesByOriginalFingerprint(
                originalFingerprint = originalFingerprint,
                parentNodeId = null,
            )
        ).thenReturn(nodeList)

        assertThat(
            underTest(
                originalFingerprint = originalFingerprint,
                parentNodeId = null,
            )
        ).isEqualTo(nodeList)
    }
}