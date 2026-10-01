package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetNodeFromCloudDriveUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetNodeFromCloudDriveUseCaseTest {

    private lateinit var underTest: GetNodeFromCloudDriveUseCase

    private val getNodesByOriginalFingerprintUseCase =
        mock<GetNodesByOriginalFingerprintUseCase>()
    private val getNodesByFingerprintUseCase = mock<GetNodesByFingerprintUseCase>()

    private val originalFingerprint = "originalFingerprint"
    private val generatedFingerprint = "generatedFingerprint"
    private val parentNodeId = NodeId(1234L)
    private val nodeInsideTargetFolder = mock<FileNode>()
    private val nodeOutsideTargetFolder = mock<FileNode>()

    @BeforeAll
    fun setUp() {
        underTest = GetNodeFromCloudDriveUseCase(
            getNodesByOriginalFingerprintUseCase = getNodesByOriginalFingerprintUseCase,
            getNodesByFingerprintUseCase = getNodesByFingerprintUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodesByOriginalFingerprintUseCase,
            getNodesByFingerprintUseCase,
        )
    }

    @Test
    fun `test that the node in the parent folder is retrieved using the original fingerprint when querying all nodes`() =
        runTest {
            whenever(nodeInsideTargetFolder.parentId).thenReturn(parentNodeId)
            whenever(nodeOutsideTargetFolder.parentId).thenReturn(NodeId(7890L))
            whenever(
                getNodesByOriginalFingerprintUseCase(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = null,
                )
            ).thenReturn(listOf(nodeInsideTargetFolder, nodeOutsideTargetFolder))

            val result = underTest(
                originalFingerprint = originalFingerprint,
                generatedFingerprint = null,
                parentNodeId = parentNodeId,
            )

            assertThat(result).isEqualTo(nodeInsideTargetFolder)
        }

    @Test
    fun `test that the first node found outside the parent folder is retrieved using the original fingerprint when querying all nodes`() =
        runTest {
            val firstNode = mock<FileNode>()
            val secondNode = mock<FileNode>()
            whenever(
                getNodesByOriginalFingerprintUseCase(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = null,
                )
            ).thenReturn(listOf(firstNode, secondNode))

            val result = underTest(
                originalFingerprint = originalFingerprint,
                generatedFingerprint = null,
                parentNodeId = parentNodeId,
            )

            assertThat(result).isEqualTo(firstNode)
        }

    @Test
    fun `test that the node in the parent folder is retrieved using the fingerprint when querying all nodes`() =
        runTest {
            whenever(nodeInsideTargetFolder.parentId).thenReturn(parentNodeId)
            whenever(nodeOutsideTargetFolder.parentId).thenReturn(NodeId(7890L))
            whenever(
                getNodesByOriginalFingerprintUseCase(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = null,
                )
            ).thenReturn(emptyList())
            whenever(getNodesByFingerprintUseCase(originalFingerprint)).thenReturn(
                listOf(nodeInsideTargetFolder, nodeOutsideTargetFolder)
            )

            val result = underTest(
                originalFingerprint = originalFingerprint,
                generatedFingerprint = null,
                parentNodeId = parentNodeId,
            )

            assertThat(result).isEqualTo(nodeInsideTargetFolder)
        }

    @Test
    fun `test that the first node found outside the parent folder is retrieved using the fingerprint when querying all nodes`() =
        runTest {
            val firstNode = mock<FileNode>()
            val secondNode = mock<FileNode>()
            whenever(
                getNodesByOriginalFingerprintUseCase(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = null,
                )
            ).thenReturn(emptyList())
            whenever(getNodesByFingerprintUseCase(originalFingerprint)).thenReturn(
                listOf(firstNode, secondNode)
            )

            val result = underTest(
                originalFingerprint = originalFingerprint,
                generatedFingerprint = null,
                parentNodeId = parentNodeId,
            )

            assertThat(result).isEqualTo(firstNode)
        }

    @Test
    fun `test that the node in the parent folder is retrieved using the generated fingerprint when querying all nodes`() =
        runTest {
            whenever(nodeInsideTargetFolder.parentId).thenReturn(parentNodeId)
            whenever(nodeOutsideTargetFolder.parentId).thenReturn(NodeId(7890L))
            whenever(
                getNodesByOriginalFingerprintUseCase(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = null,
                )
            ).thenReturn(emptyList())
            whenever(getNodesByFingerprintUseCase(originalFingerprint)).thenReturn(
                emptyList()
            )
            whenever(getNodesByFingerprintUseCase(generatedFingerprint)).thenReturn(
                listOf(nodeInsideTargetFolder, nodeOutsideTargetFolder)
            )

            val result = underTest(
                originalFingerprint = originalFingerprint,
                generatedFingerprint = generatedFingerprint,
                parentNodeId = parentNodeId,
            )

            assertThat(result).isEqualTo(nodeInsideTargetFolder)
        }

    @Test
    fun `test that the first node found outside the parent folder is retrieved using the generated fingerprint when querying all nodes`() =
        runTest {
            val firstNode = mock<FileNode>()
            val secondNode = mock<FileNode>()
            whenever(
                getNodesByOriginalFingerprintUseCase(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = null,
                )
            ).thenReturn(emptyList())
            whenever(getNodesByFingerprintUseCase(originalFingerprint)).thenReturn(emptyList())
            whenever(getNodesByFingerprintUseCase(generatedFingerprint)).thenReturn(
                listOf(firstNode, secondNode)
            )

            val result = underTest(
                originalFingerprint = originalFingerprint,
                generatedFingerprint = generatedFingerprint,
                parentNodeId = parentNodeId,
            )

            assertThat(result).isEqualTo(firstNode)
        }

    @Test
    fun `test that no node is retrieved when it cannot be found using the original fingerprint and there is no generated fingerprint`() =
        runTest {
            whenever(
                getNodesByOriginalFingerprintUseCase(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = null,
                )
            ).thenReturn(emptyList())
            whenever(getNodesByFingerprintUseCase(originalFingerprint)).thenReturn(emptyList())

            val result = underTest(
                originalFingerprint = originalFingerprint,
                generatedFingerprint = null,
                parentNodeId = parentNodeId,
            )

            assertThat(result).isNull()
        }

    @Test
    fun `test that no node is retrieved when it cannot be found using the original or generated fingerprint`() =
        runTest {
            whenever(
                getNodesByOriginalFingerprintUseCase(
                    originalFingerprint = originalFingerprint,
                    parentNodeId = null,
                )
            ).thenReturn(emptyList())
            whenever(getNodesByFingerprintUseCase(originalFingerprint)).thenReturn(emptyList())
            whenever(getNodesByFingerprintUseCase(generatedFingerprint)).thenReturn(emptyList())

            val result = underTest(
                originalFingerprint = originalFingerprint,
                generatedFingerprint = generatedFingerprint,
                parentNodeId = parentNodeId,
            )

            assertThat(result).isNull()
        }
}
