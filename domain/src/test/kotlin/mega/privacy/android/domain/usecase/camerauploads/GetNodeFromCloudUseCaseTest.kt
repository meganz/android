package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Before
    fun setUp() {
        underTest = GetNodeFromCloudUseCase(
            getNodeByFingerprintUseCase,
            getNodeByOriginalFingerprintUseCase,
            getNodeByFingerprintAndParentNodeUseCase,
            addNodeType
        )
        runBlocking {
            whenever(addNodeType.invoke(expected)).thenReturn(actual)
        }
    }

    @Test
    fun `test that node is found by original fingerprint from parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(expected)
        val actual = underTest("", nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by fingerprint from parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(expected)
        val actual = underTest("", nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by original fingerprint without parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", null)).thenReturn(expected)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(null)
        val actual = underTest("", nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is found by fingerprint without parent node`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintUseCase("")).thenReturn(expected)
        val actual = underTest("", nodeId)
        assertThat(actual).isNotNull()
        assertThat(actual?.id).isEqualTo(expected.id)
    }

    @Test
    fun `test that node is not found`() = runTest {
        whenever(getNodeByOriginalFingerprintUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNodeUseCase("", nodeId)).thenReturn(null)
        whenever(getNodeByFingerprintUseCase("")).thenReturn(null)

        assertThat(underTest("", nodeId)).isEqualTo(null)
    }
}
