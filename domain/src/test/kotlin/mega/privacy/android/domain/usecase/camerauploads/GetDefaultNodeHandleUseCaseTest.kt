package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
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
    private val isNodeInRubbish: IsNodeInRubbish = mock()

    @BeforeAll
    fun setUp() {
        underTest = GetDefaultNodeHandleUseCase(
            nodeRepository = nodeRepository,
            isNodeInRubbish = isNodeInRubbish
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository, isNodeInRubbish)
    }

    @Test
    fun `test that get default node handle returns expected value when invoked`() =
        runTest {
            val defaultFolderName = "Camera Upload"
            val nodeId = NodeId(123456789L)
            whenever(isNodeInRubbish.invoke(nodeId.longValue)).thenReturn(false)
            whenever(nodeRepository.getDefaultNodeHandle(defaultFolderName)).thenReturn(nodeId)
            val actual = underTest(defaultFolderName)
            verify(nodeRepository).getDefaultNodeHandle(defaultFolderName)
            Truth.assertThat(actual).isEqualTo(nodeId.longValue)
        }

    @ParameterizedTest(name = "when nodeId is {0} and is node in rubbish is {1}")
    @MethodSource("provideParams")
    fun `test that get default node handle returns invalid node value when there is no default node handle`(
        nodeId: Long?,
        isInRubbishBin: Boolean,
    ) =
        runTest {
            val defaultFolderName = "Camera Upload"
            val invalidHandle = -1L
            whenever(isNodeInRubbish.invoke(any())).thenReturn(isInRubbishBin)
            whenever(nodeRepository.getDefaultNodeHandle(defaultFolderName)).thenReturn(
                nodeId?.let { NodeId(it) }
            )
            whenever(nodeRepository.getInvalidHandle()).thenReturn(invalidHandle)
            val actual = underTest(defaultFolderName)
            Truth.assertThat(actual).isEqualTo(invalidHandle)
        }

    private fun provideParams() = Stream.of(
        Arguments.of(null, true),
        Arguments.of(null, false),
        Arguments.of(1234L, true)
    )
}
