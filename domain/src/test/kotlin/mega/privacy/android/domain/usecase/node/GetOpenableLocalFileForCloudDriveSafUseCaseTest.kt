package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOpenableLocalFileForCloudDriveSafUseCaseTest {

    private val getFileNodeContentForFileNodeUseCase: GetFileNodeContentForFileNodeUseCase = mock()
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase = mock()
    private val downloadPreviewFileForNodeAndAwaitUseCase: DownloadPreviewFileForNodeAndAwaitUseCase =
        mock()

    private lateinit var underTest: GetOpenableLocalFileForCloudDriveSafUseCase

    @BeforeEach
    fun setUp() {
        reset(
            getFileNodeContentForFileNodeUseCase,
            getNodeContentUriUseCase,
            downloadPreviewFileForNodeAndAwaitUseCase,
        )
        underTest = GetOpenableLocalFileForCloudDriveSafUseCase(
            getFileNodeContentForFileNodeUseCase = getFileNodeContentForFileNodeUseCase,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            downloadPreviewFileForNodeAndAwaitUseCase = downloadPreviewFileForNodeAndAwaitUseCase,
        )
    }

    @Test
    fun `test that invoke returns local file when content is Other with existing local file`() =
        runTest {
            val node = fileNode()
            val local = tempFileWithContent("a")
            whenever(getFileNodeContentForFileNodeUseCase(node, false)).thenReturn(
                FileNodeContent.Other(local),
            )

            val result = underTest(node)

            assertThat(result).isEqualTo(local)
            verify(downloadPreviewFileForNodeAndAwaitUseCase, times(0)).invoke(any())
        }

    @Test
    fun `test that invoke delegates to download when Other has null local file`() = runTest {
        val node = fileNode()
        val downloaded = tempFileWithContent("b")
        whenever(getFileNodeContentForFileNodeUseCase(node, false)).thenReturn(
            FileNodeContent.Other(null),
        )
        whenever(downloadPreviewFileForNodeAndAwaitUseCase(node)).thenReturn(downloaded)

        val result = underTest(node)

        assertThat(result).isEqualTo(downloaded)
        verify(downloadPreviewFileForNodeAndAwaitUseCase).invoke(node)
    }

    @Test
    fun `test that invoke returns file for Pdf with LocalContentUri when file valid`() = runTest {
        val node = fileNode()
        val local = tempFileWithContent("pdf")
        val uri = NodeContentUri.LocalContentUri(local)
        whenever(getFileNodeContentForFileNodeUseCase(node, false)).thenReturn(
            FileNodeContent.Pdf(uri),
        )

        val result = underTest(node)

        assertThat(result).isEqualTo(local)
        verify(downloadPreviewFileForNodeAndAwaitUseCase, times(0)).invoke(any())
    }

    @Test
    fun `test that invoke downloads when Pdf has RemoteContentUri`() = runTest {
        val node = fileNode()
        val downloaded = tempFileWithContent("remote")
        whenever(getFileNodeContentForFileNodeUseCase(node, false)).thenReturn(
            FileNodeContent.Pdf(
                NodeContentUri.RemoteContentUri("https://example.com/f", false),
            ),
        )
        whenever(downloadPreviewFileForNodeAndAwaitUseCase(node)).thenReturn(downloaded)

        val result = underTest(node)

        assertThat(result).isEqualTo(downloaded)
        verify(downloadPreviewFileForNodeAndAwaitUseCase).invoke(node)
    }

    @Test
    fun `test that invoke uses getNodeContentUri for TextContent when local uri valid`() = runTest {
        val node = fileNode()
        val local = tempFileWithContent("text")
        whenever(getFileNodeContentForFileNodeUseCase(node, false)).thenReturn(
            FileNodeContent.TextContent,
        )
        whenever(getNodeContentUriUseCase(node)).thenReturn(NodeContentUri.LocalContentUri(local))

        val result = underTest(node)

        assertThat(result).isEqualTo(local)
        verify(downloadPreviewFileForNodeAndAwaitUseCase, times(0)).invoke(any())
    }

    private fun fileNode(
        id: Long = 1L,
        name: String = "file.bin",
        type: FileTypeInfo = TextFileTypeInfo(mimeType = "text/plain", extension = "txt"),
    ): TypedFileNode {
        val node = mock<TypedFileNode>()
        whenever(node.id).thenReturn(NodeId(id))
        whenever(node.name).thenReturn(name)
        whenever(node.type).thenReturn(type)
        return node
    }

    private fun tempFileWithContent(content: String): File =
        File.createTempFile("openable_", ".tmp").apply {
            deleteOnExit()
            writeText(content)
        }
}
