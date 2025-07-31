package mega.privacy.android.core.nodecomponents.mapper


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class NodeUiItemMapperTest {

    private val fileTypeIconMapper: FileTypeIconMapper = mock()

    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()

    private val nodeSubtitleMapper: NodeSubtitleMapper = mock()

    private lateinit var underTest: NodeUiItemMapper

    @BeforeEach
    fun setUp() {
        whenever(durationInSecondsTextMapper(any())).thenReturn("1:30")
        whenever(
            nodeSubtitleMapper(
                any(),
                any()
            )
        ).thenReturn(LocalizedText.Literal("1 KB · Jan 1, 2024"))
        setupDefaultFileTypeIconMocks()
        underTest = NodeUiItemMapper(
            fileTypeIconMapper = fileTypeIconMapper,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            nodeSubtitleMapper = nodeSubtitleMapper,
            ioDispatcher = StandardTestDispatcher()
        )
    }

    private fun setupDefaultFileTypeIconMocks() {
        whenever(fileTypeIconMapper("")).thenReturn(IconPackR.drawable.ic_folder_medium_solid)
        whenever(fileTypeIconMapper("txt")).thenReturn(IconPackR.drawable.ic_text_medium_solid)
    }

    private fun createMockFolderNode(
        id: Long = 1L,
        name: String = "Test Folder",
        isFavourite: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        isIncomingShare: Boolean = false,
        description: String? = null,
        tags: List<String> = emptyList(),
        childFolderCount: Int = 2,
        childFileCount: Int = 3,
    ): TypedFolderNode = mock {
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.name).thenReturn(name)
        whenever(it.parentId).thenReturn(NodeId(0L))
        whenever(it.base64Id).thenReturn("test_base64_id")
        whenever(it.label).thenReturn(0)
        whenever(it.isFavourite).thenReturn(isFavourite)
        whenever(it.isMarkedSensitive).thenReturn(isMarkedSensitive)
        whenever(it.isSensitiveInherited).thenReturn(isSensitiveInherited)
        whenever(it.isTakenDown).thenReturn(false)
        whenever(it.isIncomingShare).thenReturn(isIncomingShare)
        whenever(it.isNodeKeyDecrypted).thenReturn(true)
        whenever(it.creationTime).thenReturn(1234567890L)
        whenever(it.isAvailableOffline).thenReturn(false)
        whenever(it.versionCount).thenReturn(0)
        whenever(it.description).thenReturn(description)
        whenever(it.tags).thenReturn(tags)
        whenever(it.childFolderCount).thenReturn(childFolderCount)
        whenever(it.childFileCount).thenReturn(childFileCount)
        whenever(it.type).thenReturn(FolderType.Default)
    }

    private fun createMockFileNode(
        id: Long = 2L,
        name: String = "test_file.txt",
        isFavourite: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        isIncomingShare: Boolean = false,
        description: String? = null,
        tags: List<String> = emptyList(),
        size: Long = 1024L,
    ): TypedFileNode = mock {
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.name).thenReturn(name)
        whenever(it.parentId).thenReturn(NodeId(1L))
        whenever(it.base64Id).thenReturn("test_base64_id")
        whenever(it.label).thenReturn(0)
        whenever(it.isFavourite).thenReturn(isFavourite)
        whenever(it.isMarkedSensitive).thenReturn(isMarkedSensitive)
        whenever(it.isSensitiveInherited).thenReturn(isSensitiveInherited)
        whenever(it.isTakenDown).thenReturn(false)
        whenever(it.isIncomingShare).thenReturn(isIncomingShare)
        whenever(it.isNodeKeyDecrypted).thenReturn(true)
        whenever(it.creationTime).thenReturn(1234567890L)
        whenever(it.isAvailableOffline).thenReturn(false)
        whenever(it.versionCount).thenReturn(0)
        whenever(it.description).thenReturn(description)
        whenever(it.tags).thenReturn(tags)
        whenever(it.size).thenReturn(size)
        whenever(it.modificationTime).thenReturn(1234567890L)
        whenever(it.hasThumbnail).thenReturn(false)
        whenever(it.hasPreview).thenReturn(false)
        whenever(it.type).thenReturn(TextFileTypeInfo("text/plain", "txt"))
    }

    @Test
    fun `test that invoke returns correct NodeUiItem when mapping folder node`() = runTest {
        val mockFolderNode = createMockFolderNode()

        val result = underTest(
            nodeList = listOf(mockFolderNode),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        val nodeUiItem = result[0]
        assertThat(nodeUiItem.node).isEqualTo(mockFolderNode)
        assertThat(nodeUiItem.isFolderNode).isTrue()
        assertThat(nodeUiItem.isVideoNode).isFalse()
        assertThat(nodeUiItem.iconRes).isEqualTo(IconPackR.drawable.ic_folder_medium_solid)
        assertThat(nodeUiItem.title).isEqualTo(LocalizedText.Literal("Test Folder"))
        assertThat(nodeUiItem.isSelected).isFalse()
        assertThat(nodeUiItem.isInvisible).isFalse()
        assertThat(nodeUiItem.isHighlighted).isFalse()
        assertThat(nodeUiItem.showLink).isFalse()
        assertThat(nodeUiItem.showFavourite).isFalse()
        assertThat(nodeUiItem.isSensitive).isFalse()
        assertThat(nodeUiItem.showBlurEffect).isFalse()
        assertThat(nodeUiItem.duration).isNull()
    }

    @Test
    fun `test that invoke returns correct NodeUiItem when mapping file node`() = runTest {
        val mockFileNode = createMockFileNode()

        val result = underTest(
            nodeList = listOf(mockFileNode),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        val nodeUiItem = result[0]
        assertThat(nodeUiItem.node).isEqualTo(mockFileNode)
        assertThat(nodeUiItem.isFolderNode).isFalse()
        assertThat(nodeUiItem.isVideoNode).isFalse()
        assertThat(nodeUiItem.iconRes).isEqualTo(IconPackR.drawable.ic_text_medium_solid)
        assertThat(nodeUiItem.title).isEqualTo(LocalizedText.Literal("test_file.txt"))
        assertThat(nodeUiItem.isSelected).isFalse()
        assertThat(nodeUiItem.isInvisible).isFalse()
        assertThat(nodeUiItem.isHighlighted).isFalse()
        assertThat(nodeUiItem.showLink).isFalse()
        assertThat(nodeUiItem.showFavourite).isFalse()
        assertThat(nodeUiItem.isSensitive).isFalse()
        assertThat(nodeUiItem.showBlurEffect).isFalse()
        assertThat(nodeUiItem.duration).isNull()
    }

    @Test
    fun `test that invoke sets isHighlighted to true when highlightedNodeId matches node id`() =
        runTest {
            val mockFolderNode = createMockFolderNode()

            val result = underTest(
                nodeList = listOf(mockFolderNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                highlightedNodeId = NodeId(1L),
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isHighlighted).isTrue()
        }

    @Test
    fun `test that invoke sets isHighlighted to true when highlightedNames contains node name`() =
        runTest {
            val mockFolderNode = createMockFolderNode()

            val result = underTest(
                nodeList = listOf(mockFolderNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                highlightedNames = setOf("Test Folder"),
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isHighlighted).isTrue()
        }

    @Test
    fun `test that invoke sets isSensitive to true when shouldApplySensitiveMode is true and node is marked sensitive`() =
        runTest {
            val mockSensitiveNode = createMockFileNode(
                id = 4L,
                name = "sensitive_file.txt",
                isMarkedSensitive = true,
            )

            val result = underTest(
                nodeList = listOf(mockSensitiveNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                shouldApplySensitiveMode = true,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isSensitive).isTrue()
        }

    @Test
    fun `test that invoke sets isSensitive to false when shouldApplySensitiveMode is false`() =
        runTest {
            val mockSensitiveNode = createMockFileNode(
                id = 4L,
                name = "sensitive_file.txt",
                isMarkedSensitive = true,
            )

            val result = underTest(
                nodeList = listOf(mockSensitiveNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                shouldApplySensitiveMode = false,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isSensitive).isFalse()
        }

    @Test
    fun `test that invoke sets isSensitive to false when nodeSourceType is INCOMING_SHARES even if shouldApplySensitiveMode is true`() =
        runTest {
            val mockSensitiveNode = createMockFileNode(
                id = 4L,
                name = "sensitive_file.txt",
                isMarkedSensitive = true,
            )

            val result = underTest(
                nodeList = listOf(mockSensitiveNode),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                shouldApplySensitiveMode = true,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isSensitive).isFalse()
        }

    @Test
    fun `test that invoke sets showLink to true when node has exportedData`() = runTest {
        val mockExportedData = mock<mega.privacy.android.domain.entity.node.ExportedData> {
            whenever(it.publicLink).thenReturn("https://mega.nz/test")
            whenever(it.publicLinkCreationTime).thenReturn(1234567890L)
        }
        val mockNodeWithLink = createMockFileNode(
            id = 5L,
            name = "shared_file.txt",
        )
        whenever(mockNodeWithLink.exportedData).thenReturn(mockExportedData)

        val result = underTest(
            nodeList = listOf(mockNodeWithLink),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].showLink).isTrue()
    }

    @Test
    fun `test that invoke sets showFavourite to true when node is favourite and not incoming share`() =
        runTest {
            val mockFavouriteNode = createMockFileNode(
                id = 6L,
                name = "favourite_file.txt",
                isFavourite = true,
            )

            val result = underTest(
                nodeList = listOf(mockFavouriteNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].showFavourite).isTrue()
        }

    @Test
    fun `test that invoke sets showFavourite to false when node is favourite but is incoming share`() =
        runTest {
            val mockFavouriteIncomingShare = createMockFileNode(
                id = 7L,
                name = "favourite_incoming_share.txt",
                isFavourite = true,
                isIncomingShare = true,
            )

            val result = underTest(
                nodeList = listOf(mockFavouriteIncomingShare),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].showFavourite).isFalse()
        }

    @Test
    fun `test that invoke sets formattedDescription when node has description`() = runTest {
        val mockNodeWithDescription = createMockFileNode(
            id = 9L,
            name = "file_with_description.txt",
            description = "This is a test description",
        )

        val result = underTest(
            nodeList = listOf(mockNodeWithDescription),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].formattedDescription).isEqualTo(
            LocalizedText.Literal("This is a test description")
        )
    }

    @Test
    fun `test that invoke replaces newlines with spaces in formattedDescription`() = runTest {
        val mockNodeWithDescription = createMockFileNode(
            id = 10L,
            name = "file_with_description_newlines.txt",
            description = "This is a test description\nwith newlines",
        )

        val result = underTest(
            nodeList = listOf(mockNodeWithDescription),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].formattedDescription).isEqualTo(
            LocalizedText.Literal("This is a test description with newlines")
        )
    }

    @Test
    fun `test that invoke sets tags when nodeSourceType is not RUBBISH_BIN`() = runTest {
        val mockNodeWithTags = createMockFileNode(
            id = 11L,
            name = "file_with_tags.txt",
            tags = listOf("tag1", "tag2"),
        )

        val result = underTest(
            nodeList = listOf(mockNodeWithTags),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].tags).isEqualTo(listOf("tag1", "tag2"))
    }

    @Test
    fun `test that invoke sets tags to null when nodeSourceType is RUBBISH_BIN`() = runTest {
        val mockNodeWithTags = createMockFileNode(
            id = 11L,
            name = "file_with_tags.txt",
            tags = listOf("tag1", "tag2"),
        )

        val result = underTest(
            nodeList = listOf(mockNodeWithTags),
            nodeSourceType = NodeSourceType.RUBBISH_BIN,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].tags).isNull()
    }

    @Test
    fun `test that invoke maps multiple nodes correctly`() = runTest {
        val mockFolderNode = createMockFolderNode()
        val mockFileNode = createMockFileNode()
        val nodeList = listOf(mockFolderNode, mockFileNode)

        val result = underTest(
            nodeList = nodeList,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(2)
        assertThat(result[0].isFolderNode).isTrue()
        assertThat(result[1].isFolderNode).isFalse()
    }
} 