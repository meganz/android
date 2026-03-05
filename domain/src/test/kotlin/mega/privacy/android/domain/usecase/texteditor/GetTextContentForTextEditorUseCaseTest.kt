package mega.privacy.android.domain.usecase.texteditor

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.domain.usecase.streaming.StartStreamingServer
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTextContentForTextEditorUseCaseTest {

    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getLocalFileForNodeUseCase: GetLocalFileForNodeUseCase = mock()
    private val getCacheFileUseCase: GetCacheFileUseCase = mock()
    private val getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase = mock()
    private val startStreamingServer: StartStreamingServer = mock()
    private val getStreamingUriStringForNode: GetStreamingUriStringForNode = mock()
    private val downloadNodeUseCase: DownloadNodeUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()

    private val underTest = GetTextContentForTextEditorUseCase(
        ioDispatcher = ioDispatcher,
        getNodeByIdUseCase = getNodeByIdUseCase,
        getLocalFileForNodeUseCase = getLocalFileForNodeUseCase,
        getCacheFileUseCase = getCacheFileUseCase,
        getDataBytesFromUrlUseCase = getDataBytesFromUrlUseCase,
        startStreamingServer = startStreamingServer,
        getStreamingUriStringForNode = getStreamingUriStringForNode,
        downloadNodeUseCase = downloadNodeUseCase,
        fileSystemRepository = fileSystemRepository,
    )

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `test that getText returns content when localPath is set and file exists`() = runTest {
        val file = File(tempDir, "file.txt").apply { writeText("hello") }
        whenever(fileSystemRepository.doesFileExist(file.absolutePath)).thenReturn(true)
        whenever(fileSystemRepository.isFolderPath(file.absolutePath)).thenReturn(false)
        whenever(fileSystemRepository.readTextFromPath(file.absolutePath)).thenReturn("hello")

        val content = underTest(
            nodeHandle = 1L,
            nodeSourceType = 0,
            localPath = file.absolutePath,
        )

        assertThat(content).isEqualTo("hello")
    }

    @Test
    fun `test that getText returns failure when localPath does not exist`() = runTest {
        whenever(fileSystemRepository.doesFileExist("/nonexistent/path.txt")).thenReturn(false)

        val result = runCatching {
            underTest(
                nodeHandle = 1L,
                nodeSourceType = 0,
                localPath = "/nonexistent/path.txt",
            )
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Local path does not exist")
    }

    @Test
    fun `test that getText throws when localPath is a directory`() = runTest {
        val path = "/some/dir"
        whenever(fileSystemRepository.doesFileExist(path)).thenReturn(true)
        whenever(fileSystemRepository.isFolderPath(path)).thenReturn(true)

        val result = runCatching {
            underTest(nodeHandle = 1L, nodeSourceType = 0, localPath = path)
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Path is a directory")
    }

    @Test
    fun `test that getText throws when node not found and no localPath`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(99L))).thenReturn(null)

        val result = runCatching {
            underTest(nodeHandle = 99L, nodeSourceType = 0, localPath = null)
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Node not found or not a file")
    }

    @Test
    fun `test that getText returns content when node has local file`() = runTest {
        val nodeHandle = 10L
        val localPath = File(tempDir, "local.txt").absolutePath
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
        }
        val localFile = File(tempDir, "local.txt").apply { writeText("from cache") }
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(localFile)
        whenever(fileSystemRepository.doesFileExist(localPath)).thenReturn(true)
        whenever(fileSystemRepository.isFilePath(localPath)).thenReturn(true)
        whenever(fileSystemRepository.readTextFromPath(localPath)).thenReturn("from cache")

        val content = underTest(nodeHandle = nodeHandle, nodeSourceType = 0, localPath = null)

        assertThat(content).isEqualTo("from cache")
    }

    @Test
    fun `test that getText returns content via streaming when URL is available`() = runTest {
        val nodeHandle = 20L
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { name } doReturn "doc.txt"
        }
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getStreamingUriStringForNode(node)).thenReturn("https://stream.example/file")
        whenever(getDataBytesFromUrlUseCase(any())).thenReturn("streamed content".toByteArray(Charsets.UTF_8))

        val content = underTest(nodeHandle = nodeHandle, nodeSourceType = 0, localPath = null)

        assertThat(content).isEqualTo("streamed content")
    }

    @Test
    fun `test that getText returns content via download when streaming unavailable`() = runTest {
        val nodeHandle = 30L
        val destFile = File(tempDir, "downloaded.txt").apply { writeText("") }
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { name } doReturn "remote.txt"
        }
        val transfer = mock<Transfer>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getStreamingUriStringForNode(node)).thenReturn(null)
        whenever(getCacheFileUseCase(any(), any())).thenReturn(destFile)
        whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(TransferEvent.TransferFinishEvent(transfer, null))
        )
        whenever(fileSystemRepository.readTextFromPath(destFile.absolutePath)).thenReturn("downloaded")

        val content = underTest(nodeHandle = nodeHandle, nodeSourceType = 0, localPath = null)

        assertThat(content).isEqualTo("downloaded")
    }

    @Test
    fun `test that getText throws when cache file for download is null`() = runTest {
        val nodeHandle = 40L
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { name } doReturn "x.txt"
        }
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getStreamingUriStringForNode(node)).thenReturn(null)
        whenever(getCacheFileUseCase(any(), any())).thenReturn(null)

        val result = runCatching {
            underTest(nodeHandle = nodeHandle, nodeSourceType = 0, localPath = null)
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Cannot get cache file for download")
    }

    @Test
    fun `test that getText throws when download finishes with error`() = runTest {
        val nodeHandle = 50L
        val destFile = File(tempDir, "fail.txt")
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { name } doReturn "y.txt"
        }
        val transfer = mock<Transfer>()
        val downloadError = MegaException(-1, "Download failed")
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getStreamingUriStringForNode(node)).thenReturn(null)
        whenever(getCacheFileUseCase(any(), any())).thenReturn(destFile)
        whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(TransferEvent.TransferFinishEvent(transfer, downloadError))
        )

        val result = runCatching {
            underTest(nodeHandle = nodeHandle, nodeSourceType = 0, localPath = null)
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isSameInstanceAs(downloadError)
    }
}
