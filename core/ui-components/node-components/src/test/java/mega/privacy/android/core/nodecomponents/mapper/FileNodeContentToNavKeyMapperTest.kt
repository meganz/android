package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
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
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
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
    fun `test that Pdf content maps to LegacyPdfViewerNavKey when isPDFViewerEnabled is false`() {
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE),
            isPDFViewerEnabled = false
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that Pdf content maps to PdfViewerNavKey when isPDFViewerEnabled is true with RemoteContentUri`() {
        val nodeHandle = 456L
        val contentUrl = "http://example.com/remote.pdf"
        val shouldStopServer = true
        val nodeContentUri = NodeContentUri.RemoteContentUri(contentUrl, shouldStopServer)
        val fileNode = createMockFileNode(
            id = nodeHandle,
            name = "remote.pdf",
            fileTypeInfo = PdfFileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.CLOUD_DRIVE))
            .thenReturn(NodeSourceTypeInt.FILE_BROWSER_ADAPTER)

        val content = FileNodeContent.Pdf(uri = nodeContentUri)
        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE),
            isPDFViewerEnabled = true
        )

        val expected = PdfViewerNavKey(
            nodeHandle = nodeHandle,
            contentUri = contentUrl,
            isLocalContent = false,
            shouldStopHttpServer = shouldStopServer,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            mimeType = "application/pdf",
            title = null,
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that Pdf content maps to PdfViewerNavKey when isPDFViewerEnabled is true with LocalContentUri`() {
        val nodeHandle = 789L
        val localFile = File("/path/to/local.pdf")
        val nodeContentUri = NodeContentUri.LocalContentUri(localFile)
        val fileNode = createMockFileNode(
            id = nodeHandle,
            name = "local.pdf",
            fileTypeInfo = PdfFileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.CLOUD_DRIVE))
            .thenReturn(NodeSourceTypeInt.FILE_BROWSER_ADAPTER)

        val content = FileNodeContent.Pdf(uri = nodeContentUri)
        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE),
            isPDFViewerEnabled = true
        )

        val expected = PdfViewerNavKey(
            nodeHandle = nodeHandle,
            contentUri = localFile.path,
            isLocalContent = true,
            shouldStopHttpServer = false,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            mimeType = "application/pdf",
            title = null,
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that Pdf content maps to PdfViewerNavKey with FolderLink sets nodeSourceType FOLDER_LINK`() {
        val nodeHandle = 111L
        val localFile = File("/path/to/folder-link.pdf")
        val nodeContentUri = NodeContentUri.LocalContentUri(localFile)
        val fileNode = createMockFileNode(
            id = nodeHandle,
            name = "folder-link.pdf",
            fileTypeInfo = PdfFileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.FOLDER_LINK))
            .thenReturn(NodeSourceTypeInt.FOLDER_LINK_ADAPTER)

        val content = FileNodeContent.Pdf(uri = nodeContentUri)
        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.FolderLink,
            isPDFViewerEnabled = true
        )

        val expected = PdfViewerNavKey(
            nodeHandle = nodeHandle,
            contentUri = localFile.path,
            isLocalContent = true,
            shouldStopHttpServer = false,
            nodeSourceType = NodeSourceType.FOLDER_LINK,
            mimeType = "application/pdf",
            title = null,
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.FAVOURITES)
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that ImageForNode content maps to LegacyImageViewerNavKey with FileLink sets parentNodeHandle to minus one and url`() {
        val nodeHandle = 999L
        val publicUrl = "https://mega.nz/file/abc"
        val expectedViewType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER
        val fileNode = createMockFileNode(
            id = nodeHandle,
            parentId = 100L,
            name = "photo.jpg",
            fileTypeInfo = StaticImageFileTypeInfo("image/jpeg", "jpg")
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.FILE_LINK))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.ImageForNode
        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.FileLink(url = publicUrl)
        )

        val expected = LegacyImageViewerNavKey(
            nodeHandle = nodeHandle,
            parentNodeHandle = -1L,
            nodeSourceType = expectedViewType,
            url = publicUrl
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that ImageForNode content maps to LegacyImageViewerNavKey with RecentsBucket sets nodeIds and isInShare`() {
        val nodeHandle = 888L
        val parentHandle = 77L
        val nodeIds = listOf(1L, 2L, 3L)
        val isInShare = true
        val expectedViewType = NodeSourceTypeInt.RECENTS_BUCKET_ADAPTER
        val fileNode = createMockFileNode(
            id = nodeHandle,
            parentId = parentHandle,
            name = "image.png",
            fileTypeInfo = StaticImageFileTypeInfo("image/png", "png")
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.RECENTS_BUCKET))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.ImageForNode
        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.RecentsBucket(nodeIds = nodeIds, isInShare = isInShare)
        )

        val expected = LegacyImageViewerNavKey(
            nodeHandle = nodeHandle,
            parentNodeHandle = parentHandle,
            nodeSourceType = expectedViewType,
            nodeIds = nodeIds,
            isInShare = isInShare
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
            nodeSourceType = expectedViewType,
        )

        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE),
            textEditorMode = TextEditorMode.Edit
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that TextContent maps to LegacyTextEditorNavKey with View mode and default parameters`() {
        val nodeHandle = 123L
        val expectedViewType = NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
        val fileNode = createMockFileNode(
            id = nodeHandle,
            name = "readme.txt",
            fileTypeInfo = TextFileTypeInfo("text/plain", "txt")
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.INCOMING_SHARES))
            .thenReturn(expectedViewType)

        val result = underTest(
            content = FileNodeContent.TextContent,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.Default(NodeSourceType.INCOMING_SHARES),
            textEditorMode = TextEditorMode.View
        )

        val expected = LegacyTextEditorNavKey(
            nodeHandle = nodeHandle,
            mode = TextEditorMode.View.value,
            nodeSourceType = expectedViewType,
            fileName = null
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE),
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.AUDIO)
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that AudioOrVideo content maps to LegacyMediaPlayerNavKey with FolderLink sets isFolderLink true`() {
        val nodeHandle = 555L
        val parentHandle = 444L
        val expectedViewType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER
        val nodeContentUri = NodeContentUri.LocalContentUri(File("/path/to/video.mp4"))
        val fileName = "folder_link_video.mp4"
        val fileTypeInfo = VideoFileTypeInfo("video/mp4", "mp4", 60.seconds)
        val fileNode = createMockFileNode(
            id = nodeHandle,
            parentId = parentHandle,
            name = fileName,
            fileTypeInfo = fileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.FOLDER_LINK))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.AudioOrVideo(uri = nodeContentUri)
        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.FolderLink
        )

        val expected = LegacyMediaPlayerNavKey(
            nodeHandle = nodeHandle,
            nodeContentUri = nodeContentUri,
            nodeSourceType = expectedViewType,
            sortOrder = SortOrder.ORDER_NONE,
            isFolderLink = true,
            fileName = fileName,
            parentHandle = parentHandle,
            fileHandle = nodeHandle,
            fileTypeInfo = fileTypeInfo
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that AudioOrVideo content maps to LegacyMediaPlayerNavKey with FileLink sets parentHandle to minus one`() {
        val nodeHandle = 333L
        val expectedViewType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER
        val nodeContentUri = NodeContentUri.RemoteContentUri("http://example.com/video.mp4", false)
        val fileName = "file_link_video.mp4"
        val fileTypeInfo = VideoFileTypeInfo("video/mp4", "mp4", 90.seconds)
        val fileNode = createMockFileNode(
            id = nodeHandle,
            parentId = 999L,
            name = fileName,
            fileTypeInfo = fileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.FILE_LINK))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.AudioOrVideo(uri = nodeContentUri)
        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.FileLink(url = "https://mega.nz/file/xyz")
        )

        val expected = LegacyMediaPlayerNavKey(
            nodeHandle = nodeHandle,
            nodeContentUri = nodeContentUri,
            nodeSourceType = expectedViewType,
            sortOrder = SortOrder.ORDER_NONE,
            isFolderLink = false,
            fileName = fileName,
            parentHandle = -1L,
            fileHandle = nodeHandle,
            fileTypeInfo = fileTypeInfo
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that AudioOrVideo content maps to LegacyMediaPlayerNavKey with RecentsBucket sets nodeHandles and searchedItems`() {
        val nodeHandle = 222L
        val parentHandle = 111L
        val nodeIds = listOf(10L, 20L, 30L)
        val searchedItems = listOf(40L, 50L)
        val expectedViewType = NodeSourceTypeInt.RECENTS_BUCKET_ADAPTER
        val nodeContentUri = NodeContentUri.LocalContentUri(File("/path/to/audio.mp3"))
        val fileName = "recents_audio.mp3"
        val fileTypeInfo = AudioFileTypeInfo("audio/mpeg", "mp3", 120.seconds)
        val fileNode = createMockFileNode(
            id = nodeHandle,
            parentId = parentHandle,
            name = fileName,
            fileTypeInfo = fileTypeInfo
        )

        whenever(nodeSourceTypeToViewTypeMapper(NodeSourceType.RECENTS_BUCKET))
            .thenReturn(expectedViewType)

        val content = FileNodeContent.AudioOrVideo(uri = nodeContentUri)
        val result = underTest(
            content = content,
            fileNode = fileNode,
            nodeSourceData = NodeSourceData.RecentsBucket(nodeIds = nodeIds, isInShare = false),
            sortOrder = SortOrder.ORDER_DEFAULT_ASC,
            searchedItems = searchedItems
        )

        val expected = LegacyMediaPlayerNavKey(
            nodeHandle = nodeHandle,
            nodeContentUri = nodeContentUri,
            nodeSourceType = expectedViewType,
            sortOrder = SortOrder.ORDER_DEFAULT_ASC,
            isFolderLink = false,
            fileName = fileName,
            parentHandle = parentHandle,
            fileHandle = nodeHandle,
            fileTypeInfo = fileTypeInfo,
            searchedItems = searchedItems,
            nodeHandles = nodeIds
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE)
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE)
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE)
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE)
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
            nodeSourceData = NodeSourceData.Default(NodeSourceType.CLOUD_DRIVE)
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
