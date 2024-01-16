package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeFromCloudUseCaseTest {

    private lateinit var underTest: GetNodeFromCloudUseCase

    private val getNodeByOriginalFingerprintUseCase = mock<GetNodeByOriginalFingerprintUseCase>()
    private val getNodeByFingerprintAndParentNodeUseCase =
        mock<GetNodeByFingerprintAndParentNodeUseCase>()
    private val addNodeType = mock<AddNodeType>()
    private val nodeId = NodeId(1234L)
    private val expected = mock<FileNode> {
        on { id }.thenReturn(nodeId)
    }
    private val actual = mock<TypedFileNode> {
        on { id }.thenReturn(nodeId)
    }

    @BeforeAll
    fun setUp() {
        underTest = GetNodeFromCloudUseCase(
            getNodeByOriginalFingerprintUseCase,
            getNodeByFingerprintAndParentNodeUseCase,
            addNodeType,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            getNodeByOriginalFingerprintUseCase,
            getNodeByFingerprintAndParentNodeUseCase,
            addNodeType,
        )
        runBlocking {
            whenever(addNodeType.invoke(expected)).thenReturn(actual)
        }
    }

    @Test
    fun `test that node is found by original fingerprint in cloud drive`() = runTest {
        val parentNodeId = mock<NodeId>()
        val originalFingerprint = "originalFingerprint"
        whenever(getNodeByOriginalFingerprintUseCase(originalFingerprint, null))
            .thenReturn(expected)
        val actual = underTest(originalFingerprint, null, parentNodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by fingerprint from parent node`() = runTest {
        val parentNodeId = mock<NodeId>()
        val fingerprint = "fingerprint"
        whenever(getNodeByOriginalFingerprintUseCase(fingerprint, null)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase(fingerprint, parentNodeId))
            .thenReturn(expected)
        val actual = underTest(fingerprint, null, parentNodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by generated fingerprint with parent node`() = runTest {
        val parentNodeId = mock<NodeId>()
        val fingerprint = "fingerprint"
        val generatedFingerprint = "generatedFingerprint"
        whenever(getNodeByOriginalFingerprintUseCase(fingerprint, null)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase(fingerprint, parentNodeId))
            .thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase(generatedFingerprint, parentNodeId))
            .thenReturn(expected)

        val actual = underTest(fingerprint, generatedFingerprint, parentNodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is not found`() = runTest {
        val parentNodeId = mock<NodeId>()
        val fingerprint = "fingerprint"
        val generatedFingerprint = "generatedFingerprint"
        whenever(getNodeByOriginalFingerprintUseCase(fingerprint, null)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase(fingerprint, parentNodeId))
            .thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase(generatedFingerprint, parentNodeId))
            .thenReturn(null)

        assertThat(underTest("", null, parentNodeId)).isEqualTo(null)
    }
}
