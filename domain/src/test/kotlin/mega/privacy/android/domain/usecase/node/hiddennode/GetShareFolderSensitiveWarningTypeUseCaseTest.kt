package mega.privacy.android.domain.usecase.node.hiddennode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SensitiveNodeShareWarning
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetShareFolderSensitiveWarningTypeUseCaseTest {
    private lateinit var underTest: GetShareFolderSensitiveWarningTypeUseCase
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val hasSensitiveDescendantUseCase = mock<HasSensitiveDescendantUseCase>()

    @Before
    fun setUp() {
        underTest = GetShareFolderSensitiveWarningTypeUseCase(
            getNodeByIdUseCase = getNodeByIdUseCase,
            hasSensitiveDescendantUseCase = hasSensitiveDescendantUseCase,
        )
    }

    private suspend fun stubFolder(
        id: Long,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        isShared: Boolean = false,
        hasSensitiveDescendant: Boolean = false,
    ): NodeId {
        val nodeId = NodeId(id)
        val node = mock<TypedFolderNode> {
            on { this.id }.thenReturn(nodeId)
            on { this.isMarkedSensitive }.thenReturn(isMarkedSensitive)
            on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
            on { this.isShared }.thenReturn(isShared)
        }
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        whenever(hasSensitiveDescendantUseCase(nodeId)).thenReturn(hasSensitiveDescendant)
        return nodeId
    }

    @Test
    fun `test that None is returned and no nodes are fetched when hidden nodes are not enabled`() =
        runTest {
            val nodeId = NodeId(1L)

            val result = underTest(listOf(nodeId), hiddenNodesEnabled = false)

            assertThat(result).isEqualTo(SensitiveNodeShareWarning.None)
            verifyNoInteractions(getNodeByIdUseCase)
            verifyNoInteractions(hasSensitiveDescendantUseCase)
        }

    @Test
    fun `test that None is returned when the shared folder is not sensitive`() = runTest {
        val nodeId = stubFolder(id = 1L)

        val result = underTest(listOf(nodeId), hiddenNodesEnabled = true)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.None)
    }

    @Test
    fun `test that Folder is returned when the single folder is marked sensitive`() = runTest {
        val nodeId = stubFolder(id = 1L, isMarkedSensitive = true)

        val result = underTest(listOf(nodeId), hiddenNodesEnabled = true)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.Folder)
    }

    @Test
    fun `test that Folder is returned when the single folder inherits sensitivity`() = runTest {
        val nodeId = stubFolder(id = 1L, isSensitiveInherited = true)

        val result = underTest(listOf(nodeId), hiddenNodesEnabled = true)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.Folder)
    }

    @Test
    fun `test that Folder is returned when the single folder has a sensitive descendant`() =
        runTest {
            val nodeId = stubFolder(id = 1L, hasSensitiveDescendant = true)

            val result = underTest(listOf(nodeId), hiddenNodesEnabled = true)

            assertThat(result).isEqualTo(SensitiveNodeShareWarning.Folder)
        }

    @Test
    fun `test that Folders is returned when one of multiple folders is sensitive`() = runTest {
        val first = stubFolder(id = 1L)
        val second = stubFolder(id = 2L, isMarkedSensitive = true)

        val result = underTest(listOf(first, second), hiddenNodesEnabled = true)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.Folders)
    }

    @Test
    fun `test that already-shared folders are skipped`() = runTest {
        val nodeId = stubFolder(id = 1L, isMarkedSensitive = true, isShared = true)

        val result = underTest(listOf(nodeId), hiddenNodesEnabled = true)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.None)
    }

    @Test
    fun `test that non-folder nodes are skipped`() = runTest {
        val nodeId = NodeId(1L)
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(mock<TypedFileNode>())

        val result = underTest(listOf(nodeId), hiddenNodesEnabled = true)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.None)
    }

    @Test
    fun `test that missing nodes are skipped`() = runTest {
        val nodeId = NodeId(1L)
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(null)

        val result = underTest(listOf(nodeId), hiddenNodesEnabled = true)

        assertThat(result).isEqualTo(SensitiveNodeShareWarning.None)
    }
}
