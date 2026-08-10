package mega.privacy.android.domain.usecase.texteditor

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.node.GetFolderLinkNodeContentUriUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTextContentForFolderLinkUseCaseTest {

    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val getFolderLinkNodeContentUriUseCase: GetFolderLinkNodeContentUriUseCase = mock()
    private val readStreamingContentUseCase: ReadStreamingContentUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()

    private val underTest = GetTextContentForFolderLinkUseCase(
        ioDispatcher = ioDispatcher,
        getFolderLinkNodeContentUriUseCase = getFolderLinkNodeContentUriUseCase,
        readStreamingContentUseCase = readStreamingContentUseCase,
        fileSystemRepository = fileSystemRepository,
    )

    private val node: TypedFileNode = mock()

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            getFolderLinkNodeContentUriUseCase,
            readStreamingContentUseCase,
            fileSystemRepository,
        )
    }

    @Test
    fun `test that invoke reads remote content via streaming`() = runTest {
        val url = "http://127.0.0.1:4443/abc"
        whenever(getFolderLinkNodeContentUriUseCase(node))
            .thenReturn(NodeContentUri.RemoteContentUri(url, shouldStopHttpSever = true))
        whenever(readStreamingContentUseCase(url)).thenReturn("folder content")

        val chunks = underTest(node = node).toList()
        val content = chunks.flatten().joinToString("\n")

        assertThat(content).isEqualTo("folder content")
        verify(getFolderLinkNodeContentUriUseCase).invoke(node)
        verify(readStreamingContentUseCase).invoke(url)
    }

    @Test
    fun `test that invoke emits chunked lines for remote content`() = runTest {
        val url = "http://127.0.0.1:4443/abc"
        whenever(getFolderLinkNodeContentUriUseCase(node))
            .thenReturn(NodeContentUri.RemoteContentUri(url, shouldStopHttpSever = true))
        whenever(readStreamingContentUseCase(url)).thenReturn("a\nb\nc\nd\ne")

        val chunks = underTest(node = node, chunkSizeLines = 2).toList()

        assertThat(chunks).hasSize(3)
        assertThat(chunks[0]).containsExactly("a", "b")
        assertThat(chunks[1]).containsExactly("c", "d")
        assertThat(chunks[2]).containsExactly("e")
    }

    @Test
    fun `test that invoke reads local content from disk when already downloaded`() = runTest {
        val file = File("/tmp/folder-link.txt")
        whenever(getFolderLinkNodeContentUriUseCase(node))
            .thenReturn(NodeContentUri.LocalContentUri(file))
        whenever(fileSystemRepository.readLinesFromPathInChunks(file.absolutePath, 500))
            .thenReturn(flowOf(listOf("local line")))

        val chunks = underTest(node = node).toList()

        assertThat(chunks.flatten()).containsExactly("local line")
        verify(fileSystemRepository).readLinesFromPathInChunks(file.absolutePath, 500)
        verify(readStreamingContentUseCase, never()).invoke(any())
    }
}
