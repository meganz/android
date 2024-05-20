package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetParentNodeFromMegaApiFolderUseCaseTest {
    private lateinit var underTest: GetParentNodeFromMegaApiFolderUseCase
    private val nodeRepository = mock<NodeRepository>()

    private val testHandle: Long = 1
    private val testNode = mock<FileNode>()

    @BeforeAll
    fun setUp() {
        underTest = GetParentNodeFromMegaApiFolderUseCase(nodeRepository = nodeRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(nodeRepository)
    }

    @Test
    fun `test that result is null`() =
        runTest {
            whenever(nodeRepository.getParentNodeFromMegaApiFolder(anyOrNull())).thenReturn(null)
            assertThat(underTest(testHandle)).isNull()
        }

    @Test
    fun `test that parent node is returned`() =
        runTest {
            whenever(nodeRepository.getParentNodeFromMegaApiFolder(anyOrNull())).thenReturn(testNode)
            assertThat(underTest(testHandle)).isEqualTo(testNode)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest(testHandle)
            verify(nodeRepository).getParentNodeFromMegaApiFolder(testHandle)
        }
}