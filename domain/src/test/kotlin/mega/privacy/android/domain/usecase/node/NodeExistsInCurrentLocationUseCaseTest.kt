package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeExistsInCurrentLocationUseCaseTest {
    private val nodeRepository: NodeRepository = mock()

    private val underTest = NodeExistsInCurrentLocationUseCase(nodeRepository)

    @ParameterizedTest(name = "Search Node with name for {0}")
    @MethodSource("provideParams")
    fun `test that invoke returns the result of doesChildExistByName`(
        providedName: String,
        expected: Boolean
    ) = runTest {
        val currentNodeMock = mock<FileNode> {
            whenever(it.id).thenReturn(NodeId(123L))
        }
        whenever(nodeRepository.doesChildExistByName(currentNodeMock.id, providedName))
            .thenReturn(expected)
        val actual = underTest(currentNodeMock.id, providedName)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParams() = Stream.of(
        Arguments.of("SameName", true),
        Arguments.of("SameName", false),
    )
}
