package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [IsNodeDeletedFromBackupsUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsNodeDeletedFromBackupsUseCaseTest {

    private lateinit var underTest: IsNodeDeletedFromBackupsUseCase

    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsNodeDeletedFromBackupsUseCase(
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isNodeInRubbishBinUseCase,
            nodeRepository,
        )
    }

    @Test
    fun `test that false is returned when the node does not exist in the rubbish bin`() = runTest {
        whenever(isNodeInRubbishBinUseCase(NodeId(any()))).thenReturn(false)
        val deletedFromBackups = underTest(NodeId(123456))
        assertThat(deletedFromBackups).isFalse()
        verifyNoInteractions(nodeRepository)
    }

    @ParameterizedTest(name = "node path: {0}, from backups: {1}")
    @MethodSource("provideParameters")
    fun `test that the deleted node came from backups or not`(
        nodePath: String,
        expected: Boolean,
    ) = runTest {
        val testNodeId = NodeId(123456)
        whenever(isNodeInRubbishBinUseCase(NodeId(any()))).thenReturn(true)
        whenever(nodeRepository.getNodePathById(testNodeId)).thenReturn(nodePath)

        val actual = underTest(testNodeId)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of("", false),
        Arguments.of("/", false),
        Arguments.of("/bin/SyncDebris", false),
        Arguments.of("//bin/SyncDebri", false),
        Arguments.of("//bin/SyncDebris", true),
        Arguments.of("//bin/SyncDebris/", true),
        Arguments.of("//bin/SyncDebris/test", true),
        Arguments.of("//bin/SyncDebriss", false),
        Arguments.of("//bin/SyncDebriss/test", false),
    )
}
