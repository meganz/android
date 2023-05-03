package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [GetDefaultNodeHandleUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDefaultNodeHandleUseCaseTest {

    private lateinit var underTest: GetDefaultNodeHandleUseCase

    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetDefaultNodeHandleUseCase(
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that get default node handle returns expected value when invoked`() =
        runTest {
            val defaultFolderName = "Camera Upload"
            val nodeId = NodeId(123456789L)
            whenever(nodeRepository.getDefaultNodeHandle(defaultFolderName)).thenReturn(nodeId)
            val actual = underTest(defaultFolderName)
            verify(nodeRepository).getDefaultNodeHandle(defaultFolderName)
            Truth.assertThat(actual).isEqualTo(nodeId.longValue)
        }

    @Test
    fun `test that get default node handle returns invalid node value when there is no default node handle`() =
        runTest {
            val defaultFolderName = "Camera Upload"
            val invalidHandle = -1L
            whenever(nodeRepository.getDefaultNodeHandle(defaultFolderName)).thenReturn(null)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(invalidHandle)
            val actual = underTest(defaultFolderName)
            Truth.assertThat(actual).isEqualTo(invalidHandle)
        }
}
