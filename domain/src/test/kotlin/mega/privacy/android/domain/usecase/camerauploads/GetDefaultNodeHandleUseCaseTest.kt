package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [GetDefaultNodeHandleUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDefaultNodeHandleUseCaseTest {

    private lateinit var underTest: GetDefaultNodeHandleUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = GetDefaultNodeHandleUseCase(
            nodeRepository = nodeRepository,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository, isNodeInRubbishBinUseCase)
    }

    @Test
    fun `test that get default node handle returns expected value when invoked`() =
        runTest {
            val defaultFolderName = "Camera Upload"
            val nodeId = NodeId(123456789L)
            whenever(isNodeInRubbishBinUseCase(nodeId)).thenReturn(false)
            whenever(nodeRepository.getDefaultNodeHandle(defaultFolderName)).thenReturn(nodeId)
            val actual = underTest(defaultFolderName)
            verify(nodeRepository).getDefaultNodeHandle(defaultFolderName)
            Truth.assertThat(actual).isEqualTo(nodeId.longValue)
        }

    @ParameterizedTest(name = "when nodeId is {0} and is node in rubbish is {1}")
    @MethodSource("provideParams")
    fun `test that get default node handle returns invalid node value when there is no default node handle`(
        nodeId: NodeId?,
        isInRubbishBin: Boolean,
    ) =
        runTest {
            val defaultFolderName = "Camera Upload"
            val invalidHandle = -1L
            nodeId?.let {
                whenever(isNodeInRubbishBinUseCase(it)).thenReturn(isInRubbishBin)
            }
            whenever(nodeRepository.getDefaultNodeHandle(defaultFolderName)).thenReturn(nodeId)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(invalidHandle)
            val actual = underTest(defaultFolderName)
            Truth.assertThat(actual).isEqualTo(invalidHandle)
        }

    private fun provideParams() = Stream.of(
        Arguments.of(null, true),
        Arguments.of(null, false),
        Arguments.of(NodeId(1234L), true)
    )
}
