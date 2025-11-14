package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.destination.LegacyImageViewerNavKey
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey
import mega.privacy.android.navigation.destination.LegacyPdfViewerNavKey
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import mega.privacy.android.navigation.destination.LegacyZipBrowserNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileNodeContentToNavKeyMapperTest {

    private lateinit var underTest: FileNodeContentToNavKeyMapper
    private val nodeSourceTypeToViewTypeMapper: NodeSourceTypeToViewTypeMapper = mock()

    @BeforeAll
    fun setUp() {
        underTest = FileNodeContentToNavKeyMapper(nodeSourceTypeToViewTypeMapper)
    }

    @BeforeEach
    fun cleanUp() {
        reset(nodeSourceTypeToViewTypeMapper)
    }

    @Test
    fun `test that Pdf content maps to LegacyPdfViewerNavKey`() {
        val nodeHandle = 123L
        val nodeContentUri = NodeContentUri.RemoteContentUri("http://example.com/file.pdf", false)
        val expectedViewType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER
        val fileNode = createMockFileNode(
            id = nodeHandle,
            name = "test.pdf",
            fileTypeInfo = PdfFileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.CLOUD_DRIVE))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.Pdf(uri = nodeContentUri)
        val expected = LegacyPdfViewerNavKey(
            nodeHandle = nodeHandle,
            nodeContentUri = nodeContentUri,
            nodeSourceType = expectedViewType,
            mimeType = "application/pdf"
        )

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that ImageForNode content maps to LegacyImageViewerNavKey`() {
        val nodeHandle = 789L
        val parentHandle = 101L
        val expectedViewType = NodeSourceTypeInt.FAVOURITES_ADAPTER
        val fileNode = createMockFileNode(
            id = nodeHandle,
            parentId = parentHandle,
            name = "test.jpg",
            fileTypeInfo = StaticImageFileTypeInfo("image/jpeg", "jpg")
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.FAVOURITES))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.ImageForNode
        val expected = LegacyImageViewerNavKey(
            nodeHandle = nodeHandle,
            parentNodeHandle = parentHandle,
            nodeSourceType = expectedViewType
        )

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.FAVOURITES
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that TextContent maps to LegacyTextEditorNavKey`() {
        val nodeHandle = 333L
        val expectedViewType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER
        val fileNode = createMockFileNode(
            id = nodeHandle,
            name = "test.txt",
            fileTypeInfo = TextFileTypeInfo("text/plain", "txt")
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.CLOUD_DRIVE))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.TextContent
        val expected = LegacyTextEditorNavKey(
            nodeHandle = nodeHandle,
            mode = TextEditorMode.Edit.value,
            nodeSourceType = expectedViewType
        )

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            textEditorMode = TextEditorMode.Edit
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that Video content maps to LegacyMediaPlayerNavKey correctly`() {
        val nodeHandle = 444L
        val parentHandle = 555L
        val expectedViewType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER
        val nodeContentUri = NodeContentUri.LocalContentUri(File("/path/to/video.mp4"))
        val fileName = "test_video.mp4"
        val fileTypeInfo = VideoFileTypeInfo("video/mp4", "mp4", 120.seconds)
        val fileNode = createMockFileNode(
            id = nodeHandle,
            parentId = parentHandle,
            name = fileName,
            fileTypeInfo = fileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.CLOUD_DRIVE))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.AudioOrVideo(uri = nodeContentUri)
        val expected = LegacyMediaPlayerNavKey(
            nodeHandle = nodeHandle,
            nodeContentUri = nodeContentUri,
            nodeSourceType = expectedViewType,
            sortOrder = SortOrder.ORDER_MODIFICATION_DESC,
            isFolderLink = false,
            fileName = fileName,
            parentHandle = parentHandle,
            fileHandle = nodeHandle,
            fileTypeInfo = fileTypeInfo
        )

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that Audio content maps to LegacyMediaPlayerNavKey correctly`() {
        val nodeHandle = 666L
        val parentHandle = 777L
        val expectedViewType = NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER
        val nodeContentUri = NodeContentUri.RemoteContentUri("http://example.com/audio.mp3", true)
        val fileName = "test_audio.mp3"
        val fileTypeInfo = AudioFileTypeInfo("audio/mpeg", "mp3", 180.seconds)
        val fileNode = createMockFileNode(
            id = nodeHandle,
            parentId = parentHandle,
            name = fileName,
            fileTypeInfo = fileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.AUDIO))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.AudioOrVideo(uri = nodeContentUri)
        val expected = LegacyMediaPlayerNavKey(
            nodeHandle = nodeHandle,
            nodeContentUri = nodeContentUri,
            nodeSourceType = expectedViewType,
            sortOrder = SortOrder.ORDER_NONE,
            isFolderLink = false,
            fileName = fileName,
            parentHandle = parentHandle,
            fileHandle = nodeHandle,
            fileTypeInfo = fileTypeInfo
        )

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.AUDIO
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that LocalZipFile content maps to LegacyZipBrowserNavKey`() {
        val zipFile = File("/path/to/archive.zip")
        val content = FileNodeContent.LocalZipFile(localFile = zipFile)
        val expected = LegacyZipBrowserNavKey(zipFilePath = zipFile.absolutePath)

        val fileNode = createMockFileNode(
            id = 888L,
            name = "archive.zip",
            fileTypeInfo = ZipFileTypeInfo("application/zip", "zip")
        )

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that ImageForChat content returns null`() {
        val content = FileNodeContent.ImageForChat(allAttachmentMessageIds = listOf(1L, 2L, 3L))
        val fileNode = createMockFileNode()

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isNull()
    }

    @Test
    fun `test that Other content returns null`() {
        val content = FileNodeContent.Other(localFile = null)
        val fileNode = createMockFileNode()

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isNull()
    }

    @Test
    fun `test that Other content with localFile returns null`() {
        val content = FileNodeContent.Other(localFile = File("/path/to/file.doc"))
        val fileNode = createMockFileNode()

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isNull()
    }

    @Test
    fun `test that UrlContent returns null`() {
        val nodeContentUri = NodeContentUri.RemoteContentUri("http://example.com", false)
        val content = FileNodeContent.UrlContent(uri = nodeContentUri, path = "http://example.com")
        val fileNode = createMockFileNode()

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isNull()
    }

    private fun createMockFileNode(
        id: Long = 1L,
        parentId: Long = 0L,
        name: String = "test_file",
        fileTypeInfo: mega.privacy.android.domain.entity.FileTypeInfo = TextFileTypeInfo(
            "text/plain",
            "txt"
        ),
    ): TypedFileNode = mock {
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.parentId).thenReturn(NodeId(parentId))
        whenever(it.name).thenReturn(name)
        whenever(it.type).thenReturn(fileTypeInfo)
        whenever(it.isNodeKeyDecrypted).thenReturn(true)
        whenever(it.size).thenReturn(1024L)
        whenever(it.modificationTime).thenReturn(1234567890L)
    }
}

