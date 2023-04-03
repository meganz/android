package mega.privacy.android.domain.usecase.filenode

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetFileHistoryNumVersionsUseCaseTest {

    private val nodeRepository = mock<NodeRepository>()
    private val node = mock<FileNode>()
    val underTest = GetFileHistoryNumVersionsUseCase(nodeRepository)

    @BeforeEach
    fun resetMock() {
        reset(nodeRepository, node)
    }

    @Test
    fun `test that if file node has no versions it returns 0`() = runTest {
        whenever(node.hasVersion).thenReturn(false)
        val result = underTest(node)
        Truth.assertThat(result).isEqualTo(0)
    }


    @Test
    fun `test that if file node has versions it returns the total returned by repository minus 1`() =
        runTest {
            val versions = 5
            whenever(node.hasVersion).thenReturn(true)
            whenever(nodeRepository.getNumVersions(any())).thenReturn(versions)
            val result = underTest(node)
            Truth.assertThat(result).isEqualTo(versions - 1)
        }
}