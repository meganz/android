package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.file.GetPartialDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.domain.usecase.streaming.StartStreamingServer
import mega.privacy.android.domain.usecase.streaming.StopStreamingServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileTypeInfoByContentUseCaseTest {
    private lateinit var underTest: GetFileTypeInfoByContentUseCase

    private val getLocalFileForNodeUseCase = mock<GetLocalFileForNodeUseCase>()
    private val httpServerIsRunning = mock<MegaApiHttpServerIsRunningUseCase>()
    private val startStreamingServer = mock<StartStreamingServer>()
    private val stopStreamingServer = mock<StopStreamingServer>()
    private val getStreamingUriStringForNode = mock<GetStreamingUriStringForNode>()
    private val getPartialDataBytesFromUrlUseCase = mock<GetPartialDataBytesFromUrlUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getNodeContentUriUseCase = mock<GetNodeContentUriUseCase>()
    private val getLocalFolderLinkFromMegaApiUseCase = mock<GetLocalFolderLinkFromMegaApiUseCase>()

    private val headerSize = GetFileTypeInfoByContentUseCase.HEADER_SIZE_BYTES

    @BeforeEach
    fun setUp() {
        underTest = GetFileTypeInfoByContentUseCase(
            getLocalFileForNodeUseCase = getLocalFileForNodeUseCase,
            httpServerIsRunning = httpServerIsRunning,
            startStreamingServer = startStreamingServer,
            stopStreamingServer = stopStreamingServer,
            getStreamingUriStringForNode = getStreamingUriStringForNode,
            getPartialDataBytesFromUrlUseCase = getPartialDataBytesFromUrlUseCase,
            fileSystemRepository = fileSystemRepository,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            getLocalFolderLinkFromMegaApiUseCase = getLocalFolderLinkFromMegaApiUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getLocalFileForNodeUseCase,
            httpServerIsRunning,
            startStreamingServer,
            stopStreamingServer,
            getStreamingUriStringForNode,
            getPartialDataBytesFromUrlUseCase,
            fileSystemRepository,
            getNodeContentUriUseCase,
            getLocalFolderLinkFromMegaApiUseCase,
        )
    }

    private fun stubNode(handle: Long = 1L) = mock<TypedFileNode>().stub {
        on { type } doReturn UnMappedFileTypeInfo("")
        on { id } doReturn NodeId(handle)
    }

    @Test
    fun `test that the type is detected from a local file header when a local file exists`() =
        runTest {
            val node = stubNode()
            val localFile = File.createTempFile("no-extension", null).apply { deleteOnExit() }
            val header = byteArrayOf(1, 2, 3)
            whenever(getLocalFileForNodeUseCase(node)).thenReturn(localFile)
            whenever(
                fileSystemRepository.readFirstBytesFromPath(
                    localFile.absolutePath,
                    headerSize
                )
            )
                .thenReturn(header)
            whenever(fileSystemRepository.getFileTypeInfoFromContent(header, 0))
                .thenReturn(TextFileTypeInfo("text/plain", ""))

            val result = underTest(node)

            assertThat(result).isEqualTo(TextFileTypeInfo("text/plain", ""))
            verifyNoInteractions(startStreamingServer)
            verifyNoInteractions(stopStreamingServer)
            verifyNoInteractions(getPartialDataBytesFromUrlUseCase)
        }

    @Test
    fun `test that the type is detected from streaming when no local file exists`() = runTest {
        val node = stubNode()
        val url = "http://localhost/stream"
        val header = byteArrayOf(0x89.toByte(), 0x50)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getStreamingUriStringForNode(node)).thenReturn(url)
        whenever(getPartialDataBytesFromUrlUseCase(URL(url), headerSize)).thenReturn(header)
        whenever(fileSystemRepository.getFileTypeInfoFromContent(header, 0))
            .thenReturn(StaticImageFileTypeInfo("image/png", ""))

        val result = underTest(node)

        assertThat(result).isEqualTo(StaticImageFileTypeInfo("image/png", ""))
        verify(startStreamingServer).invoke()
    }

    @Test
    fun `test that the streaming server is stopped when it was started by this use case`() =
        runTest {
            val node = stubNode()
            val url = "http://localhost/stream"
            whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
            whenever(httpServerIsRunning()).thenReturn(0)
            whenever(getStreamingUriStringForNode(node)).thenReturn(url)
            whenever(getPartialDataBytesFromUrlUseCase(URL(url), headerSize))
                .thenReturn(byteArrayOf(0x25, 0x50, 0x44, 0x46))
            whenever(fileSystemRepository.getFileTypeInfoFromContent(any(), any()))
                .thenReturn(TextFileTypeInfo("text/plain", ""))

            underTest(node)

            verify(stopStreamingServer).invoke()
        }

    @Test
    fun `test that the streaming server is not stopped when it was already running`() = runTest {
        val node = stubNode()
        val url = "http://localhost/stream"
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(httpServerIsRunning()).thenReturn(8080)
        whenever(getStreamingUriStringForNode(node)).thenReturn(url)
        whenever(getPartialDataBytesFromUrlUseCase(URL(url), headerSize))
            .thenReturn(byteArrayOf(0x25, 0x50, 0x44, 0x46))
        whenever(fileSystemRepository.getFileTypeInfoFromContent(any(), any()))
            .thenReturn(TextFileTypeInfo("text/plain", ""))

        underTest(node)

        verifyNoInteractions(startStreamingServer, stopStreamingServer)
    }

    @Test
    fun `test that null is returned when the header is empty`() = runTest {
        val node = stubNode()
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getStreamingUriStringForNode(node)).thenReturn("http://localhost/stream")
        whenever(getPartialDataBytesFromUrlUseCase(any(), any())).thenReturn(ByteArray(0))

        assertThat(underTest(node)).isNull()
        verifyNoInteractions(fileSystemRepository)
    }

    @Test
    fun `test that null is returned when the streaming url is blank`() = runTest {
        val node = stubNode()
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getStreamingUriStringForNode(node)).thenReturn("")

        assertThat(underTest(node)).isNull()
        verifyNoInteractions(getPartialDataBytesFromUrlUseCase)
    }

    @Test
    fun `test that a folder link child reads the header from the authorized main api url`() =
        runTest {
            val node = stubNode(handle = 55L)
            val url = "http://localhost/authorized"
            val header = byteArrayOf(1, 2, 3)
            whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
            whenever(getLocalFolderLinkFromMegaApiUseCase(55L)).thenReturn(url)
            whenever(getPartialDataBytesFromUrlUseCase(URL(url), headerSize)).thenReturn(header)
            whenever(fileSystemRepository.getFileTypeInfoFromContent(header, 0))
                .thenReturn(TextFileTypeInfo("text/plain", ""))

            val result = underTest(node, isLinkNode = true)

            assertThat(result).isEqualTo(TextFileTypeInfo("text/plain", ""))
            verifyNoInteractions(getNodeContentUriUseCase, getStreamingUriStringForNode)
        }

    @Test
    fun `test that a file link falls back to the node content uri when not in the folder api`() =
        runTest {
            val node = stubNode(handle = 77L)
            val url = "http://localhost/filelink"
            val header = byteArrayOf(4, 5, 6)
            whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
            whenever(getLocalFolderLinkFromMegaApiUseCase(77L)).thenReturn(null)
            whenever(getNodeContentUriUseCase(node))
                .thenReturn(NodeContentUri.RemoteContentUri(url, false))
            whenever(getPartialDataBytesFromUrlUseCase(URL(url), headerSize)).thenReturn(header)
            whenever(fileSystemRepository.getFileTypeInfoFromContent(header, 0))
                .thenReturn(TextFileTypeInfo("text/plain", ""))

            val result = underTest(node, isLinkNode = true)

            assertThat(result).isEqualTo(TextFileTypeInfo("text/plain", ""))
            verifyNoInteractions(getStreamingUriStringForNode)
        }

    @Test
    fun `test that the main streaming server is started and stopped for a link header read`() =
        runTest {
            val node = stubNode(handle = 55L)
            whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
            whenever(httpServerIsRunning()).thenReturn(0)
            whenever(getLocalFolderLinkFromMegaApiUseCase(55L)).thenReturn("http://localhost/a")
            whenever(getPartialDataBytesFromUrlUseCase(any(), any())).thenReturn(byteArrayOf(1))
            whenever(fileSystemRepository.getFileTypeInfoFromContent(any(), any()))
                .thenReturn(TextFileTypeInfo("text/plain", ""))

            underTest(node, isLinkNode = true)

            verify(startStreamingServer).invoke()
            verify(stopStreamingServer).invoke()
        }

    @Test
    fun `test that the main streaming server is not stopped when it was already running`() =
        runTest {
            val node = stubNode(handle = 55L)
            whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
            whenever(httpServerIsRunning()).thenReturn(8080)
            whenever(getLocalFolderLinkFromMegaApiUseCase(55L)).thenReturn("http://localhost/a")
            whenever(getPartialDataBytesFromUrlUseCase(any(), any())).thenReturn(byteArrayOf(1))
            whenever(fileSystemRepository.getFileTypeInfoFromContent(any(), any()))
                .thenReturn(TextFileTypeInfo("text/plain", ""))

            underTest(node, isLinkNode = true)

            verifyNoInteractions(startStreamingServer, stopStreamingServer)
        }

    @Test
    fun `test that a link node does not use the generic streaming path`() = runTest {
        val node = stubNode(handle = 55L)
        whenever(getLocalFileForNodeUseCase(node)).thenReturn(null)
        whenever(getLocalFolderLinkFromMegaApiUseCase(55L)).thenReturn("http://localhost/a")
        whenever(getPartialDataBytesFromUrlUseCase(any(), any())).thenReturn(byteArrayOf(1))
        whenever(fileSystemRepository.getFileTypeInfoFromContent(any(), any()))
            .thenReturn(TextFileTypeInfo("text/plain", ""))

        underTest(node, isLinkNode = true)

        verifyNoInteractions(getStreamingUriStringForNode)
    }
}
