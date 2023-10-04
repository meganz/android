package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeFromCloudUseCaseTest {

    private lateinit var underTest: GetNodeFromCloudUseCase

    private val getNodeByFingerprintUseCase = mock<GetNodeByFingerprintUseCase>()
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
            getNodeByFingerprintUseCase,
            getNodeByOriginalFingerprintUseCase,
            getNodeByFingerprintAndParentNodeUseCase,
            addNodeType,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            getNodeByFingerprintUseCase,
            getNodeByOriginalFingerprintUseCase,
            getNodeByFingerprintAndParentNodeUseCase,
            addNodeType,
        )
        runBlocking {
            whenever(addNodeType.invoke(expected)).thenReturn(actual)
        }
    }

    @Test
    fun `test that node is found by original fingerprint from parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(expected)
        val actual = underTest("", null, nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by fingerprint from parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(expected)
        val actual = underTest("", null, nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by original fingerprint without parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", null)).thenReturn(expected)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(null)
        val actual = underTest("", null, nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by fingerprint without parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintUseCase("")).thenReturn(expected)
        val actual = underTest("", null, nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by generated fingerprint with parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintUseCase("")).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("generatedFingerprint", nodeId))
            .thenReturn(expected)

        val actual = underTest("", "generatedFingerprint", nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by generated fingerprint without parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintUseCase("")).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("generatedFingerprint", nodeId))
            .thenReturn(null)
        whenever(getNodeByFingerprintUseCase("generatedFingerprint"))
            .thenReturn(expected)
        val actual = underTest("", "generatedFingerprint", nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is not found`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintUseCase("")).thenReturn(null)

        assertThat(underTest("", null, nodeId)).isEqualTo(null)
    }
}
