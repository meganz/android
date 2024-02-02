package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodePreviewFilePathUseCaseTest {
    private val fileSystemRepository: FileSystemRepository = mock()
    private val cacheRepository: CacheRepository = mock()

    private val underTest = GetNodePreviewFilePathUseCase(fileSystemRepository, cacheRepository)

    @ParameterizedTest(name = "test path for file is expected {0}")
    @MethodSource("provideParams")
    fun `test that file path is same as expected and actual`(
        node: TypedFileNode,
        expected: String,
    ) = runTest {
        if (node.id.longValue == 1234L)
            whenever(fileSystemRepository.getLocalFile(node)).thenReturn(File(expected))
        else {
            whenever(fileSystemRepository.getLocalFile(node)).thenReturn(null)
        }
        whenever(cacheRepository.getFilePreviewPath(node.name)).thenReturn(expected)
        whenever(fileSystemRepository.doesFileExist(expected)).thenReturn(true)
        val actual = underTest(node)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParams() = Stream.of(
        Arguments.of(mock<DefaultTypedFileNode> {
            whenever(it.name).thenReturn("some name")
        }, "cached path"),
        Arguments.of(mock<DefaultTypedFileNode> {
            whenever(it.id).thenReturn(NodeId(1234))
            whenever(it.name).thenReturn("some name")
        }, "local path"),
    )
}