package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.StreamingServerRepository
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class StreamingServerRepositoryImplTest {
    private lateinit var underTest: StreamingServerRepository

    private val streamingGateway = mock<StreamingGateway>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val fileTypeInfoMapper = mock<FileTypeInfoMapper>()

    private val videoFileTypeInfo =
        VideoFileTypeInfo(mimeType = "video/mp4", extension = "mp4", duration = 10.seconds)
    private val audioFileTypeInfo =
        AudioFileTypeInfo(mimeType = "audio/mpeg", extension = "mp3", duration = 10.seconds)
    private val textFileTypeInfo = TextFileTypeInfo(mimeType = "text/plain", extension = "txt")

    @Before
    fun setUp() {
        underTest = StreamingServerRepositoryImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            streamingGateway = streamingGateway,
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            fileTypeInfoMapper = fileTypeInfoMapper,
        )
    }

    @Test
    fun `test that streaming server is started if port is 0`() = runTest {
        whenever(streamingGateway.getPort()).thenReturn(0)
        underTest.startServer()

        verify(streamingGateway).startServer()
    }

    @Test
    fun `test that server is not started if port is not 0`() = runTest {
        whenever(streamingGateway.getPort()).thenReturn(1)
        underTest.startServer()

        verify(streamingGateway, never()).startServer()
    }

    @Test
    fun `test that local file url string is returned if node exists`() = runTest {
        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
        whenever(fileTypeInfoMapper(any(), any())).thenReturn(textFileTypeInfo)
        val expected = "expectedUrl"
        whenever(streamingGateway.getLocalLink(any())).thenReturn(expected)

        val actual = underTest.getFileStreamingUri(stubNode(name = "notes.txt"))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that isLastStreamedContentMedia is false when nothing has been streamed`() {
        assertThat(underTest.isLastStreamedContentMedia()).isFalse()
    }

    @Test
    fun `test that isLastStreamedContentMedia is true when a video was streamed by handle`() =
        runTest {
            stubStreamingByHandle(name = "clip.mp4", fileTypeInfo = videoFileTypeInfo)

            underTest.getFileStreamingUri(nodeHandle = 1L)

            assertThat(underTest.isLastStreamedContentMedia()).isTrue()
        }

    @Test
    fun `test that isLastStreamedContentMedia is true when audio was streamed by handle`() =
        runTest {
            stubStreamingByHandle(name = "song.mp3", fileTypeInfo = audioFileTypeInfo)

            underTest.getFileStreamingUri(nodeHandle = 1L)

            assertThat(underTest.isLastStreamedContentMedia()).isTrue()
        }

    @Test
    fun `test that isLastStreamedContentMedia is false when a text file was streamed`() = runTest {
        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
        whenever(fileTypeInfoMapper(any(), any())).thenReturn(textFileTypeInfo)

        underTest.getFileStreamingUri(stubNode(name = "notes.txt"))

        assertThat(underTest.isLastStreamedContentMedia()).isFalse()
    }

    @Test
    fun `test that isLastStreamedContentMedia reflects the most recent request`() = runTest {
        stubStreamingByHandle(name = "clip.mp4", fileTypeInfo = videoFileTypeInfo)
        underTest.getFileStreamingUri(nodeHandle = 1L)

        whenever(fileTypeInfoMapper(any(), any())).thenReturn(textFileTypeInfo)
        underTest.getFileStreamingUri(stubNode(name = "notes.txt"))

        assertThat(underTest.isLastStreamedContentMedia()).isFalse()
    }

    @Test
    fun `test that isLastStreamedContentMedia is true when a folder link video was streamed`() =
        runTest {
            val megaNode = mock<MegaNode> { on { name }.thenReturn("clip.mp4") }
            whenever(megaApiFolderGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(megaApiFolderGateway.authorizeNode(any<MegaNode>())).thenReturn(megaNode)
            whenever(fileTypeInfoMapper(any(), any())).thenReturn(videoFileTypeInfo)

            underTest.getFolderLinkFileStreamingUri(nodeHandle = 1L)

            assertThat(underTest.isLastStreamedContentMedia()).isTrue()
        }

    private fun stubNode(name: String) = mock<Node> {
        on { id }.thenReturn(NodeId(1L))
        on { this.name }.thenReturn(name)
    }

    private suspend fun stubStreamingByHandle(name: String, fileTypeInfo: FileTypeInfo) {
        val megaNode = mock<MegaNode> { on { this.name }.thenReturn(name) }
        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
        whenever(fileTypeInfoMapper(any(), any())).thenReturn(fileTypeInfo)
    }
}
