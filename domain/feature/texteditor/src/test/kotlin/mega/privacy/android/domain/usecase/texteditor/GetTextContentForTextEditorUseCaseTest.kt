package mega.privacy.android.domain.usecase.texteditor

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.toList
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.domain.usecase.streaming.StartStreamingServer
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTextContentForTextEditorUseCaseTest {

    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getLocalFileForNodeUseCase: GetLocalFileForNodeUseCase = mock()
    private val getCacheFileUseCase: GetCacheFileUseCase = mock()
    private val startStreamingServer: StartStreamingServer = mock()
    private val getStreamingUriStringForNode: GetStreamingUriStringForNode = mock()
    private val downloadNodeUseCase: DownloadNodeUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()
    private val readStreamingContentUseCase: ReadStreamingContentUseCase = mock()
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase = mock()

    private val underTest = GetTextContentForTextEditorUseCase(
        ioDispatcher = ioDispatcher,
        getNodeByIdUseCase = getNodeByIdUseCase,
        getLocalFileForNodeUseCase = getLocalFileForNodeUseCase,
        getCacheFileUseCase = getCacheFileUseCase,
        startStreamingServer = startStreamingServer,
        getStreamingUriStringForNode = getStreamingUriStringForNode,
        downloadNodeUseCase = downloadNodeUseCase,
        fileSystemRepository = fileSystemRepository,
        readStreamingContentUseCase = readStreamingContentUseCase,
        cancelTransferByTagUseCase = cancelTransferByTagUseCase,
    )

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            getNodeByIdUseCase,
            getLocalFileForNodeUseCase,
            getCacheFileUseCase,
            startStreamingServer,
            getStreamingUriStringForNode,
            downloadNodeUseCase,
            fileSystemRepository,
            readStreamingContentUseCase,
            cancelTransferByTagUseCase,
        )
    }

    @Test
    fun `test that getText returns content when localPath is set and file exists`() = runTest {
        val file = File(tempDir, "file.txt").apply { writeText("hello") }
        whenever(fileSystemRepository.doesFileExist(file.absolutePath)).thenReturn(true)
        whenever(fileSystemRepository.isFolderPath(file.absolutePath)).thenReturn(false)
        whenever(fileSystemRepository.readLinesFromPathInChunks(file.absolutePath, 500))
            .thenReturn(flowOf(listOf("hello")))

        val chunks = underTest(
            nodeHandle = 1L,
            localPath = file.absolutePath,
        ).toList()
        val content = chunks.flatten().joinToString("\n")

        assertThat(content).isEqualTo("hello")
    }

    @Test
    fun `test that getText emits chunks with custom chunkSizeLines when localPath is set`() = runTest {
        val file = File(tempDir, "chunked.txt").apply { writeText("a\nb\nc\nd\ne") }
        whenever(fileSystemRepository.doesFileExist(file.absolutePath)).thenReturn(true)
        whenever(fileSystemRepository.isFolderPath(file.absolutePath)).thenReturn(false)
        val chunkSize = 2
        whenever(fileSystemRepository.readLinesFromPathInChunks(file.absolutePath, chunkSize))
            .thenReturn(flowOf(listOf("a", "b"), listOf("c", "d"), listOf("e")))

        val chunks = underTest(
            nodeHandle = 1L,
            localPath = file.absolutePath,
            chunkSizeLines = chunkSize,
        ).toList()

        assertThat(chunks).hasSize(3)
        assertThat(chunks[0]).containsExactly("a", "b")
        assertThat(chunks[1]).containsExactly("c", "d")
        assertThat(chunks[2]).containsExactly("e")
        assertThat(chunks.flatten().joinToString("\n")).isEqualTo("a\nb\nc\nd\ne")
    }

    @Test
    fun `test that getText falls back to node resolution when localPath does not exist`() = runTest {
        whenever(fileSystemRepository.doesFileExist("/nonexistent/path.txt")).thenReturn(false)
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)

        val result = runCatching {
            underTest(
                nodeHandle = 1L,
                localPath = "/nonexistent/path.txt",
            ).toList()
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Node not found or not a file")
    }

    @Test
    fun `test that getText falls back to node resolution when localPath is a directory`() = runTest {
        val path = "/some/dir"
        whenever(fileSystemRepository.doesFileExist(path)).thenReturn(true)
        whenever(fileSystemRepository.isFolderPath(path)).thenReturn(true)
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)

        val result = runCatching {
            underTest(nodeHandle = 1L, localPath = path).toList()
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Node not found or not a file")
    }

    @Test
    fun `test that getText throws when node not found and no localPath`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(99L))).thenReturn(null)

        val result = runCatching {
            underTest(nodeHandle = 99L, localPath = null).toList()
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Node not found or not a file")
    }

    @Test
    fun `test that getText throws when node is not a file`() = runTest {
        val folderNode = mock<TypedFolderNode> {
            on { id } doReturn NodeId(99L)
        }
        whenever(getNodeByIdUseCase(NodeId(99L))).thenReturn(folderNode)

        val result = runCatching {
            underTest(nodeHandle = 99L, localPath = null).toList()
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
        whenever(fileSystemRepository.readLinesFromPathInChunks(localPath, 500))
            .thenReturn(flowOf(listOf("from cache")))

        val chunks = underTest(nodeHandle = nodeHandle, localPath = null).toList()
        val content = chunks.flatten().joinToString("\n")

        assertThat(content).isEqualTo("from cache")
    }

    @Test
    fun `test that getText returns content via streaming when URL is available`() = runTest {
        val nodeHandle = 20L
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { name } doReturn "doc.txt"
        }
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getStreamingUriStringForNode(node)).thenReturn("https://stream.example/file")
        whenever(readStreamingContentUseCase(any())).thenReturn("streamed content")

        val chunks = underTest(nodeHandle = nodeHandle, localPath = null).toList()
        val content = chunks.flatten().joinToString("\n")

        assertThat(content).isEqualTo("streamed content")
    }

    @Test
    fun `test that getText returns content via download when streaming unavailable`() = runTest {
        val nodeHandle = 30L
        val destFile = File(tempDir, "downloaded.txt")
        val downloadedPath = destFile.absolutePath
        val node = stubRemoteNode(nodeHandle, "remote.txt")
        val transfer = mock<Transfer> {
            on { tag } doReturn 1
            on { localPath } doReturn downloadedPath
        }
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getStreamingUriStringForNode(node)).thenReturn(null)
        whenever(getCacheFileUseCase(any(), any())).thenReturn(destFile)
        whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(TransferEvent.TransferFinishEvent(transfer, null))
        )
        whenever(fileSystemRepository.doesFileExist(downloadedPath)).thenReturn(true)
        whenever(fileSystemRepository.readLinesFromPathInChunks(downloadedPath, 500))
            .thenReturn(flowOf(listOf("downloaded")))

        val chunks = underTest(nodeHandle = nodeHandle, localPath = null).toList()

        assertThat(chunks.flatten().joinToString("\n")).isEqualTo("downloaded")
        verify(cancelTransferByTagUseCase, never()).invoke(any())
    }

    @Test
    fun `test that getText returns content via download when startStreamingServer throws`() =
        runTest {
            val nodeHandle = 35L
            val destFile = File(tempDir, "fallback.txt")
            val downloadedPath = destFile.absolutePath
            val node = stubRemoteNode(nodeHandle, "remote.txt")
            val transfer = mock<Transfer> {
                on { tag } doReturn 2
                on { localPath } doReturn downloadedPath
            }
            whenever(startStreamingServer()).thenThrow(RuntimeException("Streaming unavailable"))
            whenever(getCacheFileUseCase(any(), any())).thenReturn(destFile)
            whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
                flowOf(TransferEvent.TransferFinishEvent(transfer, null))
            )
            whenever(fileSystemRepository.doesFileExist(downloadedPath)).thenReturn(true)
            whenever(fileSystemRepository.readLinesFromPathInChunks(downloadedPath, 500))
                .thenReturn(flowOf(listOf("fallback content")))

            val chunks = underTest(nodeHandle = nodeHandle, localPath = null).toList()

            assertThat(chunks.flatten().joinToString("\n")).isEqualTo("fallback content")
        }

    @Test
    fun `test that getText returns content via download when the streaming read fails`() = runTest {
        val nodeHandle = 36L
        val destFile = File(tempDir, "after_streaming_failure.txt")
        val downloadedPath = destFile.absolutePath
        val node = stubRemoteNode(nodeHandle, "remote.txt")
        val transfer = mock<Transfer> {
            on { tag } doReturn 3
            on { localPath } doReturn downloadedPath
        }
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getStreamingUriStringForNode(node)).thenReturn("https://stream.example/file")
        whenever(readStreamingContentUseCase(any())).thenAnswer { throw IOException("not served") }
        whenever(getCacheFileUseCase(any(), any())).thenReturn(destFile)
        whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(TransferEvent.TransferFinishEvent(transfer, null))
        )
        whenever(fileSystemRepository.doesFileExist(downloadedPath)).thenReturn(true)
        whenever(fileSystemRepository.readLinesFromPathInChunks(downloadedPath, 500))
            .thenReturn(flowOf(listOf("downloaded after streaming failed")))

        val chunks = underTest(nodeHandle = nodeHandle, localPath = null).toList()

        assertThat(chunks.flatten().joinToString("\n"))
            .isEqualTo("downloaded after streaming failed")
    }

    @Test
    fun `test that getText throws when cache file for download is null`() = runTest {
        val nodeHandle = 40L
        stubRemoteNode(nodeHandle, "x.txt")
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getStreamingUriStringForNode(any())).thenReturn(null)
        whenever(getCacheFileUseCase(any(), any())).thenReturn(null)

        val result = runCatching {
            underTest(nodeHandle = nodeHandle, localPath = null).toList()
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `test that getText throws when download finishes with error`() = runTest {
        val nodeHandle = 50L
        val downloadError = MegaException(-1, "Download failed")
        val node = stubRemoteNode(nodeHandle, "failed.txt")
        val transfer = mock<Transfer> { on { tag } doReturn 4 }
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getStreamingUriStringForNode(node)).thenReturn(null)
        whenever(getCacheFileUseCase(any(), any())).thenReturn(File(tempDir, "failed.txt"))
        whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(TransferEvent.TransferFinishEvent(transfer, downloadError))
        )

        val result = runCatching {
            underTest(nodeHandle = nodeHandle, localPath = null).toList()
        }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(downloadError)
    }

    @Test
    fun `test that getText throws when download succeeds but file does not exist`() = runTest {
        val nodeHandle = 60L
        val downloadedPath = File(tempDir, "missing.txt").absolutePath
        val node = stubRemoteNode(nodeHandle, "missing.txt")
        val transfer = mock<Transfer> {
            on { tag } doReturn 5
            on { localPath } doReturn downloadedPath
        }
        whenever(startStreamingServer()).thenThrow(RuntimeException("unavailable"))
        whenever(getCacheFileUseCase(any(), any())).thenReturn(File(tempDir, "missing.txt"))
        whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(TransferEvent.TransferFinishEvent(transfer, null))
        )
        whenever(fileSystemRepository.doesFileExist(downloadedPath)).thenReturn(false)

        val result = runCatching {
            underTest(nodeHandle = nodeHandle, localPath = null).toList()
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(FileNotFoundException::class.java)
    }

    @Test
    fun `test that getText throws and cancels the transfer when the download hits transfer over quota`() =
        runTest {
            val nodeHandle = 70L
            val overQuotaError = QuotaExceededMegaException(-17, "Transfer quota exceeded", 60L)
            val node = stubRemoteNode(nodeHandle, "quota.txt")
            val transfer = mock<Transfer> { on { tag } doReturn 7 }
            whenever(startStreamingServer()).thenThrow(RuntimeException("unavailable"))
            whenever(getCacheFileUseCase(any(), any())).thenReturn(File(tempDir, "quota.txt"))
            whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
                flowOf(TransferEvent.TransferTemporaryErrorEvent(transfer, overQuotaError))
            )

            val result = runCatching {
                underTest(nodeHandle = nodeHandle, localPath = null).toList()
            }

            assertThat(result.exceptionOrNull()).isSameInstanceAs(overQuotaError)
            verify(cancelTransferByTagUseCase).invoke(7)
        }

    @Test
    fun `test that getText throws and cancels the transfer when the node is blocked`() = runTest {
        val nodeHandle = 71L
        val blockedError = BlockedMegaException(-16, "Blocked")
        val node = stubRemoteNode(nodeHandle, "blocked.txt")
        val transfer = mock<Transfer> { on { tag } doReturn 8 }
        whenever(startStreamingServer()).thenThrow(RuntimeException("unavailable"))
        whenever(getCacheFileUseCase(any(), any())).thenReturn(File(tempDir, "blocked.txt"))
        whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(TransferEvent.TransferTemporaryErrorEvent(transfer, blockedError))
        )

        val result = runCatching {
            underTest(nodeHandle = nodeHandle, localPath = null).toList()
        }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(blockedError)
        verify(cancelTransferByTagUseCase).invoke(8)
    }

    @Test
    fun `test that getText ignores other temporary errors during the download`() = runTest {
        val nodeHandle = 72L
        val destFile = File(tempDir, "recovered.txt")
        val downloadedPath = destFile.absolutePath
        val node = stubRemoteNode(nodeHandle, "recovered.txt")
        val transfer = mock<Transfer> {
            on { tag } doReturn 9
            on { localPath } doReturn downloadedPath
        }
        whenever(startStreamingServer()).thenThrow(RuntimeException("unavailable"))
        whenever(getCacheFileUseCase(any(), any())).thenReturn(destFile)
        whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
            flowOf(
                TransferEvent.TransferTemporaryErrorEvent(transfer, MegaException(-3, "Try again")),
                TransferEvent.TransferFinishEvent(transfer, null),
            )
        )
        whenever(fileSystemRepository.doesFileExist(downloadedPath)).thenReturn(true)
        whenever(fileSystemRepository.readLinesFromPathInChunks(downloadedPath, 500))
            .thenReturn(flowOf(listOf("recovered")))

        val chunks = underTest(nodeHandle = nodeHandle, localPath = null).toList()

        assertThat(chunks.flatten().joinToString("\n")).isEqualTo("recovered")
        verify(cancelTransferByTagUseCase, never()).invoke(any())
    }

    @Test
    fun `test that getText uses resolvedNode and skips getNodeByIdUseCase`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "chat_file.txt"
        }
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getStreamingUriStringForNode(node)).thenReturn("https://stream.example/file")
        whenever(readStreamingContentUseCase(any())).thenReturn("chat content")

        val chunks = underTest(
            resolvedNode = node,
            localPath = null,
        ).toList()
        val content = chunks.flatten().joinToString("\n")

        assertThat(content).isEqualTo("chat content")
        verify(getNodeByIdUseCase, never()).invoke(any())
    }

    private suspend fun stubRemoteNode(nodeHandle: Long, name: String): TypedFileNode {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(nodeHandle)
            on { this.name } doReturn name
        }
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(node)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        return node
    }
}
