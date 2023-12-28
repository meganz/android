package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLocalFilePathUseCaseTest {
    private val fileSystemRepository: FileSystemRepository = mock()

    private val underTest = GetLocalFilePathUseCase(fileSystemRepository)

    @ParameterizedTest(name = "test path for file is expected {0}")
    @MethodSource("provideParams")
    fun `test that file path is same as expected and actual`(
        node: FileNode,
        expected: String?,
    ) = runTest {
        if (node.id.longValue == 1234L)
            whenever(fileSystemRepository.getLocalFile(node)).thenReturn(File("some path"))
        else {
            whenever(fileSystemRepository.getLocalFile(node)).thenReturn(null)
        }
        val actual = underTest(node)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that when folder node is provided, its local path should be null`() = runTest {
        val folderNode = mock<FolderNode>()
        val path = underTest(folderNode)
        Truth.assertThat(path).isNull()
    }

    private fun provideParams() = Stream.of(
        Arguments.of(mock<FileNode>(), null),
        Arguments.of(mock<FileNode> {
            whenever(it.id).thenReturn(NodeId(1234))
        }, "some path"),
    )
}